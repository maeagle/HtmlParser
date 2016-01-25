package com.maeagle.parser.business;

import com.maeagle.utils.PropertiesUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maeagle on 16/1/25.
 */
public class MonitorThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MonitorThread.class);

    /**
     * The Success flag.
     */
    private AtomicBoolean successFlag;


    private CloseableHttpClient httpclient;

    private String id;

    private Pattern pattern_cancelYuyue = Pattern.compile(PropertiesUtils.getProperty("parser.bdgj.cancelgh.pattern"));

    private List<String> snapshots = new ArrayList<>();

    public MonitorThread(CloseableHttpClient httpclient, AtomicBoolean successFlag, String id) {
        this.httpclient = httpclient;
        this.successFlag = successFlag;
        this.id = id;

        initSnapshots();
    }


    @Override
    public void run() {
        while (!successFlag.get()) {
            String returnStr = processBookListPage();
            Matcher matcher = pattern_cancelYuyue.matcher(returnStr);
            while (matcher.find()) {
                String str = matcher.group();
                if (!snapshots.contains(str)) {
                    successFlag.set(true);
                    logger.info("[" + id + "]: 预约成功！");
                    return;
                }
            }
        }
    }

    private void initSnapshots() {
        String returnStr = processBookListPage();
        Matcher matcher = pattern_cancelYuyue.matcher(returnStr);
        while (matcher.find()) {
            snapshots.add(matcher.group());
        }
    }

    private String processBookListPage() {
        CloseableHttpResponse response = null;
        Document doc = null;
        try {
            HttpUriRequest listPage = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.bdgj.mybooklist.url")).build();
            response = httpclient.execute(listPage);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            logger.error("[" + id + "]: 无法访问已预约页面！", e);
            return "";
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
    }
}
