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

package io.github.viscent.mtpattern.ch4.gs;

import io.github.viscent.util.Debug;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionVarBlocker implements Blocker {
    private final Lock lock;
    private final Condition condition;
    private final boolean allowAccess2Lock;

    public ConditionVarBlocker(Lock lock) {
        this(lock, true);
    }

    private ConditionVarBlocker(Lock lock, boolean allowAccess2Lock) {
        this.lock = lock;
        this.allowAccess2Lock = allowAccess2Lock;
        this.condition = lock.newCondition();
    }

    public ConditionVarBlocker() {
        this(false);
    }

    public ConditionVarBlocker(boolean allowAccess2Lock) {
        this(new ReentrantLock(), allowAccess2Lock);
    }

    public Lock getLock() {
        if (allowAccess2Lock) {
            return this.lock;
        }
        throw new IllegalStateException("Access to the lock disallowed.");
    }

    @Override
    public <V> V callWithGuard(GuardedAction<V> guardedAction) throws Exception {

        lock.lockInterruptibly();
        V actionReturnValue;
        try {
            //mark 4.5. 获取保护条件
            //保护条件
            final Predicate guard = guardedAction.guard;

            //mark 6.7.8. 循环直至某次被唤醒发现条件满足
            while (!guard.evaluate()) {// 只要不满足

                Debug.info("waiting...");
                condition.await();
            }

            //满足以后才会到这


            //mark 9.10 执行目标动作
            actionReturnValue = guardedAction.call();
            //mark 11
            return actionReturnValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void signalAfter(Callable<Boolean> stateOperation) throws Exception {

        //这个锁 使stateOperation.call()改变保护方法执行线程的  while (这里面的条件) 可见
        lock.lockInterruptibly();
        try {
            //mark 唤醒4.5.改变GuardedObject实例的状态 并记录返回值
            Boolean shouldSignalBlocker = stateOperation.call();
            if (shouldSignalBlocker) {
                //mark 唤醒6.7. 唤醒被该condition实例暂挂的一个线程
                condition.signal();
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void broadcastAfter(Callable<Boolean> stateOperation)
            throws Exception {
        lock.lockInterruptibly();
        try {
            if (stateOperation.call()) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void signal() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            condition.signal();

        } finally {
            lock.unlock();
        }

    }
}