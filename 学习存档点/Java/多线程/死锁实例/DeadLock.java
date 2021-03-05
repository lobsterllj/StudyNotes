package Lock;

//死锁
//互斥 资源独占
//占有并等待 线程请求许多资源（a,b），比如请求占用了a，没获得b，此时并不会主动释放a，而是占着a，请求者b
//不可抢占 一个线程已经占有的资源不能被其他线程抢占
//环路等待 形成等待环
public class DeadLock {
    public static void main(String[] args) {
        MakeUp girl1 = new MakeUp(0, "alice");
        MakeUp girl2 = new MakeUp(1, "bolles");
        girl1.start();
        girl2.start();
    }
}

class LipStick {

}

class Mirror {

}

class MakeUp extends Thread {
    //需要的资源只有一份，用static保证只有一份
    static LipStick lipStick = new LipStick();
    static Mirror mirror = new Mirror();

    int choice;
    String girlName;


    public MakeUp(int choice, String girlName) {
        this.choice = choice;
        this.girlName = girlName;
    }

    private void makeup() throws InterruptedException {
        if (choice == 0) {
            synchronized (lipStick) {
                System.out.println(this.girlName + "获得口红");
                Thread.sleep(1000);
                synchronized (mirror) {
                    System.out.println(this.girlName + "获得镜子");
                }
            }
        } else {
            synchronized (mirror) {
                System.out.println(this.girlName + "获得镜子");
                Thread.sleep(1000);
                synchronized (lipStick) {
                    System.out.println(this.girlName + "获得口红");
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            makeup();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}