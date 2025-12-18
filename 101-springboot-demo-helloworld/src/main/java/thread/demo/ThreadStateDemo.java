package thread.demo;

public class ThreadStateDemo {

    public static void main(String[] args) throws InterruptedException {
        // ====================== 1. 新建状态（New） ======================
        // 线程对象刚创建，还未调用start()，此时线程处于“新建状态”
        // 就像工厂招聘了工人，但还没让他上岗
        Thread worker = new Thread(() -> {
            try {
                // ====================== 3. 运行状态（Running） ======================
                // 线程获取CPU执行权，开始执行run()方法内的逻辑，进入“运行状态”
                System.out.println("【运行状态】工人开始干活");

                // ====================== 4. 阻塞状态（Blocked） ======================
                // 调用sleep()：线程强制休眠，放弃CPU执行权，进入“阻塞状态”
                // 就像工人拿到工具后，临时休息1秒，期间不干活、不释放锁
                Thread.sleep(1000);
                System.out.println("【阻塞状态→就绪状态】工人休息结束，等待分配CPU");

                // 休眠结束后，线程从“阻塞状态”回到“就绪状态”，重新等CPU调度
                // 再次获取CPU后，回到“运行状态”
                System.out.println("【运行状态】工人继续干活");

            } catch (InterruptedException e) {
                // 若线程休眠时被interrupt()打断，会抛出异常，提前结束阻塞
                System.out.println("【阻塞状态】工人被叫醒，提前结束休息");
                e.printStackTrace();
            }
            // ====================== 5. 死亡状态（Dead） ======================
            // run()方法执行完毕，线程生命周期结束，进入“死亡状态”
            // 就像工人干完活，下班走人，无法再次启动
            System.out.println("【死亡状态】工人干完活，线程结束");
        }, "生产工人");

        // 打印新建状态：线程已创建，但未启动
        System.out.println("1. 线程创建后未start() → 状态：" + getThreadState(worker.getState()));

        // ====================== 2. 就绪状态（Runnable） ======================
        // 调用start()：线程进入“就绪状态”（也叫可运行状态）
        // 就像工人到工位待命，等老板（CPU）分配任务，此时还没真正干活
        worker.start();
        // 打印就绪状态（注意：start()后线程可能快速进入运行，这里加短暂休眠确保能抓到就绪状态）
        Thread.sleep(10);
        System.out.println("2. 调用start()后 → 状态：" + getThreadState(worker.getState()));

        // 等待worker线程执行完毕，观察状态变化
        worker.join();
        // 打印死亡状态：线程执行完毕
        System.out.println("3. 线程执行完毕后 → 状态：" + getThreadState(worker.getState()));

        // 测试：死亡状态的线程无法再次start()，会抛异常
        try {
            worker.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("⚠️ 死亡状态的线程无法再次start()：" + e.getMessage());
        }
    }

    private static String getThreadState(Thread.State state) {
        // 替换Java 14+的switch表达式为Java 8支持的传统switch
        switch (state) {
            case NEW:
                return "新建状态（New）：线程已创建，未调用start()";
            case RUNNABLE:
                return "就绪状态（Runnable）：调用start()，等待CPU调度（包含运行中）";
            case BLOCKED:
            case WAITING:
            case TIMED_WAITING:
                return "阻塞状态（Blocked）：sleep/join/等待锁等，暂时无法执行";
            case TERMINATED:
                return "死亡状态（Terminated）：run()执行完毕，线程结束";
            default:
                return "未知状态";
        }
    }
}
