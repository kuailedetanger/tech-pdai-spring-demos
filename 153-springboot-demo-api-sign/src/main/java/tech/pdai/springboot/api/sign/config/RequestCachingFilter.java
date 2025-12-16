package tech.pdai.springboot.api.sign.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 请求缓存过滤器配置类
 * 用于包装请求对象，使其能够多次读取请求体内容
 */
@Slf4j
public class RequestCachingFilter extends OncePerRequestFilter {

    /**
     * 过滤器的核心处理方法，对请求进行包装以支持缓存
     * 存储一个"已过滤"的请求属性，如果该属性已经存在则不再重复过滤
     *
     * @param request     HTTP请求对象
     * @param response    HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     * @see #getAlreadyFilteredAttributeName
     * @see #shouldNotFilter
     * @see #doFilterInternal
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 判断是否为首次请求（非异步分发）
        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestWrapper = request;
        
        // 如果是首次请求且请求对象不是ContentCachingRequestWrapper类型，则进行包装
        if (isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestWrapper = new ContentCachingRequestWrapper(request);
        }
        
        try {
            // 继续执行过滤器链
            filterChain.doFilter(requestWrapper, response);
        } catch (Exception e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        }
    }
}
