package thread.demo.thread.ReentrantReadWriteLock;


public class ThreadDemo2 {

    /**
     * 结论发现，多个线程共享同一个全局成员变量时，做写的操作可能会发生数据冲突问题。
     * @param args
     */
    public static void main(String[] args) {

        ThreadTrain1 threadTrain = new ThreadTrain1(); // 定义 一个实例
        Thread thread1 = new Thread(threadTrain, "一号窗口");
        Thread thread2 = new Thread(threadTrain, "二号窗口");
        thread1.start();
        thread2.start();
    }
}
