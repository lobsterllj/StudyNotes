public class main {
    public static void main(String[] args) {
        main main = new main();
        prtMatrix prtMatrix = new prtMatrix();
        prtInts prtInts = new prtInts();

        ZhansanCalculator zhansanCalculator = new ZhansanCalculator();
        SellerZ s1 = new SellerZ("s1", zhansanCalculator);
        s1.quote(30000d, 15000d);

        WangwuCalculator wangwuCalculator = new WangwuCalculator();
        SellerW s2 = new SellerW("s2", wangwuCalculator);
        s2.quote(30000d, 15000d);
    }

    interface Icount {
        public double getCount(double salary, double bonus);
    }

    static class ZhansanCalculator implements Icount {
        public double getCount(double salary, double bonus) {
            return salary + bonus;
        }
    }

    static class WangwuCalculator implements Icount {
        public double getCount(double salary, double bonus) {
            return salary + bonus + 1d;
        }
    }

    static class SellerZ {
        String name;
        ZhansanCalculator zhansanCalculator;

        public SellerZ(String name, ZhansanCalculator calculator) {
            this.name = name;
            this.zhansanCalculator = calculator;
        }

        public void quote(double salary, double bonus) {
            /* 使用接口的好处，接口已经规定好要实现的方法，以及方法的准确命名 */
            /* 调用实现接口规定方法的类便可正确计算方法 */
            System.out.println("Z请支付: " + zhansanCalculator.getCount(salary, bonus));

        }

    }

    static class SellerW {
        String name;
        WangwuCalculator wangwuCalculator;

        public SellerW(String name, WangwuCalculator calculator) {
            this.name = name;
            this.wangwuCalculator = calculator;
        }

        public void quote(double salary, double bonus) {
            /* 使用接口的好处，接口已经规定好要实现的方法，以及方法的准确命名 */
            /* 调用实现接口规定方法的类便可正确计算方法 */
            System.out.println("W请支付: " + wangwuCalculator.getCount(salary, bonus));

        }

    }

}
