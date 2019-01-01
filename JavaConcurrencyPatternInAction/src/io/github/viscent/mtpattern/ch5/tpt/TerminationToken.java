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

package io.github.viscent.mtpattern.ch5.tpt;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程停止标志。
 * 
 * @author Viscent Huang
 *
 */
public class TerminationToken {

    //mark  使用volatile修饰，以保证无需显式锁的情况下该变量的内存可见性
    //表示可停止了
    public volatile boolean toShutdown = false;

    //mark 多少未完成的工作的计数  终止了也要完成先
    public final AtomicInteger reservations = new AtomicInteger(0);

    /*
     * 在多个可停止线程实例共享一个TerminationToken实例的情况下，
     * 该队列用于记录那些共享TerminationToken实例的可停止线程，
     * 以便尽可能减少锁的使用 的情况下，实现这些线程的停止。
     */
    private final Queue<WeakReference<Terminatable>> coordinatedThreads;

    public TerminationToken() {
        coordinatedThreads = new ConcurrentLinkedQueue<>();
    }

    protected void register(Terminatable thread) {
        coordinatedThreads.add(new WeakReference<>(thread));
    }

    /**
     * 一个线程停止了， 它发起停止其它未被停止的线程。
     * mark 用于支持多个 可停止线程 都用本线程终止标志
     * @param thread 已停止的线程
     */
    protected void notifyThreadTermination(Terminatable thread) {

        WeakReference<Terminatable> wrThread;
        Terminatable otherThread;
        while (null != (wrThread = coordinatedThreads.poll())) {

            otherThread = wrThread.get();
            if (null != otherThread && otherThread != thread) {
                otherThread.terminate();
            }
        }
    }

}