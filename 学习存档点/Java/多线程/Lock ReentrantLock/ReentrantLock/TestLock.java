package ReentrantLock;

import java.util.concurrent.locks.ReentrantLock;

//测试Lock锁
public class TestLock {
    public static void main(String[] args) {
        TestLockinterface testLock = new TestLockinterface();
        new Thread(testLock).start();
        new Thread(testLock).start();
        new Thread(testLock).start();

    }

}

class TestLockinterface implements Runnable {
    int tickNums = 10;

    //定义lock锁
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void run() {
        while (true) {
            try {
                lock.lock();
                if (tickNums > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(tickNums--);
                } else {
                    break;
                }
            }finally {
                lock.unlock();
            }

        }
    }
}