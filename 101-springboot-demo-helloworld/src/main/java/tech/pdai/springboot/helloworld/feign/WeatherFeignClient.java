package tech.pdai.springboot.helloworld.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tech.pdai.springboot.helloworld.model.WeatherResponse;

/**
 * 高德天气API的Feign客户端
 * @FeignClient说明：
 *   name：客户端名称（唯一，随便取）
 *   url：接口基础地址（从配置文件读取）
 */
@FeignClient(name = "amap-weather-client", url = "${weather.amap.url}")
public interface WeatherFeignClient {

    /**
     * 调用高德天气查询接口
     * @param key 高德API密钥
     * @param city 城市编码（如成都510100）
     * @param extensions 返回类型（base=基础天气，all=全部）
     * @return 天气响应结果
     */
    @GetMapping("/v3/weather/weatherInfo") // 接口路径（拼接在baseUrl后）
    WeatherResponse getWeather(
            @RequestParam("key") String key,       // URL参数：API密钥
            @RequestParam("city") String city,     // URL参数：城市编码
            @RequestParam("extensions") String extensions // URL参数：返回类型
    );
}