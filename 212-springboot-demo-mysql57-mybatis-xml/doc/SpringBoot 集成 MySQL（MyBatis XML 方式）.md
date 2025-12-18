## 通俗 + 表格 + 图文 从原理到实战

本文以**通俗语言拆解核心概念**、**表格对比易混知识点**、**图文梳理集成流程**，系统讲解 SpringBoot 集成 MyBatis（XML 方式）的全流程，先懂 MyBatis 底层逻辑，再落地实战，零基础也能快速上手。

## 一、准备知识：MyBatis 核心原理（先懂 “为什么”）

### 1. 什么是 MyBatis？为什么是 “半自动 ORM”？

先理清 ORM 框架的核心定位，用表格对比常见 ORM 框架：

| 框架      | 定位           | 通俗解释                                    | 自动化程度                   | 核心特点                                     |
| --------- | -------------- | ------------------------------------------- | ---------------------------- | -------------------------------------------- |
| JDBC      | 数据库底层接口 | 直接写 SQL、手动管理连接 / 结果集           | 纯手动                       | 灵活但代码冗余，需处理异常 / 关闭资源        |
| Hibernate | 全自动 ORM     | 面向对象操作，几乎不用写 SQL（靠 HQL）      | 全自动                       | 开发快，复杂 SQL 优化难                      |
| MyBatis   | 半自动 ORM     | 手写 SQL，框架帮你映射 “SQL 结果→Java 对象” | 半自动（SQL 手动，映射自动） | 兼顾灵活（自定义 SQL）和高效（减少冗余代码） |

- **MyBatis 核心价值**：解决 JDBC 的 “代码冗余问题”（如手动封装结果集），又保留 SQL 的 “灵活性”（比 Hibernate 更易优化 SQL）；

- “半自动” 的本质

  ✅ 自动做：SQL 执行、结果集映射（ResultSet→POJO）、连接管理；

  ❌ 手动做：编写 SQL 语句（XML / 注解）、定义参数映射。

### 2. MyBatis 技术演进（从 JDBC 到 MyBatis-Plus）

用时间线 + 图文梳理 MyBatis 的前世今生，理解其解决的核心痛点：

```plaintext
JDBC → 自行封装JDBCUtil → IBatis → MyBatis → MyBatis-Plus → 代码生成工具（MBG）
```

| 阶段            | 核心问题                                             | 解决方案                                      | 缺点                              |
| --------------- | ---------------------------------------------------- | --------------------------------------------- | --------------------------------- |
| JDBC            | 代码冗余（注册驱动、创建连接、处理结果集）、资源泄漏 | 无（纯原生）                                  | 开发效率极低，异常处理繁琐        |
| JDBCUtil        | 简化重复操作（如封装连接池、释放资源）               | 自定义工具类（如 getConnection ()、close ()） | 每个 SQL 仍需手写，结果集映射重复 |
| IBatis（2002）  | 解耦 SQL 和 Java 代码，自动映射结果集                | 配置文件写 SQL，框架处理映射                  | 功能简陋，不支持注解、动态 SQL 弱 |
| MyBatis（2010） | 重构 IBatis，增强动态 SQL、缓存、插件                | XML / 注解写 SQL，完善的映射体系              | 仍需手动写基础 CRUD SQL           |
| MyBatis-Plus    | 简化 CRUD 操作，无需手写基础 SQL                     | 基于 MyBatis 封装，提供通用 Mapper            | 复杂 SQL 仍需手写（兼容 MyBatis） |
| 代码生成工具    | 自动生成 Entity、Mapper、Service、Controller         | MBG（MyBatis Generator）、MP 代码生成器       | 定制化场景需手动调整              |

### 3. Spring+MyBatis 集成方式对比

MyBatis 与 SpringBoot 集成有两种核心方式，按需选择：

| 集成方式 | 适用场景                   | 优点                           | 缺点                        |
| -------- | -------------------------- | ------------------------------ | --------------------------- |
| XML 方式 | 复杂 SQL（联表、动态 SQL） | SQL 与代码分离，易维护、易优化 | 需维护 XML 文件，配置稍多   |
| 注解方式 | 简单 SQL（单表 CRUD）      | 开发快，无需额外 XML 文件      | 复杂 SQL 可读性差，不易调试 |

本文重点讲解**XML 方式**（企业开发中复杂场景的主流选择）。

## 二、实战准备：环境搭建（DB + 依赖 + 配置）

### 1. 数据库准备（DB 定义）

创建测试库和用户表，执行 SQL：

```sql
CREATE DATABASE IF NOT EXISTS mybatis_demo DEFAULT CHARSET utf8mb4;
USE mybatis_demo;

-- 用户表
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 依赖配置（pom.xml）

引入 SpringBoot+MyBatis+MySQL 核心依赖：

```xml
<dependencies>
    <!-- SpringBoot Web核心 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis整合SpringBoot -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.0</version> <!-- 稳定版 -->
    </dependency>
    
    <!-- MySQL驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok（简化实体类） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3. 核心配置（application.yml）

配置数据库连接 + MyBatis 核心参数：

```yaml
spring:
  # 数据库连接配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mybatis_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root  # 替换为你的MySQL用户名
    password: root  # 替换为你的MySQL密码

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/*.xml  # 指定Mapper XML文件路径
  type-aliases-package: com.example.demo.entity  # 实体类别名包（简化XML中的类名）
  configuration:
    map-underscore-to-camel-case: true  # 自动将下划线字段映射为驼峰（如create_time → createTime）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印SQL日志
```

## 三、分层实现：从 Entity 到 Controller（XML 方式）

### 项目结构先理清（核心！）

```plaintext
com.example.demo
├── entity          // 实体类（对应数据库表）
├── mapper          // DAO层（Mapper接口）
├── service         // 服务层
│   ├── impl        // Service实现类
├── controller      // 控制层
├── resources
│   ├── mapper      // Mapper XML文件（与Mapper接口对应）
│   ├── application.yml
└── DemoApplication // 启动类
```

### 1. 第一步：定义实体类（Entity）

```java
package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data  // Lombok自动生成get/set/toString
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime; // 驼峰命名，对应数据库create_time
}
```

### 2. 第二步：定义 Mapper 接口（DAO 层）

Mapper 接口是 MyBatis 的核心，无需实现类（框架动态代理生成）：

```java
package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository  // 标记为持久层组件
public interface UserMapper {
    // 新增用户
    int insert(User user);

    // 根据ID查询用户
    User selectById(@Param("id") Long id);

    // 根据用户名模糊查询
    List<User> selectByUsernameLike(@Param("keyword") String keyword);

    // 修改用户
    int updateById(User user);

    // 删除用户
    int deleteById(@Param("id") Long id);

    // 查询所有用户
    List<User> selectAll();
}
```

### 3. 第三步：编写 Mapper XML 文件（核心！）

在`resources/mapper`下创建`UserMapper.xml`，与 Mapper 接口一一对应：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace必须对应Mapper接口的全类名 -->
<mapper namespace="com.example.demo.mapper.UserMapper">
    <!-- 结果映射（可选，默认驼峰映射已开启） -->
    <resultMap id="UserResultMap" type="User"> <!-- type对应实体类别名（因配置了type-aliases-package） -->
        <id column="id" property="id"/> <!-- 主键映射 -->
        <result column="username" property="username"/>
        <result column="password" property="password"/>
        <result column="email" property="email"/>
        <result column="create_time" property="createTime"/>
    </resultMap>

    <!-- 新增用户（useGeneratedKeys获取自增ID，keyProperty映射到实体id） -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, email)
        VALUES (#{username}, #{password}, #{email})
    </insert>

    <!-- 根据ID查询 -->
    <select id="selectById" resultMap="UserResultMap">
        SELECT * FROM user WHERE id = #{id}
    </select>

    <!-- 模糊查询用户名 -->
    <select id="selectByUsernameLike" resultMap="UserResultMap">
        SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%')
    </select>

    <!-- 修改用户（动态SQL：只更新非空字段） -->
    <update id="updateById">
        UPDATE user
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="password != null">password = #{password},</if>
            <if test="email != null">email = #{email}</if>
        </set>
        WHERE id = #{id}
    </update>

    <!-- 删除用户 -->
    <delete id="deleteById">
        DELETE FROM user WHERE id = #{id}
    </delete>

    <!-- 查询所有 -->
    <select id="selectAll" resultMap="UserResultMap">
        SELECT * FROM user ORDER BY create_time DESC
    </select>
</mapper>
```

### 4. 第四步：Service 层（业务逻辑）

#### （1）Service 接口

```java
package com.example.demo.service;

import com.example.demo.entity.User;
import java.util.List;

public interface UserService {
    // 新增用户
    boolean addUser(User user);

    // 根据ID查询
    User getUserById(Long id);

    // 模糊查询
    List<User> listUserByUsername(String keyword);

    // 修改用户
    boolean updateUser(User user);

    // 删除用户
    boolean deleteUser(Long id);

    // 查询所有
    List<User> listAllUser();
}
```

#### （2）Service 实现类

```java
package com.example.demo.service.impl;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource  // 注入Mapper（MyBatis动态代理生成的实现类）
    private UserMapper userMapper;

    @Override
    public boolean addUser(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<User> listUserByUsername(String keyword) {
        return userMapper.selectByUsernameLike(keyword);
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean deleteUser(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public List<User> listAllUser() {
        return userMapper.selectAll();
    }
}
```

### 5. 第五步：Controller 层（接口暴露）

```java
package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // 新增用户
    @PostMapping
    public ResponseEntity<String> addUser(@RequestBody User user) {
        boolean success = userService.addUser(user);
        return success ? ResponseEntity.ok("新增成功") : ResponseEntity.badRequest().body("新增失败");
    }

    // 根据ID查询
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // 模糊查询
    @GetMapping("/like")
    public ResponseEntity<List<User>> listUserByUsername(@RequestParam String keyword) {
        List<User> users = userService.listUserByUsername(keyword);
        return ResponseEntity.ok(users);
    }

    // 修改用户
    @PutMapping
    public ResponseEntity<String> updateUser(@RequestBody User user) {
        boolean success = userService.updateUser(user);
        return success ? ResponseEntity.ok("修改成功") : ResponseEntity.badRequest().body("修改失败");
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? ResponseEntity.ok("删除成功") : ResponseEntity.badRequest().body("删除失败");
    }

    // 查询所有
    @GetMapping("/all")
    public ResponseEntity<List<User>> listAllUser() {
        List<User> users = userService.listAllUser();
        return ResponseEntity.ok(users);
    }
}
```

### 6. 第六步：启动类（关键注解）

需添加`@MapperScan`注解，扫描 Mapper 接口包（避免手动注入失败）：

```java
package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.demo.mapper")  // 扫描Mapper接口包
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 四、运行测试（验证效果）

### 1. 启动项目

运行`DemoApplication`，控制台打印 MyBatis 的 SQL 日志，说明配置成功。

### 2. 接口测试（Postman / 浏览器）

#### （1）新增用户

- 请求地址：`POST http://localhost:8080/user`
- 请求体（JSON）：

```json
{
    "username": "test_mybatis",
    "password": "123456",
    "email": "test@demo.com"
}
```

- 响应：`新增成功`，数据库 user 表新增一条记录。

#### （2）查询用户

- 请求地址：`GET http://localhost:8080/user/1`
- 响应：返回 ID=1 的用户信息（含自增 ID、创建时间）。

#### （3）动态修改用户

- 请求地址：`PUT http://localhost:8080/user`
- 请求体（JSON）：

```json
{
    "id": 1,
    "email": "update@demo.com"
}
```

- 响应：`修改成功`，仅更新 email 字段（password/username 不变）。

## 五、核心知识点总结（图文梳理）

### 1. MyBatis XML 方式核心流程

```plaintext
客户端请求 → Controller → Service → Mapper接口（动态代理）→ Mapper XML（SQL）→ MyBatis框架 → 数据库 → 结果映射 → 返回客户端
```

### 2. 关键注意事项

| 注意点          | 常见问题                                    | 解决方案                                           |
| --------------- | ------------------------------------------- | -------------------------------------------------- |
| namespace 错误  | XML 的 namespace 与 Mapper 接口全类名不一致 | 严格保持一致（复制粘贴避免手写错误）               |
| 字段映射失败    | 数据库下划线字段未映射为驼峰                | 开启`map-underscore-to-camel-case: true`           |
| Mapper 注入失败 | 未加`@MapperScan`或扫描包错误               | 启动类添加`@MapperScan("com.example.demo.mapper")` |
| 动态 SQL 语法错 | if 标签缺少 test、set 标签逗号问题          | 严格遵循 MyBatis 动态 SQL 语法，测试空值判断       |

### 3. MyBatis-Plus 扩展（简化 CRUD）

如果想减少基础 CRUD 的 XML 编写，可集成 MyBatis-Plus：

- 依赖：新增`mybatis-plus-boot-starter`；
- 核心：`UserMapper`继承`BaseMapper<User>`，直接使用`selectById`/`insert`等方法，无需写 XML；
- 兼容：复杂 SQL 仍可通过 XML / 注解补充，完全兼容 MyBatis。

## 六、核心价值回顾

MyBatis XML 方式的核心优势：

1. **SQL 与代码分离**：XML 集中管理 SQL，便于 DBA 优化、团队协作；
2. **动态 SQL 强大**：通过 if/set/where 等标签灵活拼接 SQL，适配复杂业务场景；
3. **完全掌控 SQL**：比 Hibernate 更易优化性能（如索引、联表查询）；
4. **SpringBoot 集成简单**：少量配置即可快速上手，兼容主流数据源 / 连接池。

掌握 MyBatis XML 方式，是企业级项目中处理复杂数据库操作的核心技能，也是从 “初级开发” 到 “中级开发” 的关键一步。



# SpringBoot 集成 MyBatis 案例中数据库连接池的核心解析

在你上述的 SpringBoot+MyBatis 集成案例中，**会自动使用数据库连接池**，且无需手动引入额外连接池依赖 ——SpringBoot 有默认的连接池适配逻辑，下面从 “为什么自动用”“默认连接池是谁”“默认配置值”“如何自定义” 三个维度详细拆解。

## 一、为什么会自动使用连接池？

SpringBoot 对数据库连接池做了 “自动配置”，核心逻辑如下：

1. **依赖传递**：`mybatis-spring-boot-starter` 依赖 `spring-boot-starter-jdbc`，而 `spring-boot-starter-jdbc` 内置了连接池的自动配置逻辑；
2. **自动配置类**：SpringBoot 的 `DataSourceAutoConfiguration` 会自动检测类路径下的连接池实现，优先选择性能更优的连接池；
3. **无感知使用**：开发者只需配置`spring.datasource`相关参数，框架会自动创建连接池并管理连接，无需手动编写连接池代码。

## 二、默认的连接池是谁？（SpringBoot 2.x/3.x 差异）

SpringBoot 不同版本的默认连接池不同，核心规则是 “按优先级自动选择”：

| SpringBoot 版本 | 默认连接池                   | 优先级（类路径存在则优先）                  |
| --------------- | ---------------------------- | ------------------------------------------- |
| 2.0.x ~ 2.7.x   | HikariCP（默认，最高优先级） | HikariCP > Tomcat JDBC Pool > Commons DBCP2 |
| 3.0.x+          | HikariCP（唯一默认）         | 仅保留 HikariCP，移除其他连接池的自动配置   |

> 注：在你的案例中，只要引入了`spring-boot-starter-jdbc`（MyBatis starter 已间接引入），且未排除 HikariCP 依赖，默认就是 HikariCP—— 这是目前性能最优的连接池，也是 SpringBoot 官方推荐的。

## 三、默认配置值（核心！HikariCP 默认参数）

HikariCP 的默认配置是 SpringBoot 内置的，无需手动配置即可使用，核心默认值如下表（通俗解释 + 实际含义）：

| 配置项                  | 默认值               | 通俗解释                                    | 实际作用                                                     |
| ----------------------- | -------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| `maximum-pool-size`     | 10                   | 连接池最多能创建 10 个数据库连接            | 控制最大并发连接数，避免连接过多压垮数据库                   |
| `minimum-idle`          | 10（与 max 一致）    | 连接池最少保持 10 个空闲连接                | SpringBoot 2.x 中默认与 max 相同（避免频繁创建 / 销毁连接），3.x 中默认是`5` |
| `idle-timeout`          | 600000ms（10 分钟）  | 空闲连接超过 10 分钟未使用则被回收          | 释放闲置连接，节省资源                                       |
| `connection-timeout`    | 30000ms（3 秒）      | 获取连接的超时时间（超过 3 秒没拿到则报错） | 避免线程因等待连接卡死                                       |
| `max-lifetime`          | 1800000ms（30 分钟） | 一个连接的最大存活时间（超过则强制关闭）    | 防止数据库端关闭连接但客户端不知情                           |
| `connection-test-query` | 无（自动检测）       | 测试连接是否可用的 SQL（如 SELECT 1）       | HikariCP 会自动适配数据库方言，无需手动配置                  |
| `pool-name`             | HikariPool-1         | 连接池名称（便于日志排查）                  | 多数据源时区分不同连接池                                     |

### 补充：默认配置的加载来源

这些默认值定义在 SpringBoot 的`HikariDataSourceConfiguration`和 HikariCP 自身的`HikariConfig`类中，无需在`application.yml`中配置，框架会自动生效。

## 四、如何验证 / 修改连接池配置？

### 1. 验证默认连接池（两种方式）

#### 方式 1：查看启动日志

启动项目后，控制台会打印如下日志（说明使用 HikariCP）：

```plaintext
2025-12-17 10:00:00.000  INFO 12345 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2025-12-17 10:00:00.100  INFO 12345 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
```

#### 方式 2：代码验证

在 Controller/Service 中注入`DataSource`，打印其类型：

```java
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private DataSource dataSource;

    @GetMapping("/datasource")
    public String testDataSource() {
        // 打印结果：class com.zaxxer.hikari.HikariDataSource
        System.out.println("当前连接池类型：" + dataSource.getClass());
        return "当前连接池类型：" + dataSource.getClass().getSimpleName();
    }
}
```

### 2. 自定义连接池配置（推荐）

默认配置仅适用于开发环境，生产环境建议根据业务调整，在`application.yml`中添加如下配置：

```yaml
spring:
  datasource:
    # 基础连接配置
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mybatis_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    # HikariCP自定义配置
    hikari:
      maximum-pool-size: 20  # 生产环境根据QPS调整（如20-50）
      minimum-idle: 5        # 空闲连接数（建议小于max）
      idle-timeout: 300000   # 5分钟回收空闲连接
      connection-timeout: 5000 # 5秒获取连接超时
      max-lifetime: 1800000  # 30分钟连接存活时间
      pool-name: MyBatisHikariPool # 自定义连接池名称
      connection-test-query: SELECT 1  # 手动指定测试SQL（可选）
```

## 五、常见误区澄清

1. **“没配置连接池就不会用”**：错误 ——SpringBoot 的自动配置会默认启用 HikariCP，无需手动配置连接池参数也会生效；
2. **“默认连接池性能差”**：错误 ——HikariCP 是目前性能最优的连接池（比 Tomcat JDBC、DBCP2 快数倍），默认配置能满足大部分开发场景；
3. **“必须引入额外连接池依赖”**：错误 ——`mybatis-spring-boot-starter` 间接依赖了 HikariCP，无需单独引入。

## 总结

在你提供的 SpringBoot+MyBatis XML 案例中：✅ 自动使用连接池，默认是**HikariCP**；✅ 核心默认值：最大连接数 10、最小空闲连接 10、获取连接超时 3 秒、空闲连接回收 10 分钟；✅ 生产环境建议自定义`spring.datasource.hikari`参数，适配业务并发量。