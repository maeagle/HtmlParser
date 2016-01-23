package com.maeagle.parser.business;

import com.maeagle.parser.BookOrderParser;
import com.maeagle.utils.PropertiesUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Book order thread.
 *
 * @author maeagle
 */
public class BookOrderThread implements Runnable {


    private Pattern pattern_GhPage = Pattern.compile(PropertiesUtils.getProperty("parser.bdgj.ghpage.pattern"));

    private Pattern pattern_ConfirmGhPage = Pattern.compile(PropertiesUtils.getProperty("parser.bdgj.ghconfirmpage.pattern"));

    /**
     * The Cookie store.
     */
    private BasicCookieStore cookieStore = new BasicCookieStore();

    /**
     * The Success flag.
     */
    private AtomicInteger successCount;

    /**
     * Instantiates a new Book order thread.
     *
     * @param successCount the success flag
     */
    public BookOrderThread(AtomicInteger successCount) {
        this.successCount = successCount;
    }

    @Override
    public void run() {

        if (successCount.get() >= BookOrderParser.maxCount)
            return;

        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();


        boolean loginSuccess = login(httpclient);

        if (loginSuccess) {
            execute(httpclient);
        }


        // 关闭httpclient
        try {
            httpclient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Login boolean.
     *
     * @param httpclient the httpclient
     * @return the boolean
     */
    private boolean login(CloseableHttpClient httpclient) {

        CloseableHttpResponse response = null;
        try {
            HttpUriRequest loginRequest = RequestBuilder.post().setUri(PropertiesUtils.getProperty("parser.bdgj.login.url"))
                    .addParameter("mobile", PropertiesUtils.getProperty("parser.bdgj.login.username"))
                    .addParameter("password", PropertiesUtils.getProperty("parser.bdgj.login.password")).build();
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
            return false;
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
        return true;
    }

    /**
     * 主逻辑执行.
     *
     * @param httpclient the httpclient
     */
    private void execute(CloseableHttpClient httpclient) {

        CloseableHttpResponse response = null;
        Document doc = null;
        try {
            HttpUriRequest listPage = RequestBuilder.get().setUri(PropertiesUtils.getProperty("parser.bdgj.booklist.url")).build();
            response = httpclient.execute(listPage);
            HttpEntity entity = response.getEntity();
            String bookList = EntityUtils.toString(entity);
            doc = Jsoup.parse(bookList);
            doc.body().getElementsByTag("tbody").get(0).getElementsByTag("tr")
                    .stream().filter(trElement -> findDoctorElement(trElement))
                    .map(trElement -> findGhPage(trElement))
                    .map(ghPage -> findGhConfirmPage(httpclient, ghPage))
                    .forEach(ghConfrmPage -> executeGhAction(httpclient, ghConfrmPage));

        } catch (Exception e) {
            System.out.println("＝＝＝＝＝＝＝＝＝执行失败！");
            e.printStackTrace();
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
        if (!html.contains("onclick"))
            return false;

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
     * @param httpclient the httpclient
     * @param ghPage     the gh page
     * @return the string
     */
    private String findGhConfirmPage(CloseableHttpClient httpclient, String ghPage) {
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
            System.out.println("＝＝＝＝＝＝＝＝＝进入挂号页面失败！");
            e.printStackTrace();
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
     * @param httpclient    the httpclient
     * @param ghConfirmPage the gh confirm page
     */
    private void executeGhAction(CloseableHttpClient httpclient, String ghConfirmPage) {
        CloseableHttpResponse response = null;
        // 进行实际预约挂号
        if (ghConfirmPage == null)
            return;

        try {
            HttpUriRequest ghConfirmPageReq = RequestBuilder.get().setUri(new URI(PropertiesUtils.getProperty("parser.bdgj.root.url") + ghConfirmPage)).build();
            response = httpclient.execute(ghConfirmPageReq);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            System.out.println(result);
            if (result.indexOf("预约成功") > -1) {
                successCount.incrementAndGet();
                System.out.println(ghConfirmPage + " : \n" + result);
            } else {
                //System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！\n"+result);
            }
        } catch (Exception e) {
            System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！");
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
    }

}
