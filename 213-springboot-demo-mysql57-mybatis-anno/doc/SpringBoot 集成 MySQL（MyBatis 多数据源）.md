# SpringBoot 集成 MySQL（MyBatis 多数据源）全解析

## 通俗 + 表格 + 图文 从场景到实战

本文系统拆解 MyBatis 多数据源的核心知识点：先梳理多数据源的典型业务场景，再对比主流实现思路，最后通过完整示例落地 SpringBoot+MyBatis 多数据源集成，帮你搞懂 “什么时候用多数据源”“怎么优雅实现”。

## 一、为什么会用到多数据源？（典型场景）

多数据源本质是 “应用需要访问多个不同的数据库实例 / 库表”，核心场景可分为 4 大类，用表格 + 通俗解释梳理：

| 场景分类               | 通俗解释                                              | 实际案例                                                     |
| ---------------------- | ----------------------------------------------------- | ------------------------------------------------------------ |
| 1. 业务拆分（分库）    | 不同业务模块的数据存放在不同数据库（解耦 + 提升性能） | 用户模块→user_db，订单模块→order_db，支付模块→pay_db         |
| 2. 读写分离            | 读操作走从库，写操作走主库（提升并发能力）            | 下单 / 改价→主库 master，查订单 / 查商品→从库 slave1/slave2  |
| 3. 数据同步 / 跨库查询 | 需从多个数据库拉取数据做聚合分析                      | 报表系统：从用户库 + 订单库 + 商品库拉取数据生成销售报表     |
| 4. 多租户 / 环境隔离   | 不同租户 / 环境的数据存放在不同数据库                 | SaaS 系统：租户 A→tenant_a_db，租户 B→tenant_b_db；测试环境→test_db，生产环境→prod_db |

### 场景拆解（图文 + 示例）

#### 1. 业务分库（最常见）

**核心痛点**：单库承载所有业务，数据量 / 并发量过高，性能瓶颈明显。**解决方案**：按业务模块拆分到不同数据库，示例架构：

```plaintext
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  用户库user_db │    │ 订单库order_db│    │ 支付库pay_db  │
│ - user表      │    │ - order表     │    │ - payment表   │
│ - user_role表 │    │ - order_item表│    │ - refund表    │
└───────────────┘    └───────────────┘    └───────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                    ┌───────────────┐
                    │ SpringBoot应用 │
                    │ （MyBatis多数据源）│
                    └───────────────┘
```

#### 2. 读写分离（高并发场景）

**核心痛点**：单库读请求过高（如商品详情页每秒 10 万次查询），主库扛不住。**解决方案**：主库负责写（INSERT/UPDATE/DELETE），从库负责读（SELECT），示例架构：

```plaintext
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ 主库master_db │───>│ 从库slave1_db │    │ 从库slave2_db │
│ （写操作）│    │ （读操作）│    │ （读操作）│
└───────────────┘    └───────────────┘    └───────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                    ┌───────────────┐
                    │ SpringBoot应用 │
                    │ （动态切换数据源）│
                    └───────────────┘
```

#### 3. 跨库数据聚合（报表 / 数据分析）

**核心痛点**：单库无法满足数据聚合需求，需跨库拉取数据。**示例**：销售报表需统计 “用户数（user_db）+ 订单数（order_db）+ 支付金额（pay_db）”，需同时访问 3 个库。

## 二、多数据源的主流实现思路（对比 + 选型）

SpringBoot+MyBatis 实现多数据源有 3 种核心思路，从易到难、从简单到灵活，对比表如下：

| 实现思路                                     | 核心原理                                        | 优点                                     | 缺点                               | 适用场景                             |
| -------------------------------------------- | ----------------------------------------------- | ---------------------------------------- | ---------------------------------- | ------------------------------------ |
| 1. 静态多数据源（手动指定）                  | 配置多个 DataSource，通过注解 / 包名绑定 Mapper | 实现简单，无额外依赖                     | 切换数据源需改代码，不支持动态切换 | 业务分库（固定模块对应固定库）       |
| 2. 动态多数据源（注解切换）                  | 基于 AOP 拦截，通过注解动态切换 DataSource      | 切换灵活（注解指定），开发效率高         | 需手动管理数据源切换，事务支持复杂 | 读写分离、简单动态切换场景           |
| 3. 分布式数据源框架（如 Dynamic-Datasource） | 封装动态数据源，支持自动切换、负载均衡          | 开箱即用，支持读写分离 / 负载均衡 / 事务 | 引入第三方框架，有学习成本         | 复杂多数据源场景（多主多从、多租户） |

### 思路拆解（图文 + 核心代码）

#### 思路 1：静态多数据源（基础版）

**核心逻辑**：

- 配置多个数据源（如 userDataSource、orderDataSource）；
- 给不同业务的 Mapper 绑定不同数据源；
- 调用 Mapper 时自动使用绑定的数据源。

**核心配置（application.yml）**：

```yaml
spring:
  # 数据源1：用户库
  datasource:
    user:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/user_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root
      password: root
    # 数据源2：订单库
    order:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/order_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root
      password: root
```

#### 思路 2：动态多数据源（AOP 版）

**核心逻辑**：

- 配置主从数据源，封装成动态 DataSource；
- 自定义注解（如`@ReadDataSource`/`@WriteDataSource`）；
- 通过 AOP 拦截方法，根据注解切换数据源。

**核心注解示例**：

```java
// 自定义数据源切换注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
    String value() default "master"; // 默认主库
}
```

#### 思路 3：Dynamic-Datasource 框架（推荐）

**核心逻辑**：

- 引入`dynamic-datasource-spring-boot-starter`依赖；
- 配置多数据源，框架自动封装动态数据源；
- 通过注解`@DS("user")`/`@DS("order")`切换，支持读写分离、负载均衡。

**核心优势**：无需手写 AOP，开箱即用，支持事务、多主多从、动态添加数据源。

## 三、实战示例：SpringBoot+MyBatis 多数据源（Dynamic-Datasource 版）

选择目前最主流的`dynamic-datasource`框架实现，兼顾简单性和灵活性。

### 1. 环境准备

#### （1）数据库准备

创建两个库：`user_db`（用户库）、`order_db`（订单库）：

```sql
-- 用户库
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARSET utf8mb4;
USE user_db;
CREATE TABLE user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL);

-- 订单库
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARSET utf8mb4;
USE order_db;
CREATE TABLE `order` (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(50) NOT NULL, user_id BIGINT);
```

#### （2）依赖配置（pom.xml）

```xml
<dependencies>
    <!-- SpringBoot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.0</version>
    </dependency>
    
    <!-- 动态多数据源核心依赖 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
        <version>3.6.1</version>
    </dependency>
    
    <!-- MySQL驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 2. 核心配置（application.yml）

```yaml
spring:
  # 动态多数据源配置
  datasource:
    dynamic:
      primary: user # 默认数据源（用户库）
      strict: false # 关闭严格模式，未指定数据源时用默认
      datasources:
        # 数据源1：用户库
        user:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/user_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: root
          password: root
        # 数据源2：订单库
        order:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/order_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: root
          password: root
      # 可选：读写分离配置（示例）
      # slave:
      #   load-balance: round_robin # 轮询
      #   datasources:
      #     - slave1
      #     - slave2

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.demo.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 3. 代码实现

#### （1）实体类

```java
// 用户实体（user_db）
@Data
public class User {
    private Long id;
    private String username;
}

// 订单实体（order_db）
@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
}
```

#### （2）Mapper 接口 + XML



```java
// UserMapper（默认数据源user）
@Repository
public interface UserMapper {
    @Insert("INSERT INTO user (username) VALUES (#{username})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    User selectById(@Param("id") Long id);
}

// OrderMapper（需指定数据源order）
@Repository
public interface OrderMapper {
    @Insert("INSERT INTO `order` (order_no, user_id) VALUES (#{orderNo}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    List<Order> selectByUserId(@Param("userId") Long userId);
}
```

#### （3）Service 层（注解切换数据源）

```java
// UserService（使用默认数据源user）
@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    public Long addUser(String username) {
        User user = new User();
        user.setUsername(username);
        userMapper.insert(user);
        return user.getId();
    }

    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
}

// OrderService（指定数据源order）
@Service
public class OrderService {
    @Resource
    private OrderMapper orderMapper;

    // 核心：@DS注解指定数据源
    @DS("order")
    public Long addOrder(String orderNo, Long userId) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        orderMapper.insert(order);
        return order.getId();
    }

    // 指定数据源order
    @DS("order")
    public List<Order> getOrderByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }
}
```

#### （4）Controller 层

```java
@RestController
@RequestMapping("/multi-db")
public class MultiDbController {
    @Resource
    private UserService userService;
    @Resource
    private OrderService orderService;

    // 新增用户+新增订单（跨数据源）
    @PostMapping("/user-order")
    public String addUserAndOrder(@RequestParam String username, @RequestParam String orderNo) {
        // 1. 新增用户（默认数据源user_db）
        Long userId = userService.addUser(username);
        // 2. 新增订单（指定数据源order_db）
        Long orderId = orderService.addOrder(orderNo, userId);
        return "新增用户ID：" + userId + "，新增订单ID：" + orderId;
    }

    // 查询用户+订单（跨数据源）
    @GetMapping("/user-order/{userId}")
    public Map<String, Object> getUserAndOrder(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        // 1. 查询用户（user_db）
        User user = userService.getUser(userId);
        // 2. 查询订单（order_db）
        List<Order> orders = orderService.getOrderByUserId(userId);
        result.put("user", user);
        result.put("orders", orders);
        return result;
    }
}
```

### 4. 测试验证

#### （1）新增用户 + 订单

请求：`POST http://localhost:8080/multi-db/user-order?username=test&orderNo=ORDER123456`响应：`新增用户ID：1，新增订单ID：1`验证：

- `user_db.user`表新增一条记录（id=1，username=test）；
- `order_db.order`表新增一条记录（id=1，order_no=ORDER123456，user_id=1）。

#### （2）查询用户 + 订单

请求：`GET http://localhost:8080/multi-db/user-order/1`响应：

json











```json
{
  "user": {"id":1,"username":"test"},
  "orders": [{"id":1,"orderNo":"ORDER123456","userId":1}]
}
```

## 四、关键问题与避坑指南

### 1. 多数据源事务问题

| 问题场景                   | 解决方案                                      |
| -------------------------- | --------------------------------------------- |
| 单数据源事务               | 正常使用`@Transactional`，框架自动绑定数据源  |
| 跨数据源事务（分布式事务） | 需引入 Seata 等分布式事务框架，或避免跨库事务 |

### 2. 常见坑点

| 坑点             | 表现                       | 解决方案                                                     |
| ---------------- | -------------------------- | ------------------------------------------------------------ |
| @DS 注解失效     | 数据源未切换，始终用默认库 | 1. @DS 注解加在 Service 方法上（而非 Mapper）；2. 确保方法是 public；3. 避免内部调用（AOP 失效） |
| 数据源名称写错   | 报错 “找不到数据源”        | 检查 application.yml 中数据源名称与 @DS 注解值一致           |
| 严格模式导致报错 | 未指定数据源时直接报错     | 关闭 strict 模式（strict: false），或指定默认数据源          |

### 3. 性能优化建议

| 优化点           | 具体措施                                     |
| ---------------- | -------------------------------------------- |
| 连接池隔离       | 给每个数据源配置独立的 HikariCP 连接池参数   |
| 读写分离负载均衡 | 配置 slave 节点的负载均衡策略（轮询 / 随机） |
| 避免跨库联表查询 | 先查多个库的数据，再在应用层聚合             |
| 缓存跨库查询结果 | 高频跨库查询结果缓存到 Redis，减少数据库访问 |

## 五、核心总结（图文梳理）

### 多数据源实现思路选型决策树

plaintext











```plaintext
开始 → 简单业务分库（固定绑定）？→ 是→静态多数据源 → 否→需动态切换？
                                                          ↓
                                                      是→动态多数据源（AOP/注解）→ 否→复杂场景（读写分离/多租户）？
                                                                                  ↓
                                                                              是→Dynamic-Datasource框架 → 否→静态多数据源
```

### 核心结论

1. 多数据源的核心场景：业务分库、读写分离、跨库聚合、多租户隔离；
2. 实现思路优先级：Dynamic-Datasource 框架 > 动态多数据源（AOP） > 静态多数据源；
3. 最佳实践：
   - 简单场景：用`@DS`注解指定数据源，开箱即用；
   - 高并发场景：结合读写分离 + 负载均衡；
   - 分布式场景：引入 Seata 解决跨库事务；
4. 避坑关键：注解加在 Service 方法上、避免内部调用、关闭严格模式（开发阶段）。

掌握 MyBatis 多数据源的实现，是处理企业级复杂业务的核心技能，尤其是高并发、大数据量场景下的分库分表、读写分离，都依赖多数据源的基础能力。