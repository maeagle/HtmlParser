package com.maeagle.htmlparser.bdgj;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class BookOrderThread implements Runnable {

    private static String loginUrl = "http://59.108.39.19:81/api/wechatGh/login.do";

//    private static String listDocUrl = "http://59.108.39.19:81/api/wechatGh/outDoctorList.do?noType=2&office_id=0000395&officeName=妇科门诊";

    private static String listDocUrl = "http://59.108.39.19:81/api/wechatGh/outDoctorList.do?noType=2&office_id=0000555&officeName=儿童血液中心门诊";

//    private static String listDocUrl = "http://59.108.39.19:81/api/wechatGh/outDoctorList.do?noType=2&office_id=0000418&officeName=产科门诊";

    private static String ghPageUrl = "http://59.108.39.19:81/api/wechatGh/";

    private static Pattern pattern_GhPage = Pattern.compile("gh\\('(actualGhPage\\.do\\?id=\\d+)'\\)");

    private static Pattern pattern_ConfirmGhPage = Pattern.compile("(actualGh\\.do\\?id=\\d+)");

    public static String userName = "15652993327";

    public static String password = "15901041041";

    private BasicCookieStore cookieStore = new BasicCookieStore();

    private AtomicBoolean successFlag;

    public BookOrderThread(AtomicBoolean successFlag) {
        this.successFlag = successFlag;
    }

    @Override
    public void run() {

        if (successFlag.get())
            return;

        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        CloseableHttpResponse response = null;

        // 登陆
        try {
            HttpUriRequest loginRequest = RequestBuilder.post().setUri(loginUrl).addParameter("mobile", userName)
                    .addParameter("password", password).build();
            response = httpclient.execute(loginRequest);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            List<Cookie> cookies = cookieStore.getCookies();
            if (cookies.isEmpty()) {
                throw new NullPointerException();
            } else {
                // System.out.println("Login Cookies : ");
                // for (int i = 0; i < cookies.size(); i++)
                // System.out.println("- " + cookies.get(i).toString());
                // System.out.println("＝＝＝＝＝＝＝＝＝登陆成功！");
            }
        } catch (Exception e) {
            System.out.println("＝＝＝＝＝＝＝＝＝登陆失败！");
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
            response = null;
        }

        String listPageStr = null;

        // 读取出诊医生列表
        try {
            HttpUriRequest listPage = RequestBuilder.get().setUri(listDocUrl).build();
            response = httpclient.execute(listPage);
            HttpEntity entity = response.getEntity();
            listPageStr = EntityUtils.toString(entity);
        } catch (Exception e) {
            System.out.println("＝＝＝＝＝＝＝＝＝读取出诊医生列表失败！");
            e.printStackTrace();
            listPageStr = null;
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
            response = null;
        }

        // 循环进入挂号确认页面
        if (listPageStr != null) {
            Matcher matcher = pattern_GhPage.matcher(listPageStr);
            while (matcher.find()) {
                // 进入预约挂号页面
                String ghPage = matcher.group(1);
                String ghPageStr = null;
                try {
                    HttpUriRequest ghPageReq = RequestBuilder.get().setUri(new URI(ghPageUrl + ghPage)).build();
                    response = httpclient.execute(ghPageReq);
                    HttpEntity entity = response.getEntity();
                    ghPageStr = EntityUtils.toString(entity);
                } catch (Exception e) {
                    System.out.println("＝＝＝＝＝＝＝＝＝进入挂号页面失败！");
                    e.printStackTrace();
                    ghPageStr = null;
                } finally {
                    try {
                        response.close();
                    } catch (Exception e) {
                    }
                    response = null;
                }

                // 进行实际预约挂号
                if (ghPageStr != null) {
                    try {
                        Matcher matcherCon = pattern_ConfirmGhPage.matcher(ghPageStr);
                        if (matcherCon.find()) {
                            String confirmGhPage = matcherCon.group(1);
                            HttpUriRequest ghConfirmPageReq = RequestBuilder.get().setUri(new URI(ghPageUrl + confirmGhPage)).build();
                            response = httpclient.execute(ghConfirmPageReq);
                            HttpEntity entity = response.getEntity();
                            String result = EntityUtils.toString(entity);
                            if (result.indexOf("预约成功") > -1) {
                                successFlag.set(true);
                                System.out.println(ghPage + " : \n" + result);
                            } else {
                                // System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！\n"+result);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！");
                        e.printStackTrace();
                    } finally {
                        try {
                            response.close();
                        } catch (Exception e) {
                        }
                        response = null;
                    }
                }
            }
        }


        // 关闭httpclient
        try {
            httpclient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
