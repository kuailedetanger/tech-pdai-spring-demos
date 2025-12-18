package thread.demo;

/**
 * 第一种继承Thread类 重写run方法
 */
public class CreateThread extends Thread{

    @Override
    public void run() {
        for (int i = 0; i< 10; i++) {
            System.out.println(Thread.currentThread().getName() +" "+ "  i:" + i);
        }
    }

    public static void main(String[] args) {
        System.out.println("-----多线程创建开始-----");
        // 1.创建一个线程
        CreateThread createThread = new CreateThread();
        // 2.开始执行线程 注意 开启线程不是调用run方法，而是start方法
        System.out.println("-----多线程创建启动-----");
        createThread.start();
        System.out.println("-----多线程创建结束-----");
        
        // 在主线程中也打印信息，并显示线程名称
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + ": " + i);
        }

    }
}
