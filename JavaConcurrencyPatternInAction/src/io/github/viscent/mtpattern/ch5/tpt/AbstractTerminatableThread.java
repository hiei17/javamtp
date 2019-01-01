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

import org.apache.log4j.Logger;

/**
 * 可停止的抽象线程。
 * 
 * 模式角色：Two-phaseTermination.AbstractTerminatableThread
 * 
 * @author Viscent Huang
 */
public abstract class AbstractTerminatableThread extends Thread implements Terminatable {

    final static Logger logger = Logger.getLogger(AbstractTerminatableThread.class);

    private final boolean DEBUG = true;

    // 模式角色：Two-phaseTermination.TerminationToken
    public final TerminationToken terminationToken;

    public AbstractTerminatableThread() {
        this(new TerminationToken());
    }

    /**
     * 
     * @param terminationToken 线程间共享的线程终止标志实例
     */
    public AbstractTerminatableThread(TerminationToken terminationToken) {
        this.terminationToken = terminationToken;
        terminationToken.register(this);
    }

    /**
     * 留给子类实现其  线程处理逻辑。
     *
     * mark 在run()调用 就是如果没停 正常情况会一直干的工作
     * @throws Exception
     */
    protected abstract void doRun() throws Exception;

    /**
     * mark 留给子类实现。用于实现线程停止后的一些清理动作。
     * 
     * @param cause
     */
    protected void doCleanup(Exception cause) {
        // 什么也不做
    }

    /**
     * mark 留给子类实现。用于执行线程停止所需的操作。
     */
    protected void doTerminiate() {
        // 什么也不做
    }

    @Override
    public void run() {
        Exception ex = null;
        try {
            //mark  死循环不叫停 就会一直从队列里拿来处理
            for (;;) {

                //mark 在执行线程的处理逻辑前先判断线程停止的标志。
                if (terminationToken.toShutdown && terminationToken.reservations.get() <= 0) {
                    break;
                }
                doRun();
            }

        } catch (Exception e) {
            // 使得线程能够响应interrupt调用而退出
            ex = e;
            if (e instanceof InterruptedException) {
                if (DEBUG) {
                    logger.debug(e);
                }
            } else {
                logger.error("", e);
            }
        } finally {
            try {
                //子类实现的清理
                doCleanup(ex);
            } finally {
                //停止 这个线程停止标志 上其他线程
                terminationToken.notifyThreadTermination(this);
            }
        }
    }

    @Override
    public void interrupt() {
        terminate();
    }

    /*
     * mark 请求停止线程。
     *  实现Terminatable接口唯一方法
     * @see io.github.viscent.mtpattern.tpt.Terminatable#terminate()
     */
    @Override
    public void terminate() {

        //mark 3.4. 设标志
        terminationToken.toShutdown=true;
        try {
            //mark 5 调用指令实现 实现终止需要的一些额外操作(有些阻塞interrupt都停不掉),可以空着
            doTerminiate();
        } finally {

            // 若无待处理的任务，则试图强制终止线程
            if (terminationToken.reservations.get() <= 0) {

                //mark 6. 打断wait  sleep  等 让他们抛出异常终止
                super.interrupt();
            }
        }
    }

    public void terminate(boolean waitUtilThreadTerminated) {

        terminate();
        if (waitUtilThreadTerminated) {
            try {
                this.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}