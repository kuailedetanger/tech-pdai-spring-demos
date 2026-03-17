package tech.pdai.springboot.helloworld.model;

import lombok.Data;
import java.util.List;

/**
 * 天气接口根响应
 */
@Data
public class WeatherResponse {
    private String status; // 返回状态（1=成功，0=失败）
    private String info;   // 返回信息（失败时提示）
    private String infocode; // 信息码
    private List<WeatherLive> lives; // 实时天气列表
}