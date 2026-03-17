package tech.pdai.springboot.helloworld.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.pdai.springboot.helloworld.feign.WeatherFeignClient;
import tech.pdai.springboot.helloworld.model.WeatherResponse;

import javax.annotation.Resource;

/**
 * 天气服务层
 */
@Service
@Slf4j
public class WeatherService {

    @Resource
    private WeatherFeignClient weatherFeignClient; // 注入Feign接口

    @Value("${weather.amap.key}")
    private String amapKey; // 从配置文件读取高德密钥

    /**
     * 查询城市实时天气
     * @param cityCode 城市编码（如成都510100）
     * @return 天气响应
     */
    public WeatherResponse getCityWeather(String cityCode) {
        try {
            log.info("开始调用高德天气API，城市编码：{}", cityCode);
            // 调用Feign接口（像调用本地方法一样）
            WeatherResponse response = weatherFeignClient.getWeather(
                    amapKey,
                    cityCode,
                    "base" // 只查基础天气
            );
            log.info("调用高德天气API成功，响应：{}", response);
            return response;
        } catch (Exception e) {
            log.error("调用高德天气API失败，城市编码：{}，错误信息：{}", cityCode, e.getMessage(), e);
            throw new RuntimeException("查询天气失败：" + e.getMessage());
        }
    }
}