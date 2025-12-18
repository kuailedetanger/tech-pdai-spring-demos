package thread.demo.communication;

public class IntThrad extends Thread {

    private Res res;

    public IntThrad(Res res) {
        this.res = res;
    }

    @Override
    public void run() {

        setUserInfo_sync2();
    }

    /**
     * 运行结果（会看到大量错误数据）?
     * <p>
     * 线程只加一次锁,setUserInfo 方法， 两个线程一直抢，永远不停
     */
    private void setUserInfo_sync() {
        synchronized (res) {
            setUserInfo();
        }
    }

    /**
     * 设置用户信息的方法
     * <p>
     * 多线程安全性分析：
     * ❌ 线程不安全
     * 1. 此方法直接修改共享对象res的成员变量(userName和userSex)
     * 2. 这两个赋值操作不是原子性的，可能存在中间状态
     * 3. 比如刚设置了userName为"小蓝"，还没来得及设置userSex，另一个线程就可能读取到不一致的数据
     * 4. 虽然这里是单个线程在执行，但如果多个IntThrad实例操作同一个Res对象，就会出现线程安全问题
     */
    private void setUserInfo() {
        int count = 0;
        while (true) {
            if (count == 0) {
                res.userName = "小蓝";
                res.userSex = "男";
            } else {
                res.userName = "小紅";
                res.userSex = "女";
            }
            count = (count + 1) % 2;
        }
    }


    private void setUserInfo_sync2() {
        int count = 0;
        while (true) {
            while (!res.flag){
                synchronized (res) {
                    if (count == 0) {
                        res.userName = "小蓝";
                        res.userSex = "男";
                    } else {
                        res.userName = "小紅";
                        res.userSex = "女";
                    }
                    count = (count + 1) % 2;
                }
            }

        }
    }
}
