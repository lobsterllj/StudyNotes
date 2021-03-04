public class main {
    public static void main(String[] args) {
        main main = new main();
        prtMatrix prtMatrix = new prtMatrix();
        prtInts prtInts = new prtInts();
        formMatrix formMatrix = new formMatrix();

//        Pizza.PizzaStatus pizza = null;
//        System.out.println(pizza.equals(Pizza.PizzaStatus.DELIVERED));//空指针异常
//        System.out.println(pizza == Pizza.PizzaStatus.DELIVERED);//正常运行
//        if (Pizza.PizzaStatus.DELIVERED.equals(TestColor.GREEN)); // 编译正常
//        if (Pizza.PizzaStatus.DELIVERED == TestColor.GREEN);      // 编译失败，类型不匹配

        threaddef[] threaddef = new threaddef[5];
        for (int i = 0; i < 5; ++i) {
            threaddef[i] = new threaddef(i);
        }
        threaddef[0].run();
        threaddef[1].run();
        threaddef[2].run();
        threaddef[3].run();
        threaddef[4].run();
    }

    static class Resource {
        private int cnt = 0;

        public void increase() {
            cnt++;
        }

        public int getCnt() {
            return cnt;
        }
    }

    public enum SomeThing {
        INSTANCE;
        private Resource instance;

        SomeThing() {
            instance = new Resource();
        }

        public Resource getInstance() {
            return instance;
        }
    }

    static class threaddef implements Runnable {
        int num;

        public threaddef(int i) {
            num = i;
        }

        @Override
        public void run() {
            System.out.println("定义新的thread:" + num);
            Resource it = SomeThing.INSTANCE.instance;
            System.out.println(it.getCnt());
            it.increase();
        }

    }
}

