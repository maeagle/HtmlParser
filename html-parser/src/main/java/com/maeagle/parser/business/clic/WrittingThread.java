package com.maeagle.parser.business.clic;

import com.halo.core.common.util.CommonUtils;
import com.halo.core.common.util.PropertiesUtils;
import com.maeagle.parser.models.clic.log.AccountInfo;
import com.maeagle.parser.models.clic.log.LogInfo;
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
import java.util.List;

public class WrittingThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(WrittingThread.class);

    private CloseableHttpClient httpClient;

    private String id;

    private AccountInfo user;

    private List<LogInfo> logInfoList;

    WrittingThread(CloseableHttpClient httpClient, String id, AccountInfo user, List<LogInfo> logInfoList) {
        this.httpClient = httpClient;
        this.id = id;
        this.logInfoList = logInfoList;
        this.user = user;
    }

    @Override
    public void run() {
        for (LogInfo logInfo : logInfoList) {
            try {
                logger.info("[{}]: 为账号 {} 编写 {} 的日志...", id, user.getUserName(), CommonUtils.convertDateToStr(logInfo.getStartTime(), CommonUtils.YYYY_MM_DD_HH_MM_SS));
                createLog(httpClient, user, logInfo);
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("[" + id + "]: 账号：" + user.getUserName() + " 日志编写失败！");
                logger.error(e.getMessage(), e);
                return;
            }
        }
    }

    private void createLog(CloseableHttpClient httpclient, AccountInfo userInfo, LogInfo log) throws Exception {
        /**
         * 进入日志创建页面，获取token
         */
        HttpUriRequest accessRequest = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.clic.it.url.createlog"))
                .addParameter("fl", log.getLogCatalog().getCatalog())
                .addParameter("WorkDate", CommonUtils.convertDateToStr(log.getStartTime(), "yyyy-MM-dd HH:mm"))
                .addParameter("WorkDateTo", CommonUtils.convertDateToStr(log.getEndTime(), "yyyy-MM-dd HH:mm"))
                .addParameter("p2", log.getLogCatalog().getSubcatalog())
                .addParameter("p3", "").build();
        String responseStr = "";
        try (CloseableHttpResponse response = httpclient.execute(accessRequest)) {
            responseStr = EntityUtils.toString(response.getEntity());
        }
        /**
         * 构建请求参数
         */
        List<NameValuePair> logInfo = new ArrayList<>();
        Document htmlDoc = Jsoup.parse(responseStr);
        logInfo.add(new BasicNameValuePair("__EVENTTARGET", ""));
        logInfo.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
        logInfo.add(new BasicNameValuePair("__LASTFOCUS", ""));
        String __VIEWSTATE = htmlDoc.select("#__VIEWSTATE").attr("value");
        logInfo.add(new BasicNameValuePair("__VIEWSTATE", __VIEWSTATE));
        String __EVENTVALIDATION = htmlDoc.select("#__EVENTVALIDATION").attr("value");
        logInfo.add(new BasicNameValuePair("__EVENTVALIDATION", __EVENTVALIDATION));
        logInfo.add(new BasicNameValuePair("hdWorkLogID", "-1"));
        logInfo.add(new BasicNameValuePair("hdDep", htmlDoc.select("#hdDep").attr("value")));
        logInfo.add(new BasicNameValuePair("hdChushi", htmlDoc.select("#hdChushi").attr("value")));
        logInfo.add(new BasicNameValuePair("hdWorkDtl", "Office"));
        logInfo.add(new BasicNameValuePair("hdStartTime", CommonUtils.convertDateToStr(log.getStartTime(), "yyyy-MM-dd HH:mm")));
        logInfo.add(new BasicNameValuePair("hdEndTime", CommonUtils.convertDateToStr(log.getEndTime(), "yyyy-MM-dd HH:mm")));
        logInfo.add(new BasicNameValuePair("hdNote", ""));
        logInfo.add(new BasicNameValuePair("ddlFL", log.getLogCatalog().getCatalog()));
        logInfo.add(new BasicNameValuePair("ddlJuTiShiXiang", log.getLogCatalog().getSubcatalog()));
        logInfo.add(new BasicNameValuePair("ctl12$hidDate", CommonUtils.convertDateToStr(log.getStartTime(), "yyyy-MM-dd")));
        logInfo.add(new BasicNameValuePair("ctl12$startTime", CommonUtils.convertDateToStr(log.getStartTime(), "HH:mm")));
        logInfo.add(new BasicNameValuePair("ctl12$endTime", CommonUtils.convertDateToStr(log.getEndTime(), "HH:mm")));
        logInfo.add(new BasicNameValuePair("ctl12$txtContent", log.getContent()));
        logInfo.add(new BasicNameValuePair("ctl12$btnSave", "保存"));
        /**
         * 创建日志
         */
        HttpUriRequest createRequest = RequestBuilder.post()
                .setUri(PropertiesUtils.getProperty("parser.clic.it.url.createlog"))
                .addParameter("fl", log.getLogCatalog().getCatalog())
                .addParameter("WorkDate", CommonUtils.convertDateToStr(log.getStartTime(), "yyyy-MM-dd HH:mm"))
                .addParameter("WorkDateTo", CommonUtils.convertDateToStr(log.getEndTime(), "yyyy-MM-dd HH:mm"))
                .addParameter("p2", log.getLogCatalog().getSubcatalog())
                .addParameter("p3", "")
                .setEntity(new UrlEncodedFormEntity(logInfo, Charset.defaultCharset())).build();
        try (CloseableHttpResponse response = httpclient.execute(createRequest)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new NullPointerException("创建日志失败！\n" + EntityUtils.toString(response.getEntity()));
            }
        }
    }
}
