package thread.demo;

/**
 * 创建多线程例子-Thread类 重写run方法
 * <p>
 * 使用继承Thread类还是使用实现Runnable接口好？
 * <p>
 * 用实现 Runnable（绝大多数场景）
 * ✅ 想让多个线程干同一件事（比如抢票、生产零件）；✅ 你的任务类需要继承其他类（比如继承 BaseService）；✅ 想让代码更灵活、易复用。
 * 用继承 Thread（极少数场景）
 * ❌ 只是简单写个 demo，懒得拆 “工人” 和 “任务”；❌ 必须重写 Thread 的其他方法（比如 interrupt），且确定这辈子不用继承其他类。
 * 五、额外补充（进阶但好懂）
 * 现在实际开发中，连 Runnable 都用得少了，更多用「线程池 + Callable」（带返回值的任务），但核心逻辑还是 “任务和执行体分离”—— 就像工厂不会雇专属工人，而是建 “临时工池”，有任务就派一个临时工去干，效率更高。
 */
public class CreateRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + "  i:" + i);
        }

    }

    public static void main(String[] args) {
        System.out.println("-----多线程创建开始-----");
        // 1.创建一个线程
        CreateRunnable createThread = new CreateRunnable();
        // 2.开始执行线程 注意 开启线程不是调用run方法，而是start方法
        System.out.println(Thread.currentThread().getName() + "-----多线程创建启动-----");
        Thread thread = new Thread(createThread);
        thread.start();
        System.out.println(Thread.currentThread().getName() + "-----多线程创建结束-----");
    }
}
