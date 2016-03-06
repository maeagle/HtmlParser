package com.maeagle.parser;

import com.halo.core.common.PropertiesUtils;
import com.maeagle.parser.business.BookOrderExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class BookOrderParser {

    private static Logger logger = LoggerFactory.getLogger(BookOrderParser.class);

    private static Date startDate = new Date();

    private static List<String[]> accountList = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        if (!"now".equals(args[0])) {
            startDate = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").parse(args[0]);
        }
        String[] accounts = StringUtils.defaultString(args[1], "").split(";");
        if (accounts.length != 0) {
            for (String account : accounts) {
                String[] res = StringUtils.defaultString(account, ",").split(",");
                if (res.length != 2) {
                    logger.error("存在不符合要求的账号! 已经跳过...");
                    continue;
                }
                accountList.add(res);
            }
        } else {
            throw new Exception("请设置账号! 格式如下: 用户名1,密码1;用户名2,密码2...");
        }
        System.out.println("启动时间：" + new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").format(startDate));
        System.out.println("用户数量：" + accountList.size());
        System.out.println("单用户线程数：" + PropertiesUtils.getProperty("parser.bdgj.pre_account.thread.count"));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                accountList.stream().forEach(account -> {
                    Thread thread = new Thread(new BookOrderExecutor(account[0], account[1]));
                    thread.start();
                });
            }
        }, startDate);
        System.in.read();
        System.exit(0);
    }

}
