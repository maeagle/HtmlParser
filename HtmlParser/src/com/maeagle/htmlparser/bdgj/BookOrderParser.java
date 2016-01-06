package com.maeagle.htmlparser.bdgj;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookOrderParser {

    private static int thread_to_run = 1;

    private static int currentCount = 0;

    private static int interval = 500;

    private static String start_date_time = "2016-01-06/23:59:59";

    public static void main(String[] args) throws Exception {

        if (args != null && args.length != 0) {
            BookOrderThread.userName = args[0];
            BookOrderThread.password = args[1];
            start_date_time = args[2];
            thread_to_run = Integer.parseInt(args[3]);
            interval = Integer.parseInt(args[4]);
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
        Date startDate = df.parse(start_date_time);

        System.out.println("用户名：" + BookOrderThread.userName);
        System.out.println("密码：" + BookOrderThread.password);
        System.out.println("启动时间：" + start_date_time);
        System.out.println("并发数：" + thread_to_run);
        System.out.println("执行间隔：" + interval + " ms");

        AtomicBoolean successFlag = new AtomicBoolean(false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!successFlag.get()) {
                    System.out.println("==================第" + (++currentCount) + "次执行定时器！");
                    for (int i = 0; i < thread_to_run; i++) {
                        Thread thread = new Thread(new BookOrderThread(successFlag));
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
