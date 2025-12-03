package tech.pdai.springboot.schedule.timer.timertest;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * 为什么几乎很少使用Timer这种方式？Timer底层是使用一个单线来实现多个Timer任务处理的，所有任务都是由同一个线程来调度，所有任务都是串行执行，意味着同一时间只能有一个任务得到执行，而前一个任务的延迟或者异常会影响到之后的任务。如果有一个定时任务在运行时，产生未处理的异常，那么当前这个线程就会停止，那么所有的定时任务都会停止，受到影响。
 * ------
 * 著作权归@pdai所有
 * 原文链接：https://pdai.tech/md/spring/springboot/springboot-x-task-timer.html
 *
 *
 *
 * @author pdai
 */
@Slf4j
public class TimerTester {

    /**
     * 定时器测试函数
     * 该函数演示了Java定时器的基本使用方法，包括启动定时任务、等待执行和取消定时器
     *
     * 核心工作原理（简单理解）
     * Timer的工作机制很简单，核心是 “单线程 + 任务队列”：
     * Timer内部有一个单线程（TimerThread），专门负责执行所有TimerTask；
     * 你调用schedule()方法时，会把任务和执行时间存入一个优先级队列（按执行时间排序，先执行的任务排在前面）；
     * TimerThread会不断从队列中取出 “到达执行时间” 的任务，执行其run()方法；
     * 任务执行完后，如果是重复任务，会重新计算下一次执行时间，再放回队列。
     * 👉 关键注意：因为是单线程，如果某个任务执行时间过长，会阻塞后续任务（比如任务 A 执行了 5 秒，而任务 B 本该在 3 秒时执行，就会被延迟到 5 秒后）。
     *
     * 五、优缺点（新手必知，避免踩坑）
     * 优点：
     * 简单易用：JDK 内置，无需依赖第三方库（如 Quartz），几行代码就能实现定时任务；
     * 轻量级：占用资源少，适合简单的定时场景。
     *
     * 缺点（重点关注，避免生产环境踩坑）：
     * 单线程执行：所有任务排队，一个任务阻塞会影响其他任务（比如任务 A 执行 10 秒，任务 B 本该在 2 秒时执行，会被推迟到 10 秒后）；
     * 不捕获异常：如果某个任务的run()方法抛出未捕获异常，TimerThread会直接终止，导致所有后续任务都无法执行；
     * 时间精度低：依赖系统时间和线程调度，适合秒级以上的任务，不适合毫秒级高精度场景；
     * 不支持复杂场景：无法实现 cron 表达式（如 “每月 1 号凌晨执行”）、任务依赖、动态调整等复杂需求。
     *
     * 六、适用场景 & 替代方案
     * 适用场景：
     * 简单的定时任务（如延迟执行、固定间隔重复执行）；
     * 非核心业务（如日志清理、数据备份提醒）；
     * 对时间精度和可靠性要求不高的场景。
     * 替代方案（生产环境推荐）：
     * 复杂定时任务：使用 Quartz（功能强大，支持 cron 表达式、集群部署）；
     * Spring 项目：使用 @Scheduled 注解（基于 Spring 封装，支持 cron 表达式，简单易用）；
     * 高并发场景：使用 ScheduledExecutorService（JDK 1.5+ 提供，支持多线程，比 Timer 更可靠）。
     * 总结
     * Timer是 JDK 内置的简单定时任务工具，核心是Timer（调度）+TimerTask（任务）；
     * 优点是轻量、易用，缺点是单线程、不捕获异常、精度低，适合简单场景；
     * 生产环境中，复杂任务优先用 @Scheduled（Spring 项目）或 Quartz，高并发场景用 ScheduledExecutorService；
     * 新手使用时，要注意避免任务阻塞和未捕获异常，否则会导致整个Timer失效。
     *
     *
     *
     *
     *
     * @throws InterruptedException 当线程睡眠被中断时抛出此异常
     */
    @SneakyThrows
    public static void timer() {
        // 启动定时器并设置延迟1秒后执行的任务
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                log.info("timer-task @{}", LocalDateTime.now());
            }
        }, 1000);

        // 等待定时任务执行完毕（通过睡眠3秒来模拟业务处理过程）
        Thread.sleep(3000);

        // 取消定时器，释放相关资源
        timer.cancel();
    }


    /**
     * 定时器周期性任务演示函数
     * 该函数创建一个定时器，每隔1秒执行一次任务，持续10秒后停止
     * 任务执行时间故意设置为超过间隔时间，用于演示任务重叠执行的情况
     *
     * @throws InterruptedException 当线程睡眠被中断时抛出
     */
    @SneakyThrows
    public static void timerPeriod() {
        // 启动定时器，创建周期性执行的任务
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @SneakyThrows
            public void run() {
                log.info("timer-period-task @{}", LocalDateTime.now());
                Thread.sleep(100); // 执行时间大于Period间隔时间
            }
        }, 500, 1000);

        // 等待定时器任务执行完成，模拟业务处理过程
        Thread.sleep(10000);

        // 停止并清理定时器资源
        timer.cancel();
    }


    @SneakyThrows
    public static void timerFixedRate() {
        // start timer
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int count = 0;

            @SneakyThrows
            public void run() {
                if (count++==2) {
                    Thread.sleep(5000); // 某一次执行时间超过了period(执行周期）
                }
                log.info("timer-fixedRate-task @{}", LocalDateTime.now());

            }
        }, 500, 1000);

        // waiting to process(sleep to mock)
        Thread.sleep(10000);

        // stop timer
        timer.cancel();
    }


    public static void main(String[] args) {
        timer();
//        timerPeriod();
//        timerFixedRate();
    }
}
