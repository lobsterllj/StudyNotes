package ThreadCommunication;
/*
管程法
 */
public class Monitor {
    public static void main(String[] args) {
        SynContainer container = new SynContainer();
        new productor(container).start();
        new consumer(container).start();
    }
}

class productor extends Thread {
    SynContainer container;

    public productor(SynContainer container) {
        this.container = container;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            container.push(new Chicken(i));
            System.out.println("生产了：" + i + "只鸡");
        }
    }
}

class consumer extends Thread {
    SynContainer container;

    public consumer(SynContainer container) {
        this.container = container;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            System.out.println("消费了：" + container.pop().id + "只鸡");
        }
    }
}

class Chicken {
    int id;

    public Chicken(int id) {
        this.id = id;
    }
}

class SynContainer {
    //容器大小
    Chicken[] chickens = new Chicken[10];
    //容器计数器
    int count = 0;

    //生产者放入产品
    public synchronized void push(Chicken chicken) {
        //如果满了，等待消费
        if (count == chickens.length) {
            //通知消费者消费，生产等待
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //没满接着丢入
        chickens[count] = chicken;
        count++;

        //通知消费
        this.notifyAll();
    }

    public synchronized Chicken pop() {
        //判断能否消费
        if (count == 0) {
            //等待生产，消费者等待
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //可以消费
        count--;
        Chicken chicken = chickens[count];

        //通知生产者
        this.notifyAll();
        return chicken;
    }
}