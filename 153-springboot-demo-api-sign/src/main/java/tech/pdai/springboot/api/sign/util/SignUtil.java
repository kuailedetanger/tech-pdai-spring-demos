package tech.pdai.springboot.api.sign.util;

import java.util.Arrays;
import java.util.Map;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import org.springframework.util.CollectionUtils;

/**
 * 签名工具类
 */
public class SignUtil {

    /**
     * 默认密钥
     */
    private static final String DEFAULT_SECRET = "1qaz@WSX#$%&";

    /**
     * 生成签名
     *
     * @param body   请求体内容
     * @param params 请求参数
     * @param paths  路径参数
     * @return 签名字符串
     */
    public static String sign(String body, Map<String, String[]> params, String[] paths) {
        StringBuilder sb = new StringBuilder();
        // 添加请求体内容
        if (CharSequenceUtil.isNotBlank(body)) {
            sb.append(body).append('#');
        }

        // 处理请求参数并按key排序
        if (!CollectionUtils.isEmpty(params)) {
            params.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(paramEntry -> {
                        // 参数值排序并用逗号连接
                        String paramValue = String.join(",", Arrays.stream(paramEntry.getValue()).sorted().toArray(String[]::new));
                        sb.append(paramEntry.getKey()).append("=").append(paramValue).append('#');
                    });
        }

        // 处理路径参数并排序
        if (ArrayUtil.isNotEmpty(paths)) {
            String pathValues = String.join(",", Arrays.stream(paths).sorted().toArray(String[]::new));
            sb.append(pathValues);
        }
        // 使用SHA256算法生成签名
        // 使用Hutool工具类提供的SHA256算法对拼接后的字符串进行加密
        // 将默认密钥和待签名字符串用"#"连接后进行SHA256哈希运算，生成最终的签名值
        return SecureUtil.sha256(String.join("#", DEFAULT_SECRET, sb.toString()));
    }

}
