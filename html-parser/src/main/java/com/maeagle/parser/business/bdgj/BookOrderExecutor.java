package com.maeagle.parser.business.bdgj;

import com.alibaba.fastjson.JSON;
import com.halo.core.common.util.PropertiesUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by maeagle on 16/1/25.
 */
public class BookOrderExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BookOrderExecutor.class);

    private String id;

    private String userName;

    private String password;

    private BasicCookieStore cookieStore = new BasicCookieStore();

    private AtomicBoolean successFlag = new AtomicBoolean(false);

    public BookOrderExecutor(String userName, String password) {
        this.userName = userName;
        this.password = password;
        id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    @Override
    public void run() {

        logger.info("[{}]: 开始执行... 用户名: {}  密码: {}", id, userName, password);

        CloseableHttpClient httpclient = null;

        try {
            cookieStore.clear();
            httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
            boolean loginSuccess = login(httpclient);
            if (loginSuccess) {


                Thread monitorThread = new Thread(new MonitorThread(httpclient, successFlag, id));
                monitorThread.start();

                // 每个帐户执行的线程数量
                int threadCount = Integer.parseInt(PropertiesUtils.getProperty("parser.bdgj.pre_account.thread.count"));
                for (int i = 0; i < threadCount; i++) {
                    Thread subThread = new Thread(new BookingThread(httpclient, successFlag, id));
                    subThread.start();
                }
                while (true) ;
            } else {
                logger.error("登陆失败!");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 登录请求.
     *
     * @param httpclient the httpclient
     * @return the boolean
     */
    private boolean login(CloseableHttpClient httpclient) {

        Map<String, String> accountInfo = new HashMap<>();
        accountInfo.put("account", userName);
        accountInfo.put("password", password);
        String accountStr = JSON.toJSONString(accountInfo);
        CloseableHttpResponse response = null;
        try {
            StringEntity stringEntity = new StringEntity(accountStr);
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            HttpUriRequest loginRequest = RequestBuilder.post().setUri(PropertiesUtils.getProperty("parser.bdgj.login.url"))
                    .setEntity(stringEntity).build();
            response = httpclient.execute(loginRequest);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            List<Cookie> cookies = cookieStore.getCookies();
            if (cookies.isEmpty()) {
                throw new NullPointerException();
            } else {
                logger.info("＝＝＝＝＝＝＝＝＝登陆成功！");
            }
        } catch (Exception e) {
            logger.error("＝＝＝＝＝＝＝＝＝登陆失败！",e);
            return false;
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
        return true;
    }
}
