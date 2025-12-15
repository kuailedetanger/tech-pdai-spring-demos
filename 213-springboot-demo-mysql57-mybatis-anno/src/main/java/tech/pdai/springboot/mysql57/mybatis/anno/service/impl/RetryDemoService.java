package tech.pdai.springboot.mysql57.mybatis.anno.service.impl;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service

public class RetryDemoService {


    // 标记该方法需要重试
    @Retryable(
            value = {RuntimeException.class}, // 触发重试的异常类型（可写多个）：只有抛出这些异常或其子类时才会进行重试
            maxAttempts = 3, // 最大重试次数（含首次执行，即首次失败后重试2次）：总共尝试3次，第一次失败后最多再重试2次
            backoff = @Backoff(delay = 1000, multiplier = 2) // 回退策略：首次重试等待1秒，下次重试等待时间乘以2（1s→2s→4s）
    )
    public String callThirdPartyApi() {
        // 模拟调用第三方接口（可能失败）
        System.out.println("调用第三方接口...");
        
        // 使用静态变量模拟第二次调用成功的情况
        if (RetryCounter.getCounter() < 2) {
            RetryCounter.increment();
            // 模拟临时故障
            throw new RuntimeException("网络抖动，接口调用失败");
        } else {
            System.out.println("成功调用第三方接口");
            return "第三次，调用成功";
        }
    }

    /**
     * 简单的计数器类，用于模拟重试场景
     */
    private static class RetryCounter {
        private static int counter = 0;
        
        public static int getCounter() {
            return counter;
        }
        
        public static void increment() {
            counter++;
        }
        
        public static void reset() {
            counter = 0;
        }
    }

    // 重试全部失败后执行的兜底方法（参数需与重试方法一致，最后加一个异常参数）
    @Recover
    public String recover(RuntimeException e) {
        System.out.println("重试全部失败，执行兜底逻辑：" + e.getMessage());
        return "默认兜底结果"; // 返回默认值/触发告警等
    }
}
