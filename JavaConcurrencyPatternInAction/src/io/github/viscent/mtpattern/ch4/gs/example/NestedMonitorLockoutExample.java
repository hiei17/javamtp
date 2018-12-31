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

package io.github.viscent.mtpattern.ch4.gs.example;

import io.github.viscent.mtpattern.ch4.gs.Blocker;
import io.github.viscent.mtpattern.ch4.gs.ConditionVarBlocker;
import io.github.viscent.mtpattern.ch4.gs.GuardedAction;
import io.github.viscent.mtpattern.ch4.gs.Predicate;
import io.github.viscent.util.Debug;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * 本程序是为了演示“嵌套监视器锁死“而写的，因此本程序需要通过手工终止进程才能结束。
 * mark 因为唤醒和运行目标  方法都加了synchronized  死锁了
 * 执行里面让出了condition锁休眠了 等唤醒线程把它唤醒 但是唤醒线程又在等执行外面得到的NestedMonitorLockoutExample对象上的锁
 * @author Viscent Huang
 *
 */
public class NestedMonitorLockoutExample {

    public static void main(String[] args) {

        final Helper helper = new Helper();
        Debug.info("Before calling guaredMethod.");

        Thread t = new Thread(() -> {
            String result;
            //保护方法执行线程
            result = helper.xGuarededMethod("test");
            Debug.info(result);
        });
        t.start();

        final Timer timer = new Timer();

        // 延迟50ms调用helper.stateChanged方法
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                helper.xStateChanged();
                timer.cancel();
            }

        };
        //保护方法唤醒线程
        timer.schedule(task, 50, 10);


    }

    private static class Helper {

        //保护条件
        private volatile boolean isStateOK = false;
        private final Predicate stateBeOK = () -> isStateOK;

        private final Blocker blocker = new ConditionVarBlocker();

        public synchronized String xGuarededMethod(final String message) {

            GuardedAction<String> ga = new GuardedAction<String>(stateBeOK) {

                //mark 目标动作
                @Override
                public String call() {
                    return message + "->received.";
                }

            };

            String result = null;
            try {
                //mark 执行目标(堵塞)
                result = blocker.callWithGuard(ga);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }


        public synchronized void xStateChanged() {
            try {

                Callable<Boolean> booleanCallable = () -> {
                    isStateOK = true;
                    Debug.info("state ok.");
                    return Boolean.TRUE;
                };

                //mark 唤醒
                blocker.signalAfter(booleanCallable);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}