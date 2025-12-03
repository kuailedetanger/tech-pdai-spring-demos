package tech.pdai.springboot.actuator;

import java.time.LocalDateTime;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pdai
 */
@RestController("custom")
@WebEndpoint(id = "date") // 创建一个ID为"date"的Web端点，可通过HTTP访问自定义监控端点
public class CustomEndpointController {

    /**
     * 读取操作 - 返回当前日期时间
     * 该端点暴露一个GET请求接口，路径为 /actuator/date
     * 
     * @return ResponseEntity<String> 包含当前日期时间的响应体
     */
    @ReadOperation // 标识这是一个读取操作，对应HTTP GET方法
    public ResponseEntity<String> currentDate() {
        return ResponseEntity.ok(LocalDateTime.now().toString());
    }
}
