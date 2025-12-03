package tech.pdai.springboot.springtasks;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *Timer和ScheduledExecutorService是JDK内置的定时任务方案
 * 以及Netty内部基于时间轮实现的HashedWheelTimer；
 * 而主流的SpringBoot集成方案有两种，一种是Spring Sechedule,
 * 另一种是Spring集成Quartz；
 *
 *本文主要介绍Spring Schedule实现方式。
 *
 * ------
 * 著作权归@pdai所有
 * 原文链接：https://pdai.tech/md/spring/springboot/springboot-x-task-spring-task-timer.html
 *
 * @author pdai
 */
@SpringBootApplication
@EnableAdminServer
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
