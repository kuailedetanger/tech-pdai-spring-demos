在接口文档生成领域，Swagger 系列的 “侵入式注解” 是一把双刃剑 —— 虽然便捷，但会让业务代码掺杂大量非业务注解（比如`@Api`、`@ApiOperation`），破坏代码纯净性。而 Smart-doc 这类工具则走了另一条路：**完全基于 Java 注释生成接口文档，零侵入业务代码**。本文会讲清 “非侵入式” 的核心逻辑、Smart-doc 的使用方式，以及它和 Swagger 的取舍平衡。

## 一、先搞懂：侵入式 vs 非侵入式（通俗对比）

### 1. 核心差异：“是否污染业务代码”

| 类型                  | 通俗解释                                                     | 类比（写作文）                                               |
| --------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 侵入式（Swagger）     | 在接口代码里加专属注解（如`@ApiOperation`），注解是文档生成的核心依据 | 写作文时，在段落里插 “【本段中心思想】”“【此处用了比喻】” 等标注，阅卷老师（文档工具）靠标注打分 |
| 非侵入式（Smart-doc） | 只依赖 Java 原生注释（`/** 注释 */`），代码里无任何文档专属注解 | 写作文时，只按正常格式写内容 + 段落注释，阅卷老师（文档工具）靠读懂注释和代码逻辑打分 |

### 2. 直观示例对比

#### （1）Swagger（侵入式）

```java
@RestController
@RequestMapping("/api/v1/order")
@Api(tags = "订单接口V1") // 文档专属注解（侵入）
public class OrderController {

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询订单") // 文档专属注解（侵入）
    public Result<OrderVO> getOrder(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long id) { // 文档专属注解（侵入）
        return Result.success(new OrderVO(id, "2025-12-16", 99.0));
    }
}
```

#### （2）Smart-doc（非侵入式）

```java
@RestController
@RequestMapping("/api/v1/order")
/**
 * 订单接口V1
 * 负责订单查询、创建、取消等操作
 */ // 原生Java注释（非侵入）
public class OrderController {

    /**
     * 根据ID查询订单
     * @param id 订单ID（必填）
     * @return 订单详情
     */ // 原生Java注释（非侵入）
    @GetMapping("/{id}")
    public Result<OrderVO> getOrder(@PathVariable Long id) { // 无任何文档注解
        return Result.success(new OrderVO(id, "2025-12-16", 99.0));
    }
}
```







## 二、Smart-doc 核心解析：非侵入式的实现思路

### 1. Smart-doc 是什么？

Smart-doc 是一款**纯基于 Java 注释和代码逻辑分析**的 API 文档生成工具，核心特点：

- 零侵入：无需添加任何文档专属注解，只依赖 Javadoc 风格的原生注释；
- 多输出：支持生成 HTML、Markdown、OpenAPI（Swagger）规范文档、Postman 集合等；
- 智能化：能自动解析参数类型、返回值结构、枚举值、甚至 SpringBoot 的请求映射规则；
- 易集成：支持 Maven/Gradle 插件，也能通过代码一键生成。

### 2. 非侵入式的技术思路（通俗讲）

Smart-doc 就像一个 “代码侦探”，它不修改代码，只做 “读代码 + 读注释”：

plaintext











```plaintext
Step1：扫描指定包下的类（如Controller），解析Spring注解（@RestController、@GetMapping等），获取接口URL、请求方法；
Step2：读取类/方法/参数的原生Java注释（/** ... */），提取文档描述、参数说明、返回值说明；
Step3：分析方法的参数类型（如Long、自定义VO）、返回值类型（如Result<OrderVO>），递归解析复杂对象的字段和注释；
Step4：将以上信息按HTML/Markdown/OAS规范组装，生成最终文档。
```

### 3. Smart-doc 核心能力（表格）

| 能力项       | 具体说明                                                | 对比 Swagger                                       |
| ------------ | ------------------------------------------------------- | -------------------------------------------------- |
| 注释解析     | 支持 Javadoc 标准注释（@param、@return、@since 等）     | Swagger 依赖自定义注解，不依赖原生注释             |
| 代码逻辑分析 | 自动解析请求方法、参数位置（Path/Query/Body）、数据类型 | Swagger 需要注解指定（如 @ApiParam），否则无法识别 |
| 复杂对象解析 | 自动解析 VO/DTO 的字段、注释、枚举值                    | 需配合 @ApiModel/@ApiModelProperty 注解            |
| 文档输出格式 | HTML、Markdown、OpenAPI、Postman、Word                  | 原生仅支持 Swagger UI，需插件扩展                  |
| 多模块支持   | 支持多模块项目的接口扫描                                | 需手动配置多分组                                   |
| 调试功能     | 无在线调试（纯文档生成）                                | 自带在线调试功能                                   |
| 版本兼容     | 支持 SpringBoot 2.x/3.x、JDK 8+                         | SpringFox 对 SpringBoot 3.x 兼容差                 |

## 三、Smart-doc 集成案例（SpringBoot 实操）

### 1. 集成步骤（图文流程）

plaintext











```plaintext
Step1：引入Maven插件（核心依赖，无代码侵入）
Step2：编写配置文件（指定扫描包、输出路径等）
Step3：给接口写标准Java注释
Step4：执行Maven命令生成文档
Step5：查看生成的文档（HTML/Markdown）
```

### 2. 具体实操

#### （1）Step1：引入 Maven 插件（pom.xml）

xml











```xml
<build>
    <plugins>
        <!-- Smart-doc Maven插件 -->
        <plugin>
            <groupId>com.github.shalousun</groupId>
            <artifactId>smart-doc-maven-plugin</artifactId>
            <version>2.6.0</version> <!-- 最新版本可查官网 -->
            <configuration>
                <!-- 指定配置文件路径 -->
                <configFile>./src/main/resources/smart-doc.json</configFile>
                <!-- 指定项目名称 -->
                <projectName>订单系统API文档</projectName>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### （2）Step2：编写配置文件（smart-doc.json）

放在`src/main/resources`下，配置扫描规则和输出：

json











```json
{
  "outPath": "src/main/resources/static/doc", // 文档输出路径
  "packageFilters": "com.example.demo.controller", // 扫描的Controller包
  "projectName": "订单系统API文档", // 文档标题
  "isStrict": false, // 非严格模式（允许部分注释缺失）
  "allInOne": true, // 生成单页HTML文档
  "outPutMarkdown": true, // 生成Markdown文档
  "openApi": true, // 生成OpenAPI规范文档（兼容Swagger）
  "openApiVersion": "3.0.0" // OpenAPI版本
}
```

#### （3）Step3：给接口写标准注释（核心）

java



运行









```java
@RestController
@RequestMapping("/api/v1/order")
/**
 * 订单接口V1
 * 包含订单查询、创建、取消等核心接口
 */
public class OrderController {

    /**
     * 根据ID查询订单
     * @param id 订单ID（必填，示例：1001）
     * @return 订单详情VO
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getOrder(@PathVariable Long id) {
        return Result.success(new OrderVO(id, "2025-12-16", 99.0));
    }

    /**
     * 创建订单
     * @param orderDTO 订单创建参数（包含商品ID、数量、金额等）
     * @return 新建订单ID
     */
    @PostMapping("/create")
    public Result<Long> createOrder(@RequestBody OrderDTO orderDTO) {
        return Result.success(1002L);
    }
}

// 复杂对象注释示例
/**
 * 订单VO
 * 订单详情返回对象
 */
public class OrderVO {
    /**
     * 订单ID
     */
    private Long id;
    /**
     * 创建时间（格式：yyyy-MM-dd）
     */
    private String createTime;
    /**
     * 订单金额（单位：元）
     */
    private Double amount;

    // getter/setter省略
}
```

#### （4）Step4：执行 Maven 命令生成文档

在项目根目录执行：

bash



运行









```bash
# 生成HTML+Markdown+OpenAPI文档
mvn smart-doc:html
```

#### （5）Step5：查看文档

- HTML 文档：`src/main/resources/static/doc/index.html`（直接打开即可查看）；
- Markdown 文档：`src/main/resources/static/doc/订单系统API文档.md`；
- OpenAPI 文档：`src/main/resources/static/doc/openapi.json`（可导入 Swagger UI 查看）。

### 3. 生成效果示例（图文）

| 文档类型  | 效果截图描述                                                 | 核心特点                              |
| --------- | ------------------------------------------------------------ | ------------------------------------- |
| HTML 文档 | 左侧接口列表（按 Controller 分组），右侧接口详情（请求方法、URL、参数、返回值、示例） | 结构清晰，无需启动项目即可查看        |
| Markdown  | 按接口分组排版，包含参数表格、返回值结构                     | 适合嵌入 Gitbook、Wiki 等文档平台     |
| OpenAPI   | 符合 OAS 3.0 规范的 JSON 文件                                | 可无缝导入 Swagger UI、Postman 等工具 |

## 四、Smart-doc vs Swagger（全面对比 + 取舍建议）

### 1. 核心对比表格

| 维度       | Smart-doc（非侵入）                      | Swagger+Knife4J（侵入）                 |
| ---------- | ---------------------------------------- | --------------------------------------- |
| 代码侵入性 | 零侵入（仅原生注释）                     | 强侵入（需加大量自定义注解）            |
| 注释依赖   | 依赖 Javadoc 原生注释                    | 依赖自定义注解，原生注释可选            |
| 在线调试   | 无（仅生成静态文档）                     | 支持在线调试（核心优势）                |
| 文档实时性 | 需手动执行命令生成（静态）               | 随项目启动实时更新（动态）              |
| 上手成本   | 低（只需写标准注释）                     | 中（需学习注解用法）                    |
| 功能丰富度 | 侧重文档生成，功能单一                   | 文档 + 调试 + 分组 + 权限控制，功能全面 |
| 生态兼容   | 支持生成 OpenAPI 文档，兼容 Swagger 生态 | 原生支持 OpenAPI，生态完善              |
| 维护成本   | 注释和代码耦合（改代码需同步改注释）     | 注解和代码耦合（改代码需同步改注解）    |

### 2. 取舍建议（核心结论）

| 场景               | 推荐方案                             | 原因                                 |
| ------------------ | ------------------------------------ | ------------------------------------ |
| 追求代码纯净性     | Smart-doc                            | 无额外注解，代码更简洁               |
| 需要在线调试接口   | Swagger+Knife4J                      | 原生支持在线调试，无需导出文档       |
| 团队习惯写原生注释 | Smart-doc                            | 复用现有注释，无需额外学习           |
| 团队需要多端兼容   | Swagger+Knife4J                      | 生态完善，支持对接前端、测试工具     |
| 生产环境文档展示   | Smart-doc（生成静态 HTML）+ 权限控制 | 静态文档更安全，无需暴露接口调试功能 |
| 中小项目快速落地   | Swagger+Knife4J                      | 集成快，开箱即用，调试方便           |

## 五、非侵入式文档生成的局限性（客观认知）

虽然 Smart-doc 做到了 “零侵入”，但也有无法回避的短板：

1. **无在线调试**：生成的是静态文档，无法像 Swagger 那样直接在线调用接口，需配合 Postman 等工具；
2. **注释依赖高**：如果开发人员不写注释 / 注释不规范，文档会缺失关键信息（Swagger 至少有注解兜底）；
3. **复杂场景支持弱**：比如文件上传、多环境配置、接口权限等，需要额外配置，不如 Swagger 注解直观；
4. **实时性差**：代码变更后需手动执行命令重新生成文档，无法像 Swagger 那样 “改代码即更文档”。

## 六、总结：工具只是手段，平衡才是关键

| 核心认知              | 一句话概括                                                   |
| --------------------- | ------------------------------------------------------------ |
| 非侵入式（Smart-doc） | 赢在代码纯净，输在功能单一，适合追求 “代码洁癖” 的团队       |
| 侵入式（Swagger）     | 输在代码侵入，赢在功能全面，适合追求 “高效协作” 的团队       |
| 最终选择              | 多数场景下，Swagger+Knife4J 仍是最优解（功能＞纯净性），但可结合 Smart-doc 生成静态文档用于生产环境归档 |

