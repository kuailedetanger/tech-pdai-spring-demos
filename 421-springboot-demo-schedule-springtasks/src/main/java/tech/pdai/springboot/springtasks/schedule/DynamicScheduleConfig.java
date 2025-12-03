package tech.pdai.springboot.springtasks.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;



@Configuration
@Slf4j
public class DynamicScheduleConfig  implements SchedulingConfigurer {
    // 从配置文件读取线程池大小，默认值设为5（防止配置缺失报错）
    @Value("${spring.schedule.thread-pool-size:2}")
    private int threadPoolSize;


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        // 2. 用原子计数器生成唯一线程编号（确保每个线程名称不同）
        AtomicInteger threadNumber = new AtomicInteger(1);
        // 动态创建线程池（线程数来自配置文件）
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threadPoolSize,
                // 自定义线程名称（方便观测，比如看到线程名就知道是定时任务线程）
//                runnable -> new Thread(runnable, "定时任务线程-" + threadPoolSize)


                runnable -> {
                    // 线程名格式：定时任务线程-池大小-线程编号（如：定时任务线程-2-1、定时任务线程-2-2）
                    String threadName = "定时任务线程-" + threadPoolSize + "-" + threadNumber.getAndIncrement();
                    // 3. 输出线程创建日志（每个线程启动时打印一次）
                    log.info("创建定时任务线程：{}", threadName);
                    return new Thread(runnable, threadName);
                }
        );




        // 给定时任务注册线程池
        taskRegistrar.setScheduler(executorService);
    }
}
