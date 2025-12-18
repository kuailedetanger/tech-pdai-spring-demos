package thread.demo.thread.ReentrantLock;

import java.util.concurrent.locks.ReentrantLock;

public class ThreadTrain1 implements Runnable {

    // 这是货票总票数,多个线程会同时共享资源
    private int trainCount = 100;

    ReentrantLock lock = new ReentrantLock(true); // true=公平锁，默认非公平

    @Override
    public void run() {
        while (trainCount > 0) {// 循环是指线程不停的去卖票
            try {
                // 等待100毫秒
                Thread.sleep(10);
            } catch (InterruptedException e) {

            }
            sale_sync();
        }
    }


    /**
     * 售票方法
     * 检查是否有余票，如果有则售出一张票
     * <p>
     * 注意：该方法存在线程安全问题
     * 1. trainCount-- 操作不是原子性的，包含了读取、计算、写入三个步骤
     * 2. 多个线程可能同时读取到相同的trainCount值，导致超卖问题
     * 3. 需要使用同步机制（如synchronized）来保证原子性和可见性
     */
    private void sale() {
        // 判断是否还有票可以售卖
        if (trainCount > 0) {
            try {
                // 模拟售票过程中的延迟
                Thread.sleep(10);
            } catch (Exception e) {
                // 处理睡眠中断异常
            }
            // 打印售票信息：当前线程名称和售出的票号
            System.out.println(Thread.currentThread().getName() + ",出售 第" + (100 - trainCount + 1) + "张票.");
            // 票数减一，此操作非线程安全
            trainCount--;
        }
    }

    private void sale_sync() {
        try {
            lock.lock();// 手动拿钥匙（加锁）
            sale();
        }finally{
            lock.unlock(); // 手动还钥匙（释放锁，避免死锁）
        }

    }


}
