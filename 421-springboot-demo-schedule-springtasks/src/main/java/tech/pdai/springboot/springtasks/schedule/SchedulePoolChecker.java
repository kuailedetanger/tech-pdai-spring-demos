package tech.pdai.springboot.springtasks.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * 定时任务线程池检查器
 * 
 * 用于检查Spring Boot中定时任务线程池的配置信息，
 * 包括核心线程数、活跃线程数等关键参数。
 */
//@Component
@Slf4j
public class SchedulePoolChecker {

    // 注入名称为 scheduleExecutorService 的线程池 Bean
    @Autowired
    private ScheduledExecutorService scheduleExecutorService;

    // 项目启动后自动执行
    @PostConstruct
    public void checkSchedulePool() {
        try {
            if (scheduleExecutorService instanceof ScheduledThreadPoolExecutor) {
                ScheduledThreadPoolExecutor pool = (ScheduledThreadPoolExecutor) scheduleExecutorService;
                log.info("======================================");
                log.info("===== 线程池检查器 - 配置信息 =====");
                log.info("核心线程数：{}", pool.getCorePoolSize());
                log.info("当前活跃线程数：{}", pool.getActiveCount());
                log.info("队列任务数：{}", pool.getQueue().size());
                log.info("======================================");
            }
        } catch (Exception e) {
            log.error("获取定时任务线程池配置失败", e);
        }
    }
}
