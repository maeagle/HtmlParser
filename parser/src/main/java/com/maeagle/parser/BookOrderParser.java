package com.maeagle.parser;

import com.maeagle.parser.business.BookOrderThread;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BookOrderParser {

    private static int thread_to_run = 1;

    private static int currentCount = 0;

    private static int interval = 500;

    private static Date startDate = new Date();

    public static int maxCount = 1;

    public static void main(String[] args) throws Exception {

        if (args != null && args.length != 0) {
            if (!"now".equals(args[0])) {
                startDate = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").parse(args[0]);
            }
            thread_to_run = Integer.parseInt(args[1]);
            interval = Integer.parseInt(args[2]);
            maxCount = Integer.parseInt(args[3]);
        }
        System.out.println("启动时间：" + new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").format(startDate));
        System.out.println("并发数：" + thread_to_run);
        System.out.println("执行间隔：" + interval + " ms");
        System.out.println("最大数量：" + maxCount + " ms");

        AtomicInteger successCount = new AtomicInteger(0);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (successCount.get() < maxCount) {
                    System.out.println("==================第" + (++currentCount) + "次执行定时器！");
                    for (int i = 0; i < thread_to_run; i++) {
                        Thread thread = new Thread(new BookOrderThread(successCount));
                        thread.start();
                    }
                } else {
                    System.out.println("已经挂完号了！");
                    timer.cancel();
                    System.exit(0);
                }
            }
        }, startDate, interval);
        System.in.read();
        timer.cancel();
    }

}
