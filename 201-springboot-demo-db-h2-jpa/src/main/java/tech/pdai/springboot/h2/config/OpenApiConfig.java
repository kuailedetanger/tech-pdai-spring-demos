package tech.pdai.springboot.h2.config;

import java.util.ArrayList;
import java.util.List;

import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.RequestParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * swagger配置类，用于配置OpenAPI文档.
 *
 * @author pdai
 */
@Configuration
@EnableOpenApi
public class OpenApiConfig {

    /**
     * Knife4j提供的OpenAPI扩展解析器，用于增强Swagger功能.
     */
    private final OpenApiExtensionResolver openApiExtensionResolver;

    @Autowired
    public OpenApiConfig(OpenApiExtensionResolver openApiExtensionResolver) {
        this.openApiExtensionResolver = openApiExtensionResolver;
    }

    /**
     * 配置Swagger文档的核心Bean.
     * 
     * @return Docket对象，Swagger的主要配置载体
     */
    @Bean
    public Docket openApi() {
        // 定义API分组名称
        String groupName = "Test Group";
        return new Docket(DocumentationType.OAS_30)
                .groupName(groupName) // 设置分组名称
                .apiInfo(apiInfo()) // 设置API基本信息
                .select() // 开始选择API接口
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class)) // 只扫描带有@ApiOperation注解的方法
                .paths(PathSelectors.any()) // 对所有路径生效
                .build() // 构建Docket对象
                .globalRequestParameters(getGlobalRequestParameters()) // 添加全局请求参数
                .extensions(openApiExtensionResolver.buildExtensions(groupName)) // 添加Knife4j扩展
                .extensions(openApiExtensionResolver.buildSettingExtensions()); // 添加Knife4j设置扩展
    }

    /**
     * 获取全局请求参数列表.
     * 
     * @return 全局请求参数列表
     */

    /**
     * 获取全局请求参数列表，用于在所有API接口中添加统一的请求参数.
     * 目前添加了一个名为"AppKey"的可选查询参数，用于应用身份验证.
     * 
     * @return 全局请求参数列表，包含AppKey参数配置
     */
    private List<RequestParameter> getGlobalRequestParameters() {
        List<RequestParameter> parameters = new ArrayList<>();
        // 添加AppKey作为全局查询参数，用于应用身份验证
        parameters.add(new RequestParameterBuilder()
                .name("AppKey") // 参数名称
                .description("应用密钥，用于标识和验证调用方身份") // 参数描述
                .required(false) // 是否必填：否
                .in(ParameterType.QUERY) // 参数位置：查询参数
                .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING))) // 参数类型：字符串
                .build());
        return parameters;
    }

    /**
     * 构建API基本信息.
     * 
     * @return ApiInfo对象，包含API的基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("My API") // API标题
                .description("测试用API接口文档") // API描述
                .contact(new Contact("pdai", "http://pdai.tech", "suzhou.daipeng@gmail.com")) // 联系人信息
                .termsOfServiceUrl("http://xxxxxx.com/") // 服务条款URL
                .version("1.0") // 版本号
                .build(); // 构建ApiInfo对象
    }
}