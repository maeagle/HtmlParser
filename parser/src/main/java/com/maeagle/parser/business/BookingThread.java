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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The type Book order thread.
 *
 * @author maeagle
 */
public class BookingThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BookingThread.class);

    private Pattern pattern_GhPage = Pattern.compile(PropertiesUtils.getProperty("parser.bdgj.ghpage.pattern"));

    private Pattern pattern_ConfirmGhPage = Pattern.compile(PropertiesUtils.getProperty("parser.bdgj.ghconfirmpage.pattern"));

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
        Document doc = null;
        try {
            boolean findDoctorFlag = false;
            Stream<Element> bizStream = null;
            Elements bizData = null;
            logger.info("[{}]:开始寻找医生[{}]的预约号...", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
            while (!findDoctorFlag) {
                HttpUriRequest listPage = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.bdgj.booklist.url")).build();
                response = httpclient.execute(listPage);
                HttpEntity entity = response.getEntity();
                String bookList = EntityUtils.toString(entity);
                bizData = Jsoup.parse(bookList).body().getElementsByTag("tbody").get(0).getElementsByTag("tr");
                findDoctorFlag = bizData.stream().anyMatch(this::findDoctorElement);
            }
            logger.info("[{}]:找到医生[{}]的预约号!开始挂号...", id, PropertiesUtils.getProperty("parser.bdgj.doctor.name"));
            bizData.stream().filter(this::findDoctorElement)
                    .map(this::findGhPage)
                    .map(this::findGhConfirmPage)
                    .forEach(this::executeGhAction);

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
     * 找到医生信息节点.
     *
     * @param trElement the tr element
     * @return the boolean
     */
    private boolean findDoctorElement(Element trElement) {

        String html = trElement.html();
        // 不存在这个医生
        if (!html.contains(PropertiesUtils.getProperty("parser.bdgj.doctor.name"))) {
            return false;
        }
        // 存在这个医生, 但是不能点击
        if (!html.contains("onclick")) {
            return false;
        }

        return true;
    }

    /**
     * 找到挂号页面地址.
     *
     * @param trElement the tr element
     * @return the string
     */
    private String findGhPage(Element trElement) {
        Matcher matcher = pattern_GhPage.matcher(trElement.html());

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 找到挂号确认页面地址.
     *
     * @param ghPage the gh page
     * @return the string
     */
    private String findGhConfirmPage(String ghPage) {
        CloseableHttpResponse response = null;
        String ghPageStr = null;
        if (ghPage == null)
            return null;
        try {
            HttpUriRequest ghPageReq = RequestBuilder.get().setUri(new URI(PropertiesUtils.getProperty("parser.bdgj.root.url") + ghPage)).build();
            response = httpclient.execute(ghPageReq);
            HttpEntity entity = response.getEntity();
            ghPageStr = EntityUtils.toString(entity);
            Matcher matcherCon = pattern_ConfirmGhPage.matcher(ghPageStr);
            if (matcherCon.find()) {
                return matcherCon.group(1);
            }
            return null;
        } catch (Exception e) {
            logger.error("[" + id + "]: 进入挂号页面失败！", e);
            return null;
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
     * @param ghConfirmPage the gh confirm page
     */
    private void executeGhAction(String ghConfirmPage) {
        CloseableHttpResponse response = null;
        // 进行实际预约挂号
        if (ghConfirmPage == null)
            return;

        try {
            HttpUriRequest ghConfirmPageReq = RequestBuilder.get().setUri(new URI(PropertiesUtils.getProperty("parser.bdgj.root.url") + ghConfirmPage)).build();

            int count = 1;
            while (!successFlag.get()) {
                logger.info("[{}]:尝试第{}次...", id, count++);
                response = httpclient.execute(ghConfirmPageReq);
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                try {
                    response.close();
                } catch (Exception e) {
                }
                if (result.indexOf("预约成功") > -1) {
                    successFlag.set(true);
                    break;
                }
            }
        } catch (Exception e) {
        }
    }

}
