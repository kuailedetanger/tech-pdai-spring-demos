package tech.pdai.springboot.schedule.executorservice;

import cn.hutool.core.date.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 文档: https://pdai.tech/md/spring/springboot/springboot-x-task-executor-timer.html
 *
 * ScheduledExecutorService是基于线程池的，可以开启多个线程进行执行多个任务，每个任务开启一个线程； 这样任务的延迟和未处理异常就不会影响其它任务的执行了。
 * ScheduledExecutorService,JDK 内置的增强版定时工具，解决了 Timer 的核心缺点，支持多线程.
 *
 *
 * @author pdai
 */
@Slf4j
public class ScheduleExecutorServiceDemo {

    /**
     * 调度任务执行函数
     * 该函数创建一个调度线程池，安排一个延迟任务在指定时间后执行，
     * 并在主线程中等待一段时间后关闭线程池。
     *
     *  ScheduledExecutorService 有两个核心定时方法，用途完全不同
     *   schedule(...)  单次延迟执行， 延迟 initialDelay 后，只执行 1 次 任务，之后不再执行
     *
     *   scheduleAtFixedRate(...) / scheduleWithFixedDelay(...)  重复执行，  延迟 initialDelay 后，按 period 间隔 重复执行 任务
     *
     * 无参数
     * 无返回值
     */
    @SneakyThrows
    public static void schedule() {
        // 创建一个单线程的调度执行器服务
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        // 安排一个延迟任务，在1000毫秒后执行,执行一次
        executor.schedule(
                new Runnable() {
                    @Override
                    @SneakyThrows
                    public void run() {
                        log.info("run schedule @ {}",DateUtil.format(LocalDateTime.now(), "yyyy年MM月dd日 HH时mm分ss秒 SSS"));
                    }
                },
                1000,
                TimeUnit.MILLISECONDS);

        // 等待任务处理完成（通过睡眠模拟等待过程）
        Thread.sleep(10000);

        // 关闭执行器服务，释放资源
        /*
        关于 executor.shutdown()：你之前注释掉了这个方法，必须打开！否则线程池会一直运行，程序无法正常退出（即使 main 线程结束，线程池的后台线程还在存活）。
         */
        executor.shutdown();
    }


    /**
     * 每秒执行一次，延迟0.5秒执行。
     * 该方法演示了定时任务的调度执行，创建一个单线程的调度执行器，
     * 并按照固定频率执行任务。任务在执行2次后会模拟长时间运行的情况，
     * 以测试当任务执行时间超过执行周期时的行为。
     *
     * scheduleAtFixedRate 的核心设计是：下一次任务的 “计划执行时间” = 上一次任务的 “实际开始时间” + 周期。
     * 不管上一次任务执行了多久（哪怕超时），下一次的 “计划时间” 早已固定；
     * 如果上一次任务按时完成（执行时间 <周期），下一次就按 “计划时间” 执行；
     * 如果上一次任务超时（执行时间 > 周期），下一次会在 “上一次任务结束后立即执行”（相当于 “补回” 被占用的时间，尽量不减少总执行次数）。
     *
     * “不中断、不跳过、尽量补”
     * 不中断：正在执行的任务，哪怕超时，也会等它执行完；
     * 不跳过：不会为了凑次数跳过某个计划中的任务；
     * 尽量补：如果前一个任务超时导致下一个任务的 “计划时间” 已过，会在当前任务结束后立即执行下一个，尽量补回被耽误的次数。
     *
     *
     *
     *
     * “总执行时间” 的控制（为什么 10 秒后停止） ?
     * Thread.sleep(10000); // 让main线程睡眠10000毫秒（10秒）
     * 作用：main线程是程序的主线程，睡眠 10 秒期间，定时任务会正常执行；
     * 10 秒后：main线程醒来，执行下一行 executor.shutdown()，关闭定时任务执行器，任务不再继续执行；
     *
     *
     * 无参数
     * 无返回值
     */
@SneakyThrows
public static void scheduleAtFixedRate() {
    AtomicInteger count = new AtomicInteger(0);
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // 调度固定频率执行的任务，初始延迟500毫秒，之后每隔1000毫秒执行一次
    executor.scheduleAtFixedRate(
            new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    int currentCount = count.incrementAndGet();
                    // 当任务执行到第3次时，模拟执行时间超过执行周期的情况
                    if (currentCount==3) {
                        log.info("run scheduleAtFixedRate @ {} - 模拟长时间运行任务...",DateUtil.format(LocalDateTime.now(), "yyyy年MM月dd日 HH时mm分ss秒 SSS"));
                        Thread.sleep(5000); // 执行时间超过执行周期
                        log.info("长时间运行任务结束 @ {}",DateUtil.format(LocalDateTime.now(), "yyyy年MM月dd日 HH时mm分ss秒 SSS"));
                    }
                    log.info("run scheduleAtFixedRate 第{}次执行 @ {}", currentCount,DateUtil.format(LocalDateTime.now(), "yyyy年MM月dd日 HH时mm分ss秒 SSS"));
                }
            }, // 要执行的任务（无关时间/频率）
            500, // 参数2：初始延迟时间（控制“什么时候开始第一次执行”）
            1000,  // 参数3：执行周期（控制“多久执行一次”，即频率）
            TimeUnit.MILLISECONDS); // 参数4：时间单位（说明前两个参数的单位是“毫秒”）

    //让定时任务跑 10 秒就停止
    Thread.sleep(10000);

    // 停止执行器
    executor.shutdown();
}


    /**
     * 每秒执行一次，延迟0.5秒执行。
     *
     * scheduleAtFixedDelay：每次执行完当前任务后，然后间隔一个period的时间再执行下一个任务； 当某个任务执行周期大于时间间隔时，依然按照间隔时间执行下个任务，即它保证了任务之间执行的间隔。
     * ------
     * 著作权归@pdai所有
     * 原文链接：https://pdai.tech/md/spring/springboot/springboot-x-task-executor-timer.html
     *
     */
    @SneakyThrows
    public static void scheduleWithFixedDelay() {
        AtomicInteger count = new AtomicInteger(0);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    @SneakyThrows
                    public void run() {
                        if (count.getAndIncrement()==2) {
                            Thread.sleep(5000); // 执行时间超过执行周期
                        }
                        log.info("run scheduleWithFixedDelay @ {}", DateUtil.format(LocalDateTime.now(), "yyyy年MM月dd日 HH时mm分ss秒 SSS"));
                    }
                },
                500,
                1000, // 上次执行完成后，延迟多久执行
                TimeUnit.MILLISECONDS);

        // waiting to process(sleep to mock)
        Thread.sleep(10000);

        // stop
        executor.shutdown();
    }

    public static void main(String[] args) {

//        schedule();

//        scheduleAtFixedRate();
//
        scheduleWithFixedDelay();
    }
}
