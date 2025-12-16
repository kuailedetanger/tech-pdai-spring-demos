package tech.pdai.springboot.api.sign.config.sign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import cn.hutool.core.text.CharSequenceUtil;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tech.pdai.springboot.api.sign.config.exception.BusinessException;
import tech.pdai.springboot.api.sign.util.SignUtil;

/**
 * API签名验证切面类
 * 用于拦截带有@Signature注解的接口方法，验证请求签名的有效性
 *
 * @author pdai
 */
@Aspect
@Component
public class SignAspect {

    /**
     * 签名请求头名称
     */
    private static final String SIGN_HEADER = "X-SIGN";

    /**
     * 定义切点：拦截所有带有@Signature注解的方法
     */
    @Pointcut("execution(@tech.pdai.springboot.api.sign.config.sign.Signature * *(..))")
    private void verifySignPointCut() {
        // 切点定义，无需具体实现
    }

    /**
     * 在方法执行前进行签名验证
     */
    @Before("verifySignPointCut()")
    public void verify() {
        // 获取当前HTTP请求对象
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        // 从请求头中获取签名值
        String sign = request.getHeader(SIGN_HEADER);

        // 必须在请求头中包含签名信息
        if (CharSequenceUtil.isBlank(sign)) {
            throw new BusinessException("请求头中缺少签名信息: " + SIGN_HEADER);
        }

        // 验证签名有效性
        try {
            // 生成期望的签名值
            String generatedSign = generatedSignature(request);
            // 比较请求签名与生成的签名是否一致
            if (!sign.equals(generatedSign)) {
                throw new BusinessException("签名验证失败");
            }
        } catch (Throwable throwable) {
            throw new BusinessException("签名验证失败");
        }
    }

    /**
     * 根据请求参数生成签名字符串
     *
     * @param request HTTP请求对象
     * @return 生成的签名字符串
     * @throws IOException IO异常
     */
    private String generatedSignature(HttpServletRequest request) throws IOException {
        // 处理@RequestBody注解的请求体参数
        String bodyParam = null;
        if (request instanceof ContentCachingRequestWrapper) {
            bodyParam = new String(((ContentCachingRequestWrapper) request).getContentAsByteArray(), StandardCharsets.UTF_8);
        }

        // 处理@RequestParam注解的查询参数
        Map<String, String[]> requestParameterMap = request.getParameterMap();

        // 处理@PathVariable注解的路径变量
        String[] paths = null;
        ServletWebRequest webRequest = new ServletWebRequest(request, null);
        Map<String, String> uriTemplateVars = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!CollectionUtils.isEmpty(uriTemplateVars)) {
            paths = uriTemplateVars.values().toArray(new String[0]);
        }

        // 调用签名工具类生成签名
        return SignUtil.sign(bodyParam, requestParameterMap, paths);
    }

}