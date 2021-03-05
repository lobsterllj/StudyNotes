package ThreadTest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class TestSum {
    public static void main(String[] args) {
        new myThread1().start();
        new Thread(new myThread2()).start();
        FutureTask<Integer> futureTask = new FutureTask(new myThread3());
        new Thread(futureTask).start();

        try {
            Integer integer = futureTask.get();
            System.out.println(integer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}

//继承Thread类
class myThread1 extends Thread {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}

//实现Runnable接口
class myThread2 implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}

//实现Callable接口
class myThread3 implements Callable {
    @Override
    public Integer call() throws Exception {
        System.out.println(Thread.currentThread().getName());
        return 100;
    }
}