import java.util.ArrayList;
import java.util.List;

public class main {
    public static void main(String[] args) {
        main main = new main();
        prtMatrix prtMatrix = new prtMatrix();
        prtInts prtInts = new prtInts();

        main.testStr();
        main.testInt();

    }


    public void testStr() {
        List<List<List<String>>> t3 = new ArrayList<>();
        List<List<String>> t2 = new ArrayList<>();
        List<String> t1 = new ArrayList<>();
        t1.add("0");
        t2.add(t1);
        t3.add(t2);

        System.out.println("t1:" + t1);
        System.out.println("t2:" + t2);
        System.out.println("t3:" + t3);
        System.out.println("-----------");


//        List<String> ct1 = new ArrayList<>();
//        for (String s : t1)
//            ct1.add(s);
        List<String> ct1 = new ArrayList<>(t1);
        List<String> cct1 = t1;
        List<List<String>> ct2 = new ArrayList<>(t2);
        List<List<List<String>>> ct3 = new ArrayList<>(t3);
        System.out.println("hashcodeT1:" + t1.hashCode() + " hashcodeCT1:" + ct1.hashCode() + " hashcodeCCT1:" + cct1.hashCode());
        System.out.println("hashcodeT2:" + t2.hashCode() + " hashcodeCT2:" + ct2.hashCode());
        System.out.println("hashcodeT3:" + t3.hashCode() + " hashcodeCT3:" + ct3.hashCode());


        t1.add("1");

        List<String> temp1 = new ArrayList<>();
        temp1.add("2");
        t2.add(temp1);

        List<List<String>> temp2 = new ArrayList<>();
        List<String> temp3 = new ArrayList<>();
        temp3.add("3");
        temp2.add(temp3);
        t3.add(temp2);

        System.out.println("hashcodeT1:" + t1.hashCode() + " hashcodeCT1:" + ct1.hashCode() + " hashcodeCCT1:" + cct1.hashCode());
        System.out.println("hashcodeT2:" + t2.hashCode() + " hashcodeCT2:" + ct2.hashCode());
        System.out.println("hashcodeT3:" + t3.hashCode() + " hashcodeCT3:" + ct3.hashCode());

        System.out.println("t1:" + t1);
        System.out.println("t2:" + t2);
        System.out.println("t3:" + t3);

        System.out.println("cct1:" + cct1);
        System.out.println("ct1:" + ct1);
        System.out.println("ct2:" + ct2);
        System.out.println("ct3:" + ct3);
    }

    public void testInt() {
        List<List<List<Integer>>> t3 = new ArrayList<>();
        List<List<Integer>> t2 = new ArrayList<>();
        List<Integer> t1 = new ArrayList<>();
        t1.add(0);
        t2.add(t1);
        t3.add(t2);

        System.out.println("t1:" + t1);
        System.out.println("t2:" + t2);
        System.out.println("t3:" + t3);
        System.out.println("-----------");

        List<Integer> ct1 = new ArrayList<>(t1);
//        List<String> ct1 = new ArrayList<>();
//        for (String s : t1)
//            ct1.add(s);

        List<Integer> cct1 = t1;

        System.out.println("hashcodeT1:" + t1.hashCode() + " hashcodeCT1:" + ct1.hashCode() + " hashcodeCCT1:" + cct1.hashCode());


        List<List<Integer>> ct2 = new ArrayList<>(t2);
        List<List<List<Integer>>> ct3 = new ArrayList<>(t3);

        t1.add(1);

        List<Integer> temp1 = new ArrayList<>();
        temp1.add(2);
        t2.add(temp1);

        List<List<Integer>> temp2 = new ArrayList<>();
        List<Integer> temp3 = new ArrayList<>();
        temp3.add(3);
        temp2.add(temp3);
        t3.add(temp2);

        System.out.println("hashcodeT1:" + t1.hashCode() + " hashcodeCT1:" + ct1.hashCode() + " hashcodeCCT1:" + cct1.hashCode());
        System.out.println("hashcodeT2:" + t2.hashCode() + " hashcodeCT2:" + ct2.hashCode());
        System.out.println("hashcodeT3:" + t3.hashCode() + " hashcodeCT3:" + ct3.hashCode());

        System.out.println("t1:" + t1);
        System.out.println("t2:" + t2);
        System.out.println("t3:" + t3);

        System.out.println("cct1:" + cct1);
        System.out.println("ct1:" + ct1);
        System.out.println("ct2:" + ct2);
        System.out.println("ct3:" + ct3);
    }

}
