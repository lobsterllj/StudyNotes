public class TestPriority {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + "---" + Thread.currentThread().getPriority());
        mpPriority mpPriority = new mpPriority();
        Thread t1 = new Thread(mpPriority);
        Thread t2 = new Thread(mpPriority);
        Thread t3 = new Thread(mpPriority);
        Thread t4 = new Thread(mpPriority);
        //先设置优先级在启动
        //优先级高的线程只是 较大概率优先执行
        t1.start();
        t2.setPriority(1);
        t2.start();
        t3.setPriority(4);
        t3.start();
        t4.setPriority(Thread.MAX_PRIORITY);
        t4.start();
    }
}

class mpPriority implements Runnable {

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "---" + Thread.currentThread().getPriority());
    }
}