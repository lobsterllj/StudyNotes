package syn;

public class UnsafeBuyTicket {

    public static void main(String[] args) {
        BuyTicket station = new BuyTicket();
        new Thread(station, "a").start();
        new Thread(station, "b").start();
        new Thread(station, "c").start();
    }
}

class BuyTicket implements Runnable {

    private int ticketNums = 10;
    private boolean flag = true;

    @Override
    public void run() {
        //买票
        while (flag) {
            try {
                buy();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void buy() throws InterruptedException {
        if (ticketNums <= 0) {
            return;
        }
        Thread.sleep(100);
        synchronized (this) {
            if (ticketNums == 2) {
                flag = false;
            }
            System.out.println(Thread.currentThread().getName() + "拿到" + ticketNums--);

        }
    }
}