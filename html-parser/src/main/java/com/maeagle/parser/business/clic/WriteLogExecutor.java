package com.maeagle.parser.business.clic;

import com.halo.core.common.util.PropertiesUtils;
import com.maeagle.parser.models.clic.AccountInfo;
import com.maeagle.parser.models.clic.LogInfo;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WriteLogExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(WriteLogExecutor.class);

    private String id;

    private CloseableHttpClient httpClient;

    private AccountInfo user;

    private Date startTime;

    private Date endTime;

    private int countPreThread = 10;

    public WriteLogExecutor(CloseableHttpClient httpClient, String id, AccountInfo user, Date startTime, Date endTime) {
        this.httpClient = httpClient;
        this.id = id;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void run() {

        try {
            logger.info("[{}]:登录账号：{}", id, user.getUserName());
            login(httpClient);
        } catch (Exception e) {
            logger.error("[" + id + "]: 登录账号：" + user.getUserName() + " 失败！");
            logger.error(e.getMessage(), e);
            return;
        }

        Calendar cursor = Calendar.getInstance();
        cursor.setTime(startTime);
        Calendar target = Calendar.getInstance();
        target.setTime(endTime);
        target.set(Calendar.HOUR, 18);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        List<LogInfo> logInfoList = new ArrayList<>();
        while (cursor.compareTo(target) <= 0) {

            if (cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                cursor.add(Calendar.DATE, 1);
                continue;
            }
            cursor.set(Calendar.HOUR, 9);
            cursor.set(Calendar.MINUTE, 0);
            cursor.set(Calendar.SECOND, 0);

            Calendar midCal = Calendar.getInstance();
            midCal.setTime(cursor.getTime());
            midCal.set(Calendar.HOUR, 12);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(cursor.getTime());
            endCal.set(Calendar.HOUR, 18);

            LogInfo amLogInfo = new LogInfo();
            amLogInfo.setLogCatalog(user.getLogCatalog());
            amLogInfo.setContent(user.getLogContentList().get(RandomUtils.nextInt(0, user.getLogContentList().size() - 1)));
            amLogInfo.setStartTime(cursor.getTime());
            amLogInfo.setEndTime(midCal.getTime());
            logInfoList.add(amLogInfo);

            LogInfo pmLogInfo = new LogInfo();
            pmLogInfo.setLogCatalog(user.getLogCatalog());
            pmLogInfo.setContent(user.getLogContentList().get(RandomUtils.nextInt(0, user.getLogContentList().size() - 1)));
            pmLogInfo.setStartTime(midCal.getTime());
            pmLogInfo.setEndTime(endCal.getTime());
            logInfoList.add(pmLogInfo);

            cursor.add(Calendar.DATE, 1);

            if (logInfoList.size() >= countPreThread) {
                new WrittingThread(httpClient, id, user, logInfoList).run();
//                new Thread(new WrittingThread(httpClient, id, user, logInfoList)).start();
                logInfoList = new ArrayList<>();
            }
        }
        if (!logInfoList.isEmpty())
            new WrittingThread(httpClient, id, user, logInfoList).run();
        //new Thread(new WrittingThread(httpClient, id, user, logInfoList)).start();

    }


    /**
     * 登录请求.
     *
     * @param httpclient the httpclient
     * @return the boolean
     */
    private void login(CloseableHttpClient httpclient) throws Exception {

        /**
         * 初始化cookies
         */
        HttpUriRequest accessRequest = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.clic.it.url.login")).build();
        String responseStr = "";
        try (CloseableHttpResponse response = httpclient.execute(accessRequest)) {
            responseStr = EntityUtils.toString(response.getEntity());
            int status = response.getStatusLine().getStatusCode();
            if (status != 200 && status != 302)
                throw new NullPointerException("获取cookies错误！\n" + responseStr);
        }
        /**
         * 构建登录请求参数
         */
        Document htmlDoc = Jsoup.parse(responseStr);
        List<NameValuePair> accountInfo = new ArrayList<>();
        accountInfo.add(new BasicNameValuePair("__VIEWSTATE", htmlDoc.select("#__VIEWSTATE").attr("value")));
        accountInfo.add(new BasicNameValuePair("__EVENTVALIDATION", htmlDoc.select("#__EVENTVALIDATION").attr("value")));
        accountInfo.add(new BasicNameValuePair("Content$TextUserName", user.getUserName()));
        accountInfo.add(new BasicNameValuePair("Content$TextPassword", user.getPassword()));
        accountInfo.add(new BasicNameValuePair("Content$Button1", "登录"));

        /**
         * 执行登录
         */
        HttpUriRequest loginRequest = RequestBuilder.post()
                .setUri(PropertiesUtils.getProperty("parser.clic.it.url.login"))
                .setEntity(new UrlEncodedFormEntity(accountInfo, Charset.defaultCharset())).build();
        try (CloseableHttpResponse response = httpclient.execute(loginRequest)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 302) {
                throw new NullPointerException("登录失败！\n" + EntityUtils.toString(response.getEntity()));
            }
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

}
