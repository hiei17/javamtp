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

package io.github.viscent.mtpattern.ch11.stc.example;

import io.github.viscent.mtpattern.ch11.stc.AbstractSerializer;
import io.github.viscent.util.Debug;
import io.github.viscent.util.Tools;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ReusableCodeExample {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        SomeService ss = new SomeService();

        ss.init();

        Future<String> result = ss.doSomething("Serial Thread Confinement", 1);
        // 模拟执行其他操作
        Tools.randomPause(100, 50);

        Debug.info(result.get());

        ss.shutdown();

    }

    //mark 1 .定义任务
    private static class Task {

        public final String message;
        public final int id;

        public Task(String message, int id) {
            this.message = message;
            this.id = id;
        }
    }

    //Serial
    static class SomeService extends AbstractSerializer<Task, String> {

        public SomeService() {
            super(

                    new ArrayBlockingQueue<>(100),

                    //mark 3.定义 TaskProcessor 接口的实现类(就是任务的消费逻辑
                    task -> {
                        Debug.info("[" + task.id + "]:" + task.message);
                        return task.message + " accepted.";
                    }
            );
        }

        //mark 2.实现父类这个 包装任务 的方法
        @Override
        protected Task makeTask(Object... params) {

            String message = (String) params[0];
            int id = (Integer) params[1];

            return new Task(message, id);
        }

        //mark 2. 具体服务方法 里面调用父类service方法  这里只是为了取个具体的名字
        public Future<String> doSomething(String message, int id) throws InterruptedException {

            return service(message, id);
        }

    }

}
