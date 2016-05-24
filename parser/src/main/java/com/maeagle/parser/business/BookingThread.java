package com.maeagle.parser.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.halo.core.common.PropertiesUtils;
import com.maeagle.parser.utils.EncodingUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Book order thread.
 *
 * @author maeagle
 */
public class BookingThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BookingThread.class);

    /**
     * The Success flag.
     */
    private AtomicBoolean successFlag;

    private CloseableHttpClient httpclient;

    private String id;

    /**
     * Instantiates a new Book order thread.
     *
     * @param successFlag the success flag
     */
    public BookingThread(CloseableHttpClient httpclient, AtomicBoolean successFlag, String id) {
        this.httpclient = httpclient;
        this.successFlag = successFlag;
        this.id = id;
    }

    @Override
    public void run() {

        if (successFlag.get())
            return;

        CloseableHttpResponse response = null;
        try {
            logger.info("[{}]:开始寻找医生[{}]的预约号...", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
            while (!successFlag.get()) {
                HttpUriRequest listPage = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.bdgj.availd-book.url")).build();
                response = httpclient.execute(listPage);
                HttpEntity entity = response.getEntity();
                String jsonStr = EntityUtils.toString(entity);
                try {
                    JSON.parseArray(jsonStr).stream().map(obj -> (JSONObject) obj).filter(jsonObject ->
                            PropertiesUtils.getProperty("parser.bdgj.doctor.code").equals(jsonObject.get("DoctorCode"))
                                    && Integer.parseInt(ObjectUtils.defaultIfNull(jsonObject.get("AvailableNumber"), "0").toString()) > 0)
                            .forEach(jsonObject -> {
                                logger.info("[{}]:找到医生[{}]的预约号!开始挂号...", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
                                bookingAction(jsonObject);
                            });
                } catch (Exception e1) {
                    logger.info(jsonStr);
                }
            }
        } catch (Exception e) {
            logger.error("[" + id + "]: 执行失败！", e);
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 执行挂号逻辑
     *
     * @param jsonObject the gh confirm page
     */
    private void bookingAction(JSONObject jsonObject) {
        CloseableHttpResponse response = null;

        // 进行实际预约挂号
        if (jsonObject == null)
            return;
        JSONObject requestJson = (JSONObject) jsonObject.clone();
        requestJson.put("arrivedPay", "true");
        requestJson.put("selectedUser", "self");
        try {
            StringEntity stringEntity = new StringEntity(EncodingUtils.native2Ascii(requestJson.toJSONString()));
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            HttpUriRequest request = RequestBuilder.post().setUri(PropertiesUtils.getProperty("parser.bdgj.booking.url")).setEntity(stringEntity).build();
            int avaliableNum = Integer.parseInt(ObjectUtils.defaultIfNull(jsonObject.get("AvailableNumber"), "0").toString());
            int count = 1;
            while (!successFlag.get() && count <= avaliableNum) {
                logger.info("[{}]:尝试第{}次...", id, count++);
                response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                try {
                    response.close();
                } catch (Exception e) {
                }
                if (result.indexOf("orderId") > -1) {
                    successFlag.set(true);
                    logger.info("[{}]:成功预约医生[{}]的号!", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
                    break;
                } else {
                    logger.info("[{}]:失败报文 : {}", id, result);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
