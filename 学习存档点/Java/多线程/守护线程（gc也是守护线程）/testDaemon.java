public class testDaemon {

    public static void main(String[] args) {
        God god = new God();
        you you = new you();

        Thread thread = new Thread(god);
        thread.setDaemon(true);
        thread.start();

        new Thread(you).start();

    }

}

//守护
class God implements Runnable {
    @Override
    public void run() {
        while (true) {
            System.out.println("is blessing");
        }
    }

}

//你
class you implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 365; i++) {
            System.out.println("in live");
        }
        System.out.println("________gone_______");
    }
}