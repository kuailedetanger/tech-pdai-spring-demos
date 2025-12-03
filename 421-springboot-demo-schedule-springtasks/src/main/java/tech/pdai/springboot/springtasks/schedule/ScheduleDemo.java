package tech.pdai.springboot.springtasks.schedule;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 *
 * @EnableScheduling注解
 * 添加@EnableScheduling注解会自动注入SchedulingConfiguration
 *
 *
 *
 *
 * @author pdai
 */
@Slf4j
@EnableScheduling
@Configuration
public class ScheduleDemo {

    /**
     * 每隔1分钟执行一次。
     */
//    @Scheduled(fixedRate = 1000 * 60 * 1)
    @Scheduled(fixedRate = 1000  * 1)
    public void runScheduleFixedRate() {
//        log.info("runScheduleFixedRate: current DateTime, {}", LocalDateTime.now());
        log.info("【固定频率定时任务】执行时间：{}", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 每隔1分钟执行一次, 测试异常。
     */
    @Scheduled(fixedRate = 1000 * 10 * 1)
    public void runScheduleFixedRateException() throws Exception {
        log.info("【异常测试定时任务】执行时间：{}", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 抛出带中文描述的异常，方便日志排查
        throw new Exception("【异常测试定时任务】执行失败：模拟业务异常");
    }
//
//    /**
//     * 每隔5s执行一次, 测试异常。
//     */
//    @Scheduled(cron = "*/5 * * * * ?")
//    public void runScheduleFixedRateException2() {
//        log.info("runScheduleFixedRateException2: current DateTime, {}", LocalDateTime.now());
//        int a = 1 / 0;
//    }
//
//    /**
//     * 每个整点小时执行一次。
//     */
//    @Scheduled(cron = "0 0 */1 * * ?")
//    public void runScheduleCron() {
//        log.info("runScheduleCron: current DateTime, {}", LocalDateTime.now());
//    }

}
