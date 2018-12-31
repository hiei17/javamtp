/*
授权声明：
本源码系《Java多线程编程实战指南（设计模式篇）》一书（ISBN：978-7-121-27006-2，以下称之为“原书”）的配套源码，
欲了解本代码的更多细节，请参考原书。
本代码仅为原书的配套说明之用，并不附带任何承诺（如质量保证和收益）。
以任何形式将本代码之部分或者全部用于营利性用途需经版权人书面同意。
将本代码之部分或者全部用于非营利性用途需要在代码中保留本声明。
任何对本代码的修改需在代码中以注释的形式注明修改人、修改时间以及修改内容。
本代码可以从以下网址下载：
https://github.com/Viscent/javamtp
http://www.broadview.com.cn/27006
*/

package io.github.viscent.mtpattern.ch9.threadpool.example;

import io.github.viscent.util.Debug;

import java.util.concurrent.*;

public class ThreadPoolDeadLockAvoidance {

    private final ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(
                    1,
                    //mark 1 最大线程池大小为1（有限数值）：
                    1,

                    60, TimeUnit.SECONDS,

                    //mark 2 工作队列为SynchronousQueue：
                    new SynchronousQueue<>(),

                    //mark 3 线程池饱和处理策略为CallerRunsPolicy：
                    new ThreadPoolExecutor.CallerRunsPolicy());

    public static void main(String[] args) {

        ThreadPoolDeadLockAvoidance me = new ThreadPoolDeadLockAvoidance();
        me.test("<This will NOT deadlock>");
    }

    public void test(final String message) {
        Runnable taskA = () -> {

            Debug.info("Executing TaskA...");

            Runnable taskB = () -> Debug.info("TaskB processes " + message);

            //mark  线程池里面就一个线程,无等待队列 ,
            //这样提交一定失败
            //饱和策略是 打回这里提交的地方 由提交的线程自己处理
            //因此编程了 TaskB processes  TaskA Done. 这样串行了
            Future<?> result = threadPool.submit(taskB);

            try {
                // mark TaskB执行结束 才能继续执行TaskA，
                // 使TaskA和TaskB称为由依赖关系的两个任务
                result.get();
            } catch (InterruptedException e) {
                ;
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            Debug.info("TaskA Done.");
        };

        threadPool.submit(taskA);
    }

}
