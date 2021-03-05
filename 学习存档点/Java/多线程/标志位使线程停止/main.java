class teststop implements Runnable {
    private boolean flag = true;

    @Override
    public void run() {
        int i = 0;
        while (flag) {
            System.out.println(":" + i++);
        }
    }

    public void stop() {
        this.flag = false;
    }

    public static void main(String[] args) {
        teststop teststop = new teststop();
        new Thread(teststop).start();

        for (int i = 0; i < 100; i++) {
            System.out.println("main" + i);
            if (i == 90) {
                teststop.stop();
//                System.out.println("stop");

            }
        }
    }

}



