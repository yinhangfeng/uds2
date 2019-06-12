package com.mrwind.uds.test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkJoinPoolTest {

    static int computeCount = 0;

    static class Fibonacci extends RecursiveTask<Integer> {
        int n;

        Fibonacci(int n) {
            this.n = n;
        }

        @Override
        protected Integer compute() {
            computeCount ++;
            System.out.println("Current thread is " + Thread.currentThread()
                    + "\n n = " + n + "\n");

            if (n <= 2)
                return 1;
            Fibonacci f1 = new Fibonacci(n - 1);
            f1.fork();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Fibonacci f2 = new Fibonacci(n - 2);
            f2.fork();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("wait temp answer is :" + n + "\n");
            int answer = f1.join() + f2.join();
            System.out.println("temp answer is :" + answer  + ",  n is :" +n +"\n");
            return answer;
        }
    }

    public static void test1() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        Fibonacci task = new Fibonacci(10);
        int answer = pool.invoke(task);
        System.out.println("Hello answer is :" + answer +  " , compute count is :" + computeCount);
    }
}
