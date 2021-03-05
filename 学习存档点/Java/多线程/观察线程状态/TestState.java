public class TestState {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("1122233");
        });


        Thread.State state = thread.getState();
        System.out.println(state);

        thread.start();//启动线程
        state = thread.getState();
        System.out.println(state);

        while (state != Thread.State.TERMINATED) {
            //只要线程不终止
            Thread.sleep(100);
            state = thread.getState();
            System.out.println(state);

        }
        
        //停止之后的线程不能再次启动
        thread.start();

    }

}
