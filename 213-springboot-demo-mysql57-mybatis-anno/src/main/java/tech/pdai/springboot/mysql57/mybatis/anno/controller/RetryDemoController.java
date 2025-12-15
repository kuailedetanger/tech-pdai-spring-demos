package tech.pdai.springboot.mysql57.mybatis.anno.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.pdai.springboot.mysql57.mybatis.anno.service.impl.RetryDemoService;

/**
 * 重试机制演示控制器
 * 
 * @author pdai
 */
@RestController
@RequestMapping("/retry")
public class RetryDemoController {

    /**
     * 重试服务实例
     */
    private final RetryDemoService retryDemoService;

    /**
     * 构造函数注入重试服务
     * 
     * @param retryDemoService 重试服务
     */
    public RetryDemoController(RetryDemoService retryDemoService) {
        this.retryDemoService = retryDemoService;
    }

    /**
     * 调用第三方API接口
     * 
     * @return 调用结果字符串
     */
    @ApiOperation("测试Spring Retry 重试机制")
    @GetMapping("/call")
    public String callThirdPartyApi() {
        return retryDemoService.callThirdPartyApi();
    }
}
