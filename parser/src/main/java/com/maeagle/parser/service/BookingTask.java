package com.maeagle.parser.service;

import com.halo.core.common.PropertiesUtils;
import com.halo.core.init.InitializingTask;
import com.maeagle.parser.business.BookOrderExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by maeagle on 16/3/6.
 */
@Service("bookingTask")
public class BookingTask extends InitializingTask {

    private static Logger logger = LoggerFactory.getLogger(BookingTask.class);

    @Override
    public void execute(ApplicationContext applicationContext) {

        logger.info("启动北大国际挂号服务...");

        String[] accounts = PropertiesUtils.getProperty("parser.bdgj.accounts").split(";");
        if (accounts.length != 0) {
            Arrays.stream(accounts).map(account -> StringUtils.defaultString(account, ",").split(",")).forEach(account -> {
                new Thread(new BookOrderExecutor(account[0], account[1])).start();
            });
        } else {
            logger.error("请设置账号! 格式如下: 用户名1,密码1;用户名2,密码2...");
        }
    }
}
