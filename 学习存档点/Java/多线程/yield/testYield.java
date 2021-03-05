import java.util.TreeMap;

public class testYield {
    public static void main(String[] args) {
        myYield myYield = new myYield();
        new Thread(myYield, "a").start();
        new Thread(myYield, "b").start();


    }
}
//礼让让正在执行的线程暂停但不阻塞
//让程序从运行状态转变为就绪状态
//让cpu重新调度，礼让成不成功看cpu心情

class myYield implements Runnable {

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "线程开始");
        Thread.yield();
        System.out.println(Thread.currentThread().getName() + "线程停止");

    }
}
