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

package io.github.viscent.mtpattern.ch10.tss.example;

import io.github.viscent.util.Debug;

import java.text.DecimalFormat;
import java.util.concurrent.*;

public class SmsVerficationCodeSender {

    private static final ExecutorService EXECUTOR =
            new ThreadPoolExecutor(

                    //mark 事先创好核心线程 来了任务可能就不用等创建线程了
                    //一般就保留1个 最多和cpu一样多
                    1,//没满corePoolSize之前来一个任务都多一个线程 ,不排队(即使有线程闲着)
                    Runtime.getRuntime().availableProcessors(),

                    //闲置60s 就销毁线程
                    60,
                    TimeUnit.SECONDS,

                    //无等待
                    new SynchronousQueue<>(),

                    //线程取名 守护者
                    r -> {
                        Thread t = new Thread(r, "VerfCodeSender");
                        t.setDaemon(true);
                        return t;
                    },

                    //来不及处理就丢弃
                    new ThreadPoolExecutor.DiscardPolicy()
            );

    public static void main(String[] args) {
        SmsVerficationCodeSender client = new SmsVerficationCodeSender();

        client.sendVerificationSms("18912345678");
        client.sendVerificationSms("18712345679");
        client.sendVerificationSms("18612345676");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ;
        }
    }

    /**
     * 生成并下发验证码短信到指定的手机号码。
     * 
     * @param msisdn
     *            短信接收方号码。
     */
    public void sendVerificationSms(final String msisdn) {

        Runnable task = () -> {
            // 生成强随机数验证码
            int verificationCode =
                    ThreadSpecificSecureRandom.INSTANCE
                            .nextInt(999999);
            DecimalFormat df = new DecimalFormat("000000");
            String txtVerCode = df.format(verificationCode);

            // 发送验证码短信
            sendSms(msisdn, txtVerCode);
        };

        EXECUTOR.submit(task);
    }

    private void sendSms(String msisdn, String verificationCode) {
        Debug.info("Sending verification code " + verificationCode + " to "
                        + msisdn);

        // 省略其他代码
    }

}
