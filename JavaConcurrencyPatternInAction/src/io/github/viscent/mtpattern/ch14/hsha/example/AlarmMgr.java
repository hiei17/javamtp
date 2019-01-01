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

import io.github.viscent.mtpattern.ch5.tpt.example.AlarmType;
import io.github.viscent.util.Debug;

/**
 * 告警功能入口类。 模式角色：HalfSync/HalfAsync.AsyncTask
 * 模式角色：Two-phaseTermination.ThreadOwner
 */
public class AlarmMgr {

    //单例模式
    private static final AlarmMgr INSTANCE = new AlarmMgr();
    private AlarmMgr() {
        alarmSendingThread = new AlarmSendingThread();

    }
    public static AlarmMgr getInstance() {
        return INSTANCE;
    }


    //构造以后 调用init
    private final AlarmSendingThread alarmSendingThread;
    public void init() {
        alarmSendingThread.start();
    }


    //已终止 标志
    private volatile boolean shutdownRequested = false;

    /**
     * 发送告警
     *
     * @param type 告警类型
     * @param id 告警编号
     *
     * @param extraInfo 告警参数
     *
     * @return 由type+id+extraInfo唯一确定的告警信息被提交的次数。-1表示告警管理器已被关闭。
     */
    public int sendAlarm(AlarmType type, String id, String extraInfo) {

        Debug.info("Trigger alarm " + type + "," + id + ',' + extraInfo);

        int duplicateSubmissionCount = 0;
        try {
            AlarmInfo alarmInfo = new AlarmInfo(id, type);
            alarmInfo.setExtraInfo(extraInfo);

            duplicateSubmissionCount = alarmSendingThread.sendAlarm(alarmInfo);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return duplicateSubmissionCount;
    }

    //mark 1.客户端调用 线程拥有者的关闭方便
    public synchronized void shutdown() {

        if (shutdownRequested) {
            throw new IllegalStateException("shutdown already requested!");
        }

        //mark 2. 调用目标线程的终止
        alarmSendingThread.terminate();

        shutdownRequested = true;
    }

}