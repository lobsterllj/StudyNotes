public class testJoin implements Runnable {


    @Override
    public void run() {
        for (int i = 0; i < 200; ++i) {
            System.out.println("线程vip来了" + i);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        testJoin testJoin = new testJoin();
        Thread thread = new Thread(testJoin);
        thread.start();

        for (int i = 0; i < 200; i++) {
            if (i == 100) {
                thread.join();
            }
            System.out.println("main" + i);

        }
    }
}
