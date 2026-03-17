package tech.pdai.springboot.helloworld.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.pdai.springboot.helloworld.model.WeatherResponse;
import tech.pdai.springboot.helloworld.service.WeatherService;

import javax.annotation.Resource;

/**
 * 天气接口控制器（测试用）
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Resource
    private WeatherService weatherService;

    /**
     * 查询指定城市的实时天气
     * @param cityCode 城市编码（如成都510100，北京110100）
     * @return 天气信息
     */
    @GetMapping("/{cityCode}")
    public WeatherResponse getWeather(@PathVariable String cityCode) {
        return weatherService.getCityWeather(cityCode);
    }
}