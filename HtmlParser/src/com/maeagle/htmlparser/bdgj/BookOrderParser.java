package com.maeagle.htmlparser.bdgj;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookOrderParser {

	private static final int thread_to_run = 20;

	private static int currentCount = 0;

	private static final String start_date_time = "2015-10-05 00:00:00";
	
	public static void main(String[] args) throws Exception {

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = df.parse(start_date_time);
		
		startDate = new Date();
		
		AtomicBoolean successFlag = new AtomicBoolean(false);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (!successFlag.get()) {
					System.out.println("==================第" + (currentCount++) + "次执行定时器！");
					for (int i = 0; i < thread_to_run; i++) {
						Thread thread = new Thread(new BookOrderThread(successFlag));
						thread.start();
					}
				} else {
					System.out.println("已经挂完号了！");
					timer.cancel();
				}
			}
		}, startDate, 3000);

		System.out.println("第一次启动时间：" + startDate.toString());
		System.in.read();
		timer.cancel();
	}

}
