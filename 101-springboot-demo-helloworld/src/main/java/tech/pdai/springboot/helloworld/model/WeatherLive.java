package tech.pdai.springboot.helloworld.model;

import lombok.Data;

/**
 * 实时天气信息
 */
@Data
public class WeatherLive {
    private String province; // 省份
    private String city;     // 城市
    private String adcode;   // 城市编码
    private String weather;  // 天气（如：晴、阴）
    private String temperature; // 温度（℃）
    private String winddirection; // 风向（如：东北风）
    private String windpower; // 风力（如：3级）
    private String humidity; // 湿度（%）
    private String reporttime; // 更新时间
}