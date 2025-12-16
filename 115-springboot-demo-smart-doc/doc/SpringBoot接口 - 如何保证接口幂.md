## 一、先搞懂：什么是幂等？什么是接口幂等？

### 1. 幂等的通俗定义

幂等（Idempotent）原本是数学概念，比如 `1*1=1`（多次运算结果不变），引申到编程领域：

> **无论对同一个操作执行多少次，最终的结果和执行一次的结果完全一致，且不会产生副作用**。

**生活类比**：

- 非幂等操作：给手机充 100 元话费（重复操作会扣多次钱）；
- 幂等操作：查询手机话费余额（查 1 次和查 10 次结果一样，无副作用）。

### 2. 接口幂等的核心定义

> 对于同一个接口的**相同请求**，无论调用 1 次还是 N 次，服务端的业务结果都是一致的，且不会重复创建数据、重复扣减库存、重复扣款等。

### 3. 为什么接口会重复调用？（幂等性的必要性）

接口重复调用几乎是分布式系统的 “必然场景”，常见原因：

| 重复调用原因        | 通俗示例                                    | 业务风险                   |
| ------------------- | ------------------------------------------- | -------------------------- |
| 前端重复提交        | 用户点击 “下单” 按钮时网络卡顿，连续点 3 次 | 生成 3 个订单、扣 3 次库存 |
| 网络抖动 / 超时重试 | 客户端调用接口后未收到响应，触发重试机制    | 重复扣款、重复发送短信     |
| 微服务异步调用重试  | 消息队列重复消费、Feign 重试机制触发        | 重复创建数据、重复更新状态 |
| 第三方平台回调重复  | 支付平台多次回调 “支付成功” 接口            | 重复入账、重复发货         |

**图文理解**：

```plaintext
【问题场景】
用户 → 点击下单 → 前端 → 调用订单接口（网络卡顿）→ 前端重试 → 后端接收到3次相同请求 → 生成3个订单（非幂等）

【幂等保障后】
用户 → 点击下单 → 前端 → 调用订单接口（网络卡顿）→ 前端重试 → 后端接收到3次相同请求 → 仅生成1个订单（幂等）
```

## 二、常见的保证接口幂等的方式（对比 + 选型）

不同幂等方案适配不同业务场景，先通过表格对比核心特征，再逐个拆解原理和实操：

| 幂等方案                        | 核心原理                                              | 优点                           | 缺点                                   | 适用场景                               |
| ------------------------------- | ----------------------------------------------------- | ------------------------------ | -------------------------------------- | -------------------------------------- |
| 数据库悲观锁                    | 加锁后独占资源，其他请求阻塞直到锁释放                | 简单可靠、无并发问题           | 性能差、易死锁、阻塞请求               | 低并发、核心数据更新（如订单状态修改） |
| 数据库唯一 ID / 唯一索引        | 给请求唯一标识建唯一索引，重复请求触发主键 / 索引冲突 | 实现简单、性能高、无阻塞       | 仅能防重复创建，无法处理更新类操作     | 重复提交（如创建订单、提交表单）       |
| 数据库乐观锁（版本号 / 时间戳） | 基于版本号控制，仅当版本匹配时才更新数据              | 无阻塞、性能高、支持更新类操作 | 需额外字段、高并发下可能重试失败       | 高并发更新（如库存扣减、余额修改）     |
| 分布式锁（Redis/ZooKeeper）     | 先抢锁，抢到锁才执行业务，重复请求抢不到锁            | 适用分布式场景、控制粒度灵活   | 依赖中间件、需处理锁超时 / 释放问题    | 分布式系统的核心操作（如分布式下单）   |
| Token 机制                      | 前端先获取 Token，调用接口时携带，服务端验证并销毁    | 通用性强、支持各类接口         | 需额外接口（获取 Token）、增加交互成本 | 前端重复提交、第三方回调等通用场景     |

## 三、各幂等方案实操详解（原理 + 案例 + 源码）

### 方案 1：数据库悲观锁（Lock）

#### 1. 核心原理

通过数据库的`for update`语句给数据行加排他锁，同一时刻只有一个请求能获取锁并执行业务，其他请求阻塞等待，直到锁释放后发现数据已处理，直接返回结果。

#### 2. 适用场景

订单状态更新（如 “待支付”→“已支付”）、库存扣减（低并发）等核心更新操作。

#### 3. 实操案例（订单状态更新）

##### （1）Mapper 层（MyBatis）

```xml
<!-- 查询并加悲观锁，仅当订单状态为待支付时锁定 -->
<select id="getOrderForUpdate" parameterType="Long" resultType="Order">
    SELECT * FROM t_order WHERE id = #{id} AND status = 0 FOR UPDATE
</select>

<!-- 更新订单状态 -->
<update id="updateOrderStatus">
    UPDATE t_order SET status = #{status} WHERE id = #{id}
</update>
```

##### （2）Service 层

```java
@Service
@Transactional // 事务必须加，否则锁会立即释放
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    public boolean updateOrderStatus(Long orderId) {
        // 1. 查询订单并加悲观锁（阻塞其他请求）
        Order order = orderMapper.getOrderForUpdate(orderId);
        if (order == null) {
            // 订单已被处理（状态非待支付），直接返回成功（幂等）
            return true;
        }
        // 2. 执行业务逻辑（如修改状态为已支付）
        orderMapper.updateOrderStatus(orderId, 1);
        return true;
    }
}
```

#### 4. 核心注意点

- 必须在事务中使用`for update`，否则锁会查询后立即释放；
- 锁定范围要尽可能小（加条件`status = 0`），避免锁全表；
- 高并发下会导致请求阻塞，性能差，仅适合低并发场景。

### 方案 2：数据库唯一 ID / 唯一索引

#### 1. 核心原理

给每个请求分配一个**唯一标识**（如订单号、业务流水号、前端生成的 UUID），并在数据库中为该字段建立唯一索引。当重复请求到达时，数据库会抛出 “唯一索引冲突” 异常，服务端捕获异常后直接返回 “处理成功”，实现幂等。

#### 2. 适用场景

创建订单、提交表单、生成记录等 “写数据” 场景（仅防重复创建）。

#### 3. 实操案例（创建订单）

##### （1）数据库表设计（添加唯一索引）

```sql
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL COMMENT '订单号（唯一）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付',
    CREATE_TIME DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_order_no (order_no) -- 唯一索引：防重复订单
);
```

##### （2）Service 层

```java
@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Transactional
    public Result createOrder(OrderDTO orderDTO) {
        try {
            // 1. 构建订单（orderNo为唯一标识，由前端/服务端生成）
            Order order = new Order();
            order.setOrderNo(orderDTO.getOrderNo());
            order.setUserId(orderDTO.getUserId());
            order.setAmount(orderDTO.getAmount());
            // 2. 插入数据库（重复订单会触发唯一索引冲突）
            orderMapper.insert(order);
            return Result.success("创建订单成功");
        } catch (DuplicateKeyException e) {
            // 捕获唯一索引冲突异常，返回成功（幂等）
            log.warn("订单已存在，orderNo：{}", orderDTO.getOrderNo());
            return Result.success("创建订单成功");
        }
    }
}
```

#### 4. 核心注意点

- 唯一标识要全局唯一（如 UUID、雪花算法 ID），避免重复；
- 仅能防重复创建，无法处理 “更新类” 操作（如修改订单状态）；
- 依赖数据库异常捕获，需确保异常类型准确（MyBatis 的`DuplicateKeyException`）。

### 方案 3：数据库乐观锁（版本号 / 时间戳）

#### 1. 核心原理

乐观锁假设 “并发冲突概率低”，不主动加锁，而是通过**版本号字段**（如`version`）或时间戳控制：

- 每次查询数据时获取当前版本号；
- 更新数据时，仅当版本号匹配（`version = 传入版本号`）才执行更新；
- 重复请求会因版本号不匹配，更新行数为 0，判定为 “已处理”，实现幂等。

#### 2. 适用场景

高并发下的库存扣减、余额修改、订单状态更新等 “更新类” 操作。

#### 3. 实操案例（库存扣减）

##### （1）数据库表设计（添加 version 字段）

sql











```sql
CREATE TABLE t_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    stock_num INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    UNIQUE INDEX idx_product_id (product_id)
);
```

##### （2）Mapper 层

xml











```xml
<!-- 查询库存（获取版本号） -->
<select id="getStockByProductId" parameterType="Long" resultType="Stock">
    SELECT * FROM t_stock WHERE product_id = #{productId}
</select>

<!-- 乐观锁更新库存：仅当版本号匹配时扣减 -->
<update id="deductStock">
    UPDATE t_stock 
    SET stock_num = stock_num - #{deductNum}, version = version + 1 
    WHERE product_id = #{productId} AND version = #{version}
</update>
```

##### （3）Service 层（带重试机制）

java



运行









```java
@Service
public class StockService {
    @Autowired
    private StockMapper stockMapper;

    @Transactional
    public Result deductStock(Long productId, int deductNum) {
        // 1. 查询库存（获取当前版本号）
        Stock stock = stockMapper.getStockByProductId(productId);
        if (stock == null || stock.getStockNum() < deductNum) {
            return Result.fail("库存不足");
        }
        // 2. 乐观锁扣减库存
        int updateRows = stockMapper.deductStock(productId, deductNum, stock.getVersion());
        if (updateRows == 0) {
            // 版本号不匹配（重复请求/并发更新），返回成功（幂等）
            log.warn("库存已扣减，productId：{}", productId);
            return Result.success("库存扣减成功");
        }
        return Result.success("库存扣减成功");
    }
}
```

#### 4. 核心注意点

- 版本号必须自增（`version + 1`），确保每次更新后版本唯一；
- 高并发下可能出现 “更新行数为 0”，需根据业务判定是否重试；
- 时间戳方案（`update_time = #{updateTime}`）精度低（毫秒级），高并发下易冲突，优先选版本号。

### 方案 4：分布式锁（Redis 实现）

#### 1. 核心原理

利用 Redis 的`SETNX`（SET if Not Exists）命令实现分布式锁：

- 重复请求的唯一标识（如订单号）作为锁 Key；
- 第一个请求抢到锁后执行业务，执行完成释放锁；
- 后续请求抢不到锁，判定为 “已处理”，直接返回结果。

#### 2. 适用场景

分布式系统中的核心操作（如跨服务下单、分布式库存扣减）。

#### 3. 实操案例（Redis 分布式锁）

##### （1）Redis 锁工具类

java



运行









```java
@Component
public class RedisLockUtil {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取分布式锁
     * @param lockKey 锁Key（如order:1001）
     * @param expireTime 过期时间（秒）：防止死锁
     * @return 是否抢到锁
     */
    public boolean lock(String lockKey, long expireTime) {
        // SETNX + 过期时间（原子操作，防止死锁）
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}
```

##### （2）Service 层

java



运行









```java
@Service
public class OrderService {
    @Autowired
    private RedisLockUtil redisLockUtil;
    @Autowired
    private OrderMapper orderMapper;

    public Result createOrder(OrderDTO orderDTO) {
        // 1. 构建分布式锁Key（唯一标识：订单号）
        String lockKey = "order:lock:" + orderDTO.getOrderNo();
        try {
            // 2. 抢锁（过期时间5秒，防止死锁）
            boolean locked = redisLockUtil.lock(lockKey, 5);
            if (!locked) {
                // 抢不到锁，说明重复请求，返回成功（幂等）
                return Result.success("创建订单成功");
            }
            // 3. 检查订单是否已存在（双重校验）
            Order existOrder = orderMapper.getByOrderNo(orderDTO.getOrderNo());
            if (existOrder != null) {
                return Result.success("创建订单成功");
            }
            // 4. 执行业务逻辑（创建订单）
            Order order = new Order();
            order.setOrderNo(orderDTO.getOrderNo());
            order.setUserId(orderDTO.getUserId());
            order.setAmount(orderDTO.getAmount());
            orderMapper.insert(order);
            return Result.success("创建订单成功");
        } finally {
            // 5. 释放锁（必须在finally中，防止业务异常导致锁无法释放）
            redisLockUtil.unlock(lockKey);
        }
    }
}
```

#### 4. 核心注意点

- 锁必须设置过期时间，防止服务宕机导致死锁；
- 锁 Key 要唯一（关联业务唯一标识），避免锁冲突；
- 释放锁要在`finally`中，确保无论业务是否异常都能释放；
- 高并发下可结合 Redisson 实现可重入锁、公平锁，简化锁管理。

### 方案 5：Token 机制（通用方案）

#### 1. 核心原理

通过 “预生成 Token + 验证销毁 Token” 实现幂等，核心流程：

plaintext











```plaintext
Step1：前端请求“获取Token”接口，服务端生成唯一Token（如UUID），存入Redis（设置过期时间），返回给前端；
Step2：前端调用业务接口时，携带该Token；
Step3：服务端验证Token：
       - 若Token存在，删除Token并执行业务；
       - 若Token不存在（已被删除），判定为重复请求，直接返回结果。
```

#### 2. 适用场景

前端重复提交、第三方回调、接口超时重试等通用场景。

#### 3. 实操案例（表单提交）

##### （1）Token 生成接口（Controller）

java



运行









```java
@RestController
@RequestMapping("/token")
public class TokenController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/get")
    public Result getToken() {
        // 1. 生成唯一Token
        String token = UUID.randomUUID().toString();
        // 2. 存入Redis（过期时间30分钟，防止Token长期有效）
        redisTemplate.opsForValue().set("token:" + token, "1", 30, TimeUnit.MINUTES);
        return Result.success(token);
    }
}
```

##### （2）业务接口（带 Token 验证）

java



运行









```java
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result createOrder(@RequestHeader("Token") String token, @RequestBody OrderDTO orderDTO) {
        // 1. 验证Token（删除操作是原子的，防止并发问题）
        String redisKey = "token:" + token;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (Boolean.FALSE.equals(deleted)) {
            // Token不存在（已被删除），重复请求
            return Result.success("订单创建成功");
        }
        // 2. 执行业务逻辑
        return orderService.createOrder(orderDTO);
    }
}
```

##### （3）前端调用流程

javascript



运行









```javascript
// Step1：获取Token
axios.get("/token/get").then(res => {
    const token = res.data.data;
    // Step2：携带Token调用下单接口
    axios.post("/order/create", {
        orderNo: "20251216001",
        userId: 1001,
        amount: 99.0
    }, {
        headers: { Token: token }
    }).then(res => {
        console.log(res.data);
    });
});
```

#### 4. 核心注意点

- Token 必须一次性使用（验证后立即删除），避免重复使用；
- Token 要设置过期时间，防止 Redis 中堆积大量无效 Token；
- 验证 Token 的操作必须是原子的（Redis 的`delete`是原子操作），避免并发问题。

## 四、幂等方案选型指南（图文总结）

### 1. 选型决策树

plaintext











```plaintext
业务场景 → 是否创建数据？
    → 是 → 低并发：唯一索引 / 高并发：分布式锁
    → 否 → 是否更新数据？
        → 是 → 低并发：悲观锁 / 高并发：乐观锁
        → 否 → 通用场景：Token机制
```

### 2. 核心避坑点

| 常见坑点               | 解决方案                                            |
| ---------------------- | --------------------------------------------------- |
| 幂等方案与业务不匹配   | 按 “创建 / 更新” 场景选型，避免用乐观锁处理创建操作 |
| 分布式锁未设置过期时间 | 必须加过期时间，结合 Redisson 实现自动续期          |
| 乐观锁无重试机制       | 高并发下可添加重试（最多 3 次），避免更新失败       |
| Token 在前端存储不当   | Token 仅在会话中存储，用完即弃，不持久化            |

## 五、完整示例源码（核心片段汇总）

### 1. 依赖（pom.xml）

xml











```xml
<!-- SpringBoot核心 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
<!-- MySQL驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 核心配置（application.yml）





```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.demo.entity
```

## 六、总结

| 核心结论   | 一句话概括                                                   |
| ---------- | ------------------------------------------------------------ |
| 幂等性本质 | 让重复请求的业务结果一致，无副作用                           |
| 选型核心   | 按 “创建 / 更新” 场景选方案，优先选无阻塞、高性能的方案（乐观锁 / 唯一索引 / Token） |
| 落地关键   | 必须结合业务唯一标识，且处理异常 / 超时 / 并发问题           |

