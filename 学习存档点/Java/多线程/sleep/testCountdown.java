import java.text.SimpleDateFormat;
import java.util.Date;

public class testCountdown {
    //模拟倒计时
    public static void tenDown() throws InterruptedException {
        int num = 10;
        while (true) {
            Thread.sleep(1000);
            System.out.println(num--);
            if (num <= 0)
                break;
        }
    }


    public static void main(String[] args) {
        Date startTime = new Date(System.currentTimeMillis());
        while (true) {
            try {
                Thread.sleep(1000);
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(startTime));
                startTime = new Date(System.currentTimeMillis());
//                tenDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
