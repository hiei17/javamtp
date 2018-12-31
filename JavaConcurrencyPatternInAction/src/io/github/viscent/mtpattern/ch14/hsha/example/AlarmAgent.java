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

package io.github.viscent.mtpattern.ch14.hsha.example;

import io.github.viscent.mtpattern.ch4.gs.Blocker;
import io.github.viscent.mtpattern.ch4.gs.ConditionVarBlocker;
import io.github.viscent.mtpattern.ch4.gs.GuardedAction;
import io.github.viscent.mtpattern.ch4.gs.Predicate;
import io.github.viscent.util.Debug;
import io.github.viscent.util.Tools;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * 负责连接告警服务器，并发送告警信息至告警服务器
 *
 */
public class AlarmAgent {

    // 用于记录AlarmAgent是否连接上告警服务器
    private volatile boolean connectedToServer = false;

    // 模式角色：GuardedSuspension.Predicate
    private final Predicate agentConnectedPredicate = () -> connectedToServer;

    // 模式角色：GuardedSuspension.Blocker
    private final Blocker blocker = new ConditionVarBlocker();

    // 心跳定时器
    private final Timer heartbeatTimer = new Timer(true);

    // 省略其他代码

    /**
     * //mark 1.客户端调用GuardedObject.guardedMethod
     *
     * 这就是guardedMethod
     * @param alarm
     *            告警信息
     * @throws Exception
     */
    public void sendAlarm(final AlarmInfo alarm) throws Exception {
        /*
         * 可能需要等待，直到AlarmAgent连接上告警服务器（或者连接中断后重新连连上服务器）。<br/>
         * AlarmInfo类的源码参见本书配套下载。
         */
        //mark  2.创建GuardedAction的实例guardedAction
        // 模式角色：GuardedSuspension.GuardedAction (包含:目标动作 和 包含条件)
        GuardedAction<Void> guardedAction =
                new GuardedAction<Void>(agentConnectedPredicate) {//构造传入保护条件

                   //目标动作
                    @Override
                    public Void call() {
                        //等条件满足后才会执行这里
                        doSendAlarm(alarm);
                        return null;
                    }
                };

        //mark 3. guardedAction为参数 调用Blocker.callWithGuard
        //条件要是不满足会堵塞在这
        blocker.callWithGuard(guardedAction);
    }

    // 通过网络连接将告警信息发送给告警服务器
    private void doSendAlarm(AlarmInfo alarm) {

        // 省略其他代码
        Debug.info("sending alarm " + alarm);

        // 模拟发送告警至服务器的耗时
        try {
            Thread.sleep(50);
        } catch (Exception e) {

        }
    }

    public void init() {
        // 省略其他代码

        // 告警连接线程
        Thread connectingThread = new Thread(new ConnectingTask());

        connectingThread.start();

        heartbeatTimer.schedule(new HeartbeatTask(), 60_000, 2000);
    }

    public void disconnect() {
        // 省略其他代码
        Debug.info("disconnected from alarm server.");
        connectedToServer = false;
    }

    /**
     * 网络连接上或者重连上时调用
     * 模型中的stateChanged
     * mark 唤醒1.调用stateChanged 改变GuardedObject实例的状态
     */
    protected void onConnected() {
        try {

            //mark 唤醒2.stateOperation: 封装了 改变GuardedObject实例 所需的操作
            Callable<Boolean> stateOperation = () -> {
                connectedToServer = true;
                Debug.info("connected to server");
                return Boolean.TRUE;
            };

            //mark 唤醒3.(却的几步在blocker.signalAfter里面)
            blocker.signalAfter(stateOperation);

            //mark 唤醒8.此时 搜保护方法的线程 可能已经被唤醒
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mark 唤醒9.stateChanged 返回
    }

    protected void onDisconnected() {
        connectedToServer = false;
    }

    // 负责与告警服务器建立网络连接
    private class ConnectingTask implements Runnable {
        @Override
        public void run() {
            // 省略其他代码

            // 模拟连接操作耗时
            Tools.randomPause(100, 40);

            onConnected();
        }
    }

    /**
     * 心跳定时任务：定时检查与告警服务器的连接是否正常，发现连接异常后自动重新连接
     */
    private class HeartbeatTask extends TimerTask {
        // 省略其他代码

        @Override
        public void run() {
            // 省略其他代码

            if (!testConnection()) {
                onDisconnected();
                reconnect();
            }

        }

        private boolean testConnection() {
            // 省略其他代码

            return true;
        }

        private void reconnect() {
            ConnectingTask connectingThread = new ConnectingTask();

            // 直接在心跳定时器线程中执行
            connectingThread.run();
        }

    }
}