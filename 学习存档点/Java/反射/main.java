import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class main {
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        main main = new main();
        prtMatrix prtMatrix = new prtMatrix();
        prtInts prtInts = new prtInts();
        formMatrix formMatrix = new formMatrix();


        List<Integer> list = new ArrayList<>();
        list.add(12);
        //这里直接添加会报错
//        list.add("a");
        Class<? extends List> clazz = list.getClass();
        Method add = clazz.getDeclaredMethod("add", Object.class);
//        Method add = clazz.getDeclaredMethod("add", Integer.class);

        //但是通过反射添加，是可以的
        add.invoke(list, "14s");
//        add.invoke(list, 1);

        System.out.println(list);

    }


}