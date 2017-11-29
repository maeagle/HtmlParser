package com.maeagle.parser.business.clic;

import com.halo.core.common.util.PropertiesUtils;
import com.maeagle.parser.models.clic.elearn.AccountInfo;
import com.maeagle.parser.models.clic.elearn.ClassInfo;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElearnExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ElearnExecutor.class);

    private static Pattern pattern = Pattern.compile("\\d+");

    private static Pattern icr_id_pattern = Pattern.compile("\\?icrId=(\\d+)");

    private String sessionId;

    private CloseableHttpClient httpClient;

    private AccountInfo user;

    private int topPageCount = 1000;

    private BasicCookieStore cookieStore;

    public ElearnExecutor(CloseableHttpClient httpClient, BasicCookieStore cookieStore, AccountInfo user,
                          int topPageCount) {
        this.httpClient = httpClient;
        this.cookieStore = cookieStore;
        this.user = user;
        this.topPageCount = topPageCount;
    }

    @Override
    public void run() {
        try {
            logger.info("[]:登录账号：{}", user.userName);
            login(httpClient, user.userName, user.password);
            sessionId = cookieStore.getCookies().stream()
                    .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElseThrow(NullPointerException::new);
        } catch (Exception e) {
            logger.error("[" + sessionId + "]: 登录账号：" + user.userName + " 失败！");
            logger.error(e.getMessage(), e);
            return;
        }
        try {
            List<ClassInfo> classList = getClassListToDo(httpClient);
            AtomicInteger counter = new AtomicInteger(0);
            for (ClassInfo classInfo : classList) {
                new ElearnThread(user, classInfo, counter).run();
            }
            while (counter.get() < classList.size()) ;
        } catch (Exception e) {
            logger.error("[" + sessionId + "]: 为账号：" + user.userName + " 的学习课程失败！");
            logger.error(e.getMessage(), e);
            return;
        }
    }


    private List<ClassInfo> getClassListToDo(CloseableHttpClient httpclient) throws Exception {

        List<ClassInfo> classList = new ArrayList<>();

        int currentPageNo = 1;
        int nextPageNo = 1;
        int maxPageNo = 1;
        while (nextPageNo <= maxPageNo && nextPageNo <= topPageCount) {
            List<NameValuePair> queryInfo = new ArrayList<>();
            queryInfo.add(new BasicNameValuePair("view_type", "P"));
            queryInfo.add(new BasicNameValuePair("tp_id", ""));
            queryInfo.add(new BasicNameValuePair("order_by", "default"));
            queryInfo.add(new BasicNameValuePair("return_page", "en/cst_learner2/jsp/my_train_course.jsp"));
            queryInfo.add(new BasicNameValuePair("grade_icr_id", ""));
            queryInfo.add(new BasicNameValuePair("endScore", ""));
            queryInfo.add(new BasicNameValuePair("grade", "N"));
            queryInfo.add(new BasicNameValuePair("tbc_id", ""));
            queryInfo.add(new BasicNameValuePair("courseName", ""));
            queryInfo.add(new BasicNameValuePair("start_date", ""));
            queryInfo.add(new BasicNameValuePair("end_date", ""));
            queryInfo.add(new BasicNameValuePair("pagenum", String.valueOf(currentPageNo)));
            queryInfo.add(new BasicNameValuePair("info", ""));

            HttpUriRequest loginRequest = RequestBuilder.post()
                    .setUri(String.format(PropertiesUtils.getProperty("parser.clic.elearn.url.myclass"), String.valueOf(nextPageNo)))
                    .setEntity(new UrlEncodedFormEntity(queryInfo, Charset.defaultCharset())).build();
            try (CloseableHttpResponse response = httpclient.execute(loginRequest)) {
                String result = EntityUtils.toString(response.getEntity());
                Document htmlDoc = Jsoup.parse(result);
                Elements elements = htmlDoc.select("tr.tab_tr");
                for (Element classInfoEle : elements) {
                    String status = classInfoEle.select("td.homegrade:contains(完成)").text();
                    if (status.contains("已完成"))
                        continue;
                    ClassInfo classInfo = new ClassInfo();
                    classInfo.className = classInfoEle.select("td.coursename").text();
                    classInfo.accessUrl = classInfoEle.select("td.tp_start").select("a").attr("href");
                    Matcher matcher = icr_id_pattern.matcher(classInfo.accessUrl);
                    matcher.find();
                    classInfo.icr_id = matcher.group(1);
                    classList.add(classInfo);
                }
                String footerString = htmlDoc.select("td:has(b)").select("b").text();
                Matcher matcher = pattern.matcher(footerString);
                matcher.find();
                matcher.find();
                maxPageNo = Integer.parseInt(matcher.group());
            }
            currentPageNo = nextPageNo;
            nextPageNo++;
        }
        return classList;
    }

    /**
     * 登录请求.
     *
     * @param httpclient the httpclient
     * @return the boolean
     */
    public static void login(CloseableHttpClient httpclient, String userName, String password) throws Exception {

        /**
         * 初始化cookies
         */
        HttpUriRequest accessRequest = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.clic.elearn.url.protal")).build();
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
        accountInfo.add(new BasicNameValuePair("action1", "1200"));
        accountInfo.add(new BasicNameValuePair("srcreq", "1001"));
        accountInfo.add(new BasicNameValuePair("site", "chinalife"));
        accountInfo.add(new BasicNameValuePair("username", userName));
        accountInfo.add(new BasicNameValuePair("tppd", password));
        accountInfo.add(new BasicNameValuePair("tb_site_id", htmlDoc.select("#tb_site_id").attr("value")));
        accountInfo.add(new BasicNameValuePair("username1", ""));
        accountInfo.add(new BasicNameValuePair("tppd1", ""));

        /**
         * 执行登录
         */
        HttpUriRequest loginRequest = RequestBuilder.post()
                .setUri(PropertiesUtils.getProperty("parser.clic.elearn.url.login"))
                .setEntity(new UrlEncodedFormEntity(accountInfo, Charset.defaultCharset())).build();
        try (CloseableHttpResponse response = httpclient.execute(loginRequest)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 302) {
                throw new NullPointerException("登录失败！\n" + EntityUtils.toString(response.getEntity()));
            }
            String result = EntityUtils.toString(response.getEntity());
            if (result.contains("msg=pwderr")) {
                throw new NullPointerException("登录失败！\n" + result);
            }
        }
    }

}
