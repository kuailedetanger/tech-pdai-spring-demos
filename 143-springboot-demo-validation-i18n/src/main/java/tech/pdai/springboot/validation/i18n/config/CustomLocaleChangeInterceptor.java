package tech.pdai.springboot.validation.i18n.config;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * custom locale change interceptor.
 * 自定义的语言环境切换拦截器
 * 扩展了 Spring 默认的 LocaleChangeInterceptor，支持从请求头获取语言参数
 *
 * @author pdai
 */
@Slf4j
public class CustomLocaleChangeInterceptor extends LocaleChangeInterceptor {

    /**
     * try to get locale from header, if not exist then get it from request parameter.
     *  尝试从请求头中获取语言参数，如果不存在则从请求参数中获取
     *  重写了父类的 preHandle 方法，改变了语言参数的获取方式
     * @param request  request
     * @param response response
     * @param handler  handler  处理器对象
     * @return bool
     * @throws ServletException ServletException
     */
    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws ServletException {
        // 首先从请求头中获取语言参数（参数名由父类的 paramName 指定，默认是 "lang"）
        String newLocale = request.getHeader(getParamName());
        // 如果请求头中存在语言参数
        if (newLocale!=null) {
            // 检查当前请求方法是否在允许的方法列表中
            if (checkHttpMethods(request.getMethod())) {
                // 获取 Spring 的语言解析器
                LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
                if (localeResolver==null) {
                    // 如果没有找到语言解析器，抛出异常
                    throw new IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?");
                }
                try {
                    // 解析语言参数值并设置到语言解析器中
                    localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
                } catch (IllegalArgumentException ex) {
                    // 如果语言参数无效
                    if (isIgnoreInvalidLocale()) {
                        // 如果配置了忽略无效语言，则只记录调试日志
                        log.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
                    } else {
                        throw ex;
                    }
                }
            }
            // 处理完成，继续后续流程
            return true;
        } else {
            return super.preHandle(request, response, handler);
        }
    }

    /**
     检查当前请求方法是否在允许的方法列表中
     @param currentMethod 当前请求的 HTTP 方法（如 GET、POST 等）
     @return 如果允许则返回 true，否则返回 false
     */
    private boolean checkHttpMethods(String currentMethod) {
        // 获取配置的允许的 HTTP 方法列表
        String[] configuredMethods = getHttpMethods();
        // 如果没有配置允许的方法，则默认全部允许
        if (ObjectUtils.isEmpty(configuredMethods)) {
            return true;
        }
        // 检查当前方法是否在允许的列表中（忽略大小写）
        for (String configuredMethod : configuredMethods) {
            if (configuredMethod.equalsIgnoreCase(currentMethod)) {
                return true;
            }
        }
        // 不在允许的方法列表中
        return false;
    }
}

