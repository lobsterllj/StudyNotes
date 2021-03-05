import java.text.SimpleDateFormat;
import java.util.Date;

class IllegalArgumentTest {
    public static void main(String[] args) {
        /** string类型的时间戳**/
        String timeStamp = "1604043636872";
        /** 获取当前时间的时间戳**/
        long longTimeStamp = System.currentTimeMillis();
        /** 格式化转换成我们想要的格式**/
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /** 格式化long类型的时间戳**/
        String longsd = sdf.format(longTimeStamp);
        System.out.println(longsd);
        /** 格式化string类型的时间戳**/
        String sd = sdf.format(new Date(timeStamp));
        System.out.println(sd);
    }
}
