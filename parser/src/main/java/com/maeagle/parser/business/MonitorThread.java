package com.maeagle.parser.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.halo.core.common.PropertiesUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

    private List<String> snapshot = new ArrayList<>();

    public MonitorThread(CloseableHttpClient httpclient, AtomicBoolean successFlag, String id) {
        this.httpclient = httpclient;
        this.successFlag = successFlag;
        this.id = id;

        initSnapshot();
    }


    @Override
    public void run() {
        logger.info("[{}]:启动个人帐户监控线程...", id);
        while (!successFlag.get()) {
            if (queryBookedList().stream().map(obj -> (JSONObject) obj).filter(jsonObject -> !snapshot.contains(jsonObject.getString("ReservationCode"))).count() > 0) {
                successFlag.set(true);
                logger.info("[{}]:成功预约医生[{}]的号!", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
            }
        }
    }

    private void initSnapshot() {
        snapshot = queryBookedList().stream().map(obj -> (JSONObject) obj).map(jsonObject -> jsonObject.getString("ReservationCode")).collect(Collectors.toList());

    }

    private JSONArray queryBookedList() {
        CloseableHttpResponse response = null;
        Document doc = null;
        try {
            HttpUriRequest listPage = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.bdgj.booked.url")).build();
            response = httpclient.execute(listPage);
            HttpEntity entity = response.getEntity();
            return JSON.parseArray(EntityUtils.toString(entity));
        } catch (Exception e) {
            logger.error("[" + id + "]: 无法访问已预约页面！", e);
            return JSON.parseArray("[]");
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
    }
}
