package tech.pdai.springboot.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * @author pdai
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    /**
     * 健康检查方法
     * 
     * @return Health 健康状态对象，包含详细信息
     */
    @Override
    public Health health() {
        // 执行健康检查
        int errorCode = check();
        // 如果返回错误码不为0，则认为服务不健康
        if (errorCode != 0) {
            // 返回DOWN状态，并附带错误码详情
            return Health.down().withDetail("Error Code", errorCode).build();
        }
        // 否则返回UP状态，表示服务健康
        return Health.up().build();
    }

    /**
     * 执行具体的健康检查逻辑
     * 
     * @return int 错误码，0表示正常，非0表示异常
     */
    private int check() {
        // 这里可以执行一些特定的健康检查操作
        // 比如：数据库连接检查、外部服务可用性检查等
        return 0;
    }

}
