package com.maeagle.parser;

import com.halo.core.common.util.CommonUtils;
import com.halo.core.common.util.PropertiesUtils;
import com.halo.core.common.util.TransactionUtils;
import com.maeagle.parser.business.clic.WriteLogExecutor;
import com.maeagle.parser.models.clic.AccountInfo;
import com.maeagle.parser.models.clic.LogCatalog;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public class ClicLogWritterParser {

    private static Logger logger = LoggerFactory.getLogger(ClicLogWritterParser.class);

    public static void main(String[] args) throws Exception {

        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = null;

        try {
            cookieStore.clear();
            httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();

            AccountInfo account = new AccountInfo();
            account.setUserName(PropertiesUtils.getProperty("parser.clic.it.account.username"));
            account.setPassword(PropertiesUtils.getProperty("parser.clic.it.account.password"));

            LogCatalog logCatalog = new LogCatalog();
            logCatalog.setCatalog(PropertiesUtils.getProperty("parser.clic.it.account.log.catalog"));
            logCatalog.setSubcatalog(PropertiesUtils.getProperty("parser.clic.it.account.log.subcatalog"));
            account.setLogCatalog(logCatalog);

            account.setLogContentList(Arrays.stream(PropertiesUtils.getProperty("parser.clic.it.account.log.text").split(";")).collect(Collectors.toList()));

            Date start = CommonUtils.convertStrToDate(PropertiesUtils.getProperty("parser.clic.it.account.log.starttime"), "yyyy-MM-dd");
            Date end = CommonUtils.convertStrToDate(PropertiesUtils.getProperty("parser.clic.it.account.log.endtime"), "yyyy-MM-dd");

            WriteLogExecutor executor = new WriteLogExecutor(httpclient, TransactionUtils.generateTransactionCode(), account, start, end);
            executor.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            cookieStore.getCookies().forEach(cookie -> System.out.println(cookie.getName() + " : " + cookie.getValue()));
            try {
                httpclient.close();
            } catch (Exception e) {
            }
        }
    }

}
