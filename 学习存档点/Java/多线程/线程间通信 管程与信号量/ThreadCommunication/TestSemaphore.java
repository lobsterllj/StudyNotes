package ThreadCommunication;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class TestSemaphore
{
    /**
     * 测试主程序
     */
    public static void main(String[] args) {
        TestSemaphore t = new TestSemaphore();
        t.runTest();
    }

    //TestSemaphore
    /**信号量*/
    private Semaphore mSem = new Semaphore(0);
    /**产生的产品队列*/
    private Queue<String> mQueue = new LinkedList<String>();

    /**
     * 进行测试
     */
    public void runTest() {
        Runnable runProvider = new Runnable() {
            @Override
            public void run() {
                provider();
            }
        };

        Runnable runCustomer = new Runnable() {
            @Override
            public void run() {
                customer();
            }
        };

        new Thread(runCustomer).start();
        new Thread(runProvider).start();

        while(true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 生产者线程
     */
    public void provider() {
        for(int i = 0; i < 10; i++) {

            //生成一个产品，放到队列里
            String p = String.format("product_%d", i);
            mQueue.offer(p);
            System.out.println(String.format("Send sig >>>>>>>>>>>>> %s", p));

            //发出信号量
            mSem.release();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 消费者线程
     */
    public void customer() {
        while(true) {
            //等待信号量
            try {
                mSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //从队列里取出一个产品
            String s = mQueue.poll();
            System.out.println(String.format("Get sig : %s", s));

            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
