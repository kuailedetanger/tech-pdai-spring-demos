## 通俗 + 表格 + 图文 从基础到进阶

本文聚焦 MyBatis 注解方式的核心用法，从 “单表 CRUD” 到 “表关联查询”，再到 “注解与 XML 融合”，用通俗语言、对比表格、实战示例拆解注解方式的使用场景和最佳实践，帮你理清 “什么时候用注解，什么时候用 XML”。

## 一、准备知识：MyBatis 注解方式核心定位

### 1. 注解方式 vs XML 方式（核心对比）

先通过表格明确两种方式的适用场景，避免 “一刀切” 选择：

| 维度          | 注解方式                                  | XML 方式                                |
| ------------- | ----------------------------------------- | --------------------------------------- |
| 核心优势      | 开发效率高（无需维护 XML 文件）、代码内聚 | SQL 与代码分离、易维护、动态 SQL 更灵活 |
| 适用场景      | 简单 SQL（单表 CRUD、简单条件查询）       | 复杂 SQL（联表、动态 SQL、多条件组合）  |
| 可读性        | 简单 SQL 清晰，复杂 SQL 臃肿混乱          | 复杂 SQL 结构清晰，便于调试             |
| 维护成本      | 无需管理 XML 文件，改 SQL 需改代码        | 需维护 XML 文件，改 SQL 无需改代码      |
| 动态 SQL 支持 | 支持（但注解内写脚本可读性差）            | 原生支持，标签丰富（if/where/set）      |

### 2. 为什么 “纯注解不是最佳选择”？

企业级开发中很少用纯注解方式，核心原因：

- ❌ 复杂 SQL（如多表联查、嵌套子查询）写在注解里，代码臃肿、格式混乱，难以调试；
- ❌ 动态 SQL（如多条件过滤）在注解中写`<script>`脚本，可读性远低于 XML；
- ❌ 团队协作成本高：DBA 优化 SQL 需修改 Java 代码，而非独立的 XML 文件；
- ✅ 最佳实践：**简单 SQL 用注解，复杂 SQL 用 XML，两者融合使用**。

## 二、核心注解：单表 CRUD（基础用法）

基于前文的`user`表（id / 用户名 / 密码 / 邮箱 / 创建时间），先实现核心单表操作，所有示例均基于注解完成。

### 项目基础准备（复用前文环境）

1. 依赖：保留`mybatis-spring-boot-starter`、MySQL 驱动等；
2. 配置：`application.yml`中无需配置`mapper-locations`（注解方式无需 XML），仅保留基础配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mybatis_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
mybatis:
  configuration:
    map-underscore-to-camel-case: true  # 自动驼峰映射（create_time → createTime）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印SQL日志
```

1. 实体类：复用`User`实体（lombok + 驼峰字段）。

### 1. 查询操作（核心注解：@Select/@Results/@Result/@Param/@ResultMap）

#### （1）基础查询：@Select + @Param

```java
package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserMapper {
    // 根据ID查询（单参数可省略@Param，但建议统一加，增强可读性）
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    // 模糊查询用户名（多参数必须用@Param指定参数名）
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<User> selectByUsernameLike(@Param("keyword") String keyword);

    // 查询所有用户
    @Select("SELECT * FROM user ORDER BY create_time DESC")
    List<User> selectAll();
}
```

#### （2）结果映射：@Results + @Result（手动指定映射，覆盖自动驼峰）

当自动驼峰映射不生效（如字段名映射特殊），或需要自定义映射时使用：

```java
@Repository
public interface UserMapper {
    // 自定义结果映射（id标记主键，result标记普通字段）
    @Select("SELECT id, username, password, email, create_time FROM user WHERE id = #{id}")
    @Results({
        @Result(id = true, column = "id", property = "id"), // id=true表示主键
        @Result(column = "username", property = "username"),
        @Result(column = "password", property = "password"),
        @Result(column = "email", property = "email"),
        @Result(column = "create_time", property = "createTime") // 手动映射下划线→驼峰
    })
    User selectByIdWithResult(@Param("id") Long id);
}
```

#### （3）复用结果映射：@ResultMap（避免重复写 @Results）

当多个查询需要相同的结果映射时，用`@ResultMap`复用：

```java
@Repository
public interface UserMapper {
    // 定义可复用的结果映射（id="UserResultMap"）
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results(id = "UserResultMap", value = {
        @Result(id = true, column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        @Result(column = "create_time", property = "createTime")
    })
    User selectById(@Param("id") Long id);

    // 复用上面的结果映射
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%')")
    @ResultMap("UserResultMap")
    List<User> selectByUsernameLike(@Param("keyword") String keyword);
}
```

### 2. 表关联查询（注解方式实现多表联查）

以 “用户 - 角色” 多对多关联为例（新增`role`表、`user_role`关联表），演示注解方式的联表查询：

#### （1）Role 实体

```java
@Data
public class Role {
    private Long id;
    private String roleName;
    private String description;
}
```

#### （2）修改 User 实体（新增 roles 字段）

```java
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime;
    private List<Role> roles; // 关联角色
}
```

#### （3）注解实现联表查询（@Many 实现一对多）

```java
@Repository
public interface UserMapper {
    // 查询用户+关联角色（@Many实现一对多）
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results({
        @Result(id = true, column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        @Result(column = "id", property = "roles", // 用user.id作为关联查询的参数
                many = @Many(select = "com.example.demo.mapper.RoleMapper.selectByUserId"))
    })
    User selectUserWithRoles(@Param("id") Long id);
}

// RoleMapper
@Repository
public interface RoleMapper {
    // 根据用户ID查询角色
    @Select("SELECT r.* FROM role r " +
            "JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> selectByUserId(@Param("userId") Long userId);
}
```

### 3. 插入操作（@Insert + 返回自增主键）

#### （1）基础插入

```java
@Repository
public interface UserMapper {
    // 基础插入（无返回主键）
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    int insert(User user);
}
```

#### （2）插入并返回自增主键（核心！）

通过`@Options`注解配置主键生成策略，插入后自动回填实体的 id 字段：

```java
@Repository
public interface UserMapper {
    // 插入并返回自增主键（useGeneratedKeys=true开启主键返回，keyProperty指定实体主键字段）
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertWithId(User user);
}
```

**测试效果**：插入后`user.getId()`能获取到数据库自增的 id。

### 4. 更新操作（@Update + 动态 SQL）

注解方式支持动态 SQL，但需嵌套`<script>`标签，可读性略差：

```java
@Repository
public interface UserMapper {
    // 动态更新（只更新非空字段）
    @Update("<script>" +
            "UPDATE user " +
            "<set>" +
            "   <if test='username != null'>username = #{username},</if>" +
            "   <if test='email != null'>email = #{email}</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(User user);
}
```

### 5. 删除操作（@Delete）

```java
@Repository
public interface UserMapper {
    // 根据ID删除
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // 批量删除（动态SQL）
    @Delete("<script>" +
            "DELETE FROM user WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "   #{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
```

### 6. Provider 注解（动态生成 SQL，解决注解 SQL 臃肿）

当注解内的 SQL 过长（如复杂动态 SQL），可用`@SelectProvider/@InsertProvider`等注解，将 SQL 逻辑抽离到单独类中：

#### （1）定义 SQL Provider 类

```java
package com.example.demo.mapper.provider;

import com.example.demo.entity.User;
import org.apache.ibatis.jdbc.SQL;

public class UserSqlProvider {
    // 动态生成查询SQL
    public String selectByCondition(User user) {
        return new SQL() {{
            SELECT("id, username, email, create_time");
            FROM("user");
            // 动态添加条件
            if (user.getUsername() != null) {
                WHERE("username LIKE CONCAT('%', #{username}, '%')");
            }
            if (user.getEmail() != null) {
                WHERE("email = #{email}");
            }
            ORDER_BY("create_time DESC");
        }}.toString();
    }

    // 动态生成更新SQL
    public String updateById(User user) {
        return new SQL() {{
            UPDATE("user");
            if (user.getUsername() != null) {
                SET("username = #{username}");
            }
            if (user.getEmail() != null) {
                SET("email = #{email}");
            }
            WHERE("id = #{id}");
        }}.toString();
    }
}
```

#### （2）Mapper 中使用 Provider 注解

```java
@Repository
public interface UserMapper {
    // 使用Provider查询
    @SelectProvider(type = UserSqlProvider.class, method = "selectByCondition")
    @ResultMap("UserResultMap")
    List<User> selectByCondition(User user);

    // 使用Provider更新
    @UpdateProvider(type = UserSqlProvider.class, method = "updateById")
    int updateById(User user);
}
```

## 三、注解 + XML 融合（最佳实践）

纯注解和纯 XML 都有短板，企业级开发推荐 “注解 + XML 融合”，核心原则：

> 简单 SQL（单表 CRUD）用注解，复杂 SQL（联表、多条件动态 SQL）用 XML。

### 融合示例

#### 1. Mapper 接口：注解 + XML 混合定义



```java
@Repository
public interface UserMapper {
    // 简单查询：用注解
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    // 复杂联表+动态SQL：用XML（XML文件中定义）
    List<User> selectUserByComplexCondition(@Param("username") String username, 
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
```

#### 2. 对应的 XML 文件（UserMapper.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.UserMapper">
    <!-- 复杂查询：动态条件+联表 -->
    <select id="selectUserByComplexCondition" resultMap="UserResultMap">
        SELECT u.*, r.role_name 
        FROM user u
        LEFT JOIN user_role ur ON u.id = ur.user_id
        LEFT JOIN role r ON ur.role_id = r.id
        <where>
            <if test="username != null and username != ''">
                u.username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="startTime != null">
                AND u.create_time >= #{startTime}
            </if>
            <if test="endTime != null">
                AND u.create_time <= #{endTime}
            </if>
        </where>
        GROUP BY u.id
        ORDER BY u.create_time DESC
    </select>
</mapper>
```

#### 3. 配置文件（兼容注解 + XML）

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml  # 扫描XML文件
  type-aliases-package: com.example.demo.entity
  configuration:
    map-underscore-to-camel-case: true
```

## 四、为什么纯注解不是最佳选择？（核心总结）

用表格梳理纯注解的核心痛点：

| 痛点              | 具体表现                                       | 解决方案                              |
| ----------------- | ---------------------------------------------- | ------------------------------------- |
| 复杂 SQL 可读性差 | 联表 / 动态 SQL 需嵌套`<script>`标签，代码臃肿 | 复杂 SQL 抽离到 XML                   |
| 调试困难          | SQL 写在注解里，无法直接复制到客户端执行       | XML 文件可直接复制 SQL 调试           |
| 团队协作成本高    | DBA 优化 SQL 需修改 Java 代码，而非独立 XML    | XML 集中管理 SQL，便于 DBA / 开发协作 |
| 动态 SQL 灵活性低 | 注解内写 if/foreach 等标签，格式易出错         | XML 的动态 SQL 标签更直观、易维护     |

## 五、完整示例源码（核心文件）

### 1. UserMapper（注解 + XML 融合）

```java
@Repository
public interface UserMapper {
    // 注解：插入返回主键
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    // 注解：基础查询
    @Select("SELECT * FROM user WHERE id = #{id}")
    @ResultMap("UserResultMap")
    User selectById(@Param("id") Long id);

    // XML：复杂查询
    List<User> selectUserByComplexCondition(@Param("username") String username,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    // 注解：动态删除
    @Delete("<script>" +
            "DELETE FROM user WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
```

### 2. UserService/Controller（复用前文逻辑）

Service 和 Controller 层与 XML 方式完全一致，只需注入 Mapper 调用对应方法即可。

### 3. 测试接口（Postman）

| 接口类型 | 地址                                                      | 请求体 / 参数                                                | 说明                     |
| -------- | --------------------------------------------------------- | ------------------------------------------------------------ | ------------------------ |
| POST     | /user                                                     | {"username":"test_anno","password":"123456","email":"test@demo.com"} | 新增用户（注解方式）     |
| GET      | /user/1                                                   | -                                                            | 根据 ID 查询（注解方式） |
| GET      | /user/complex?username=test&startTime=2025-01-01 00:00:00 | -                                                            | 复杂查询（XML 方式）     |

## 六、核心总结（图文梳理）

### MyBatis 注解方式核心流程

```plaintext
客户端请求 → Controller → Service → Mapper接口（注解SQL/XML SQL）→ MyBatis动态代理 → 执行SQL → 结果映射 → 返回客户端
```

### 最佳实践建议

1. 单表简单 CRUD（查 / 改 / 删 / 插）：用注解，提升开发效率；
2. 复杂 SQL（联表、多条件动态 SQL、批量操作）：用 XML，保证可读性和可维护性；
3. 动态 SQL 逻辑复杂（如多条件组合）：用 Provider 注解或 XML；
4. 团队协作场景：XML 集中管理 SQL，便于 DBA 优化和版本控制。

注解方式是 MyBatis 简化开发的重要特性，但需结合场景选择 ——“注解 + XML 融合” 才是企业级项目的最优解，既保留注解的便捷，又兼顾 XML 的灵活性。