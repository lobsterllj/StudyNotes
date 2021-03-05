package ThreadPool;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestPool {
    public static void main(String[] args) {
        //参数为线程池大小
        ExecutorService service = Executors.newFixedThreadPool(10);

        service.execute(new myThread());
        service.execute(new myThread());
        service.execute(new myThread());
        service.execute(new myThread());

        service.shutdown();
    }
}

class myThread implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}