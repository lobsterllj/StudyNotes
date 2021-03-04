import java.util.*;

public class main {
    public static void main(String[] args) {
        main main = new main();
        prtMatrix prtMatrix = new prtMatrix();
        prtInts prtInts = new prtInts();

        Calculator calculator = new Calculator();
        Seller a = new Seller("a", calculator);
        a.quote(30000d, 15000d);
    }

    static class Calculator {
        public double count(double salary, double bonus) {
            return salary + bonus;
        }
    }

    static class Seller {
        String name;
        Calculator calculator;

        public Seller(String name, Calculator calculator) {
            this.name = name;
            this.calculator = calculator;
        }

        public void quote(double salary, double bonus) {
            /* 不使用接口的缺点，使用方法的人必须知道 实现方法的人对方法的准确命名 */
            // eg(wrong) : System.out.println("请支付" + calculator.countMoney(salary,bonus));
            System.out.println("请支付: " + calculator.count(salary, bonus));

        }

    }

}
