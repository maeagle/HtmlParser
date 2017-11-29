package com.maeagle.parser.business.clic;

import com.halo.core.common.util.CommonUtils;
import com.halo.core.common.util.PropertiesUtils;
import com.maeagle.parser.models.clic.elearn.AccountInfo;
import com.maeagle.parser.models.clic.elearn.ClassInfo;
import com.maeagle.parser.models.clic.elearn.LessionInfo;
import com.maeagle.parser.utils.TemplateUtils;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElearnThread implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ElearnThread.class);

    private static Pattern RCO_ID_REG = Pattern.compile("launchContent\\(\"(\\d+)\",\"\\d+\"");

    private static Pattern CLASS_ID_REG = Pattern.compile("launchContent\\(\"\\d+\",\"(\\d+)\"");

    private static Pattern TBC_ID_REG = Pattern.compile("&tbc_id=(\\d+)&");

    private static Pattern USER_ID_REG = Pattern.compile("\"user_id\",\"(\\d+)\"");

    private static Pattern ENURL_REG = Pattern.compile("enurl=(.+)\";");

    private BasicCookieStore cookieStore = new BasicCookieStore();

    private CloseableHttpClient httpClient;

    private String sessionId;

    private AccountInfo user;

    private ClassInfo classInfo;

    private AtomicInteger counter;

    ElearnThread(AccountInfo user, ClassInfo classInfo, AtomicInteger counter) throws Exception {
        this.classInfo = classInfo;
        this.user = user;
        this.counter = counter;
        try {
            httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
            ElearnExecutor.login(httpClient, user.userName, user.password);
            this.sessionId = cookieStore.getCookies().stream()
                    .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElseThrow(NullPointerException::new);
        } catch (Exception e) {
            try {
                httpClient.close();
            } catch (Exception e1) {
            }
            throw e;
        }

    }

    @Override
    public void run() {
        logger.info("[{}]: 为账号 {} 学习 [{}] 课程...", sessionId, user.userName, classInfo.className);
        try {
            getAndSetValues(httpClient);
            getAndSetLessions(httpClient);
            study(httpClient);
            logger.info("[{}]: 账号 {} 完成了 [{}] 课程的学习。", sessionId, user.userName, classInfo.className);
        } catch (Exception e) {
            logger.error("[{}]: 账号 {} 学习 [{}] 课程失败！", sessionId, user.userName, classInfo.className);
            logger.error(e.getMessage(), e);
            return;
        } finally {
            counter.addAndGet(1);
            try {
                httpClient.close();
            } catch (Exception e) {
            }
        }
    }

    private void getAndSetValues(CloseableHttpClient httpclient) throws Exception {

        HttpUriRequest request1 = RequestBuilder.get().setUri(classInfo.accessUrl).build();
        String responseStr = "";
        try (CloseableHttpResponse response = httpclient.execute(request1)) {
            responseStr = EntityUtils.toString(response.getEntity());

            Matcher matcher = RCO_ID_REG.matcher(responseStr);
            if (matcher.find()) {
                classInfo.rco_id = matcher.group(1);
            } else {
                throw new Exception("无法获取rco_id信息。报文如下：\n" + responseStr);
            }

            matcher = TBC_ID_REG.matcher(responseStr);
            if (matcher.find()) {
                classInfo.tbc_id = matcher.group(1);
            } else {
                throw new Exception("无法获取tbc_id信息。报文如下：\n" + responseStr);
            }

            matcher = CLASS_ID_REG.matcher(responseStr);
            if (matcher.find()) {
                classInfo.class_id = matcher.group(1);
            } else {
                throw new Exception("无法获取class_id信息。报文如下：\n" + responseStr);
            }
        }
        String enurl = "";
        HttpUriRequest request2 = RequestBuilder.get()
                .setUri(String.format(PropertiesUtils.getProperty("parser.clic.elearn.url.redirect"),
                        classInfo.rco_id,
                        classInfo.icr_id,
                        classInfo.tbc_id,
                        classInfo.class_id))
                .build();
        try (CloseableHttpResponse response = httpclient.execute(request2)) {
            responseStr = EntityUtils.toString(response.getEntity());

            Matcher matcher = ENURL_REG.matcher(responseStr);
            if (matcher.find()) {
                enurl = matcher.group(1);
            } else {
                throw new Exception("无法获取enurl信息。报文如下：\n" + responseStr);
            }
        }
        HttpUriRequest request3 = RequestBuilder.get()
                .setUri(String.format(PropertiesUtils.getProperty("parser.clic.elearn.url.play"), enurl))
                .build();
        try (CloseableHttpResponse response = httpclient.execute(request3)) {
            responseStr = EntityUtils.toString(response.getEntity());
        }
    }

    private void getAndSetLessions(CloseableHttpClient httpclient) throws Exception {

        List<LessionInfo> lessions = new ArrayList<>();
        HttpUriRequest accessRequest = RequestBuilder.get().
                setUri(String.format(PropertiesUtils.getProperty("parser.clic.elearn.url.loadclass"), classInfo.rco_id))
                .build();
        String responseStr = "";
        try (CloseableHttpResponse response = httpclient.execute(accessRequest)) {
            responseStr = EntityUtils.toString(response.getEntity());
        }
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(IOUtils.toInputStream(responseStr, Charset.forName("UTF-8")));
        NodeList nodeList = document.getDocumentElement().getChildNodes();
        for (int j = 0; j < nodeList.getLength(); j++) {
            readFromXMl(nodeList.item(j), lessions);
        }
        classInfo.lessions = lessions;
    }

    void readFromXMl(Node lessionNode, List<LessionInfo> lessions) throws Exception {

        NodeList childList = lessionNode.getChildNodes();

        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeName().equals("item")) {
                for (int j = 0; j < childList.getLength(); j++) {
                    if (childList.item(j).getNodeName().equals("item")) {
                        readFromXMl(childList.item(j), lessions);
                    }
                }
                return;
            }
        }

        String iFileStr = null;
        String cdirStr = null;
        String urlStr = null;
        for (int i = 0; i < childList.getLength(); i++) {
            Node tempNode = childList.item(i);
            String nodeName = tempNode.getAttributes().getNamedItem("name").getNodeValue();
            if (nodeName.equals("url"))
                urlStr = tempNode.getTextContent();
            else if (nodeName.equals("cdir"))
                cdirStr = tempNode.getTextContent();
            else if (nodeName.equals("ifile"))
                iFileStr = tempNode.getTextContent();
        }
        LessionInfo lession = new LessionInfo();
        lession.lessionId = lessionNode.getAttributes().getNamedItem("id").getNodeValue();
        lession.lessionName = lessionNode.getAttributes().getNamedItem("text").getNodeValue();
        lession.accessUrl = String.format(PropertiesUtils.getProperty("parser.clic.elearn.url.loadlession"),
                cdirStr, iFileStr, URLEncoder.encode(URLEncoder.encode(urlStr.replace("/" + iFileStr, "")
                        , "UTF-8"), "UTF-8"));
        lessions.add(lession);
    }


    private void study(CloseableHttpClient httpclient) throws Exception {

        for (LessionInfo lession : classInfo.lessions) {

            String user_id = "";
            HttpUriRequest accessRequest = RequestBuilder.get().setUri(lession.accessUrl).build();
            String responseStr = "";
            try (CloseableHttpResponse response = httpclient.execute(accessRequest)) {
                responseStr = EntityUtils.toString(response.getEntity());
            }
            Matcher matcher = USER_ID_REG.matcher(responseStr);
            if (matcher.find()) {
                user_id = matcher.group(1);
            } else {
                throw new Exception("无法获取user_id信息。报文如下：\n" + responseStr);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("rco_id", classInfo.rco_id);
            data.put("scope_id", "582B0598C56FF462072D6300DC42C38F777");
            data.put("session_id", sessionId);
            data.put("lession_id", lession.lessionId);
            data.put("start_time", URLEncoder.encode(CommonUtils.convertDateToStr(new Date()
                    , CommonUtils.YYYY_MM_DD_HH_MM_SS), "UTF-8"));
            data.put("icr_id", classInfo.icr_id);
            data.put("tbc_id", classInfo.tbc_id);
            data.put("user_id", user_id);
            data.put("cmi_suspend_data", "");

            Template template = TemplateUtils.getTemplate("elearn_study.ftl");
            StringWriter sw = new StringWriter();
            template.process(data, sw);
            sw.flush();

            HttpUriRequest postRequest = RequestBuilder.post()
                    .setUri(PropertiesUtils.getProperty("parser.clic.elearn.url.study"))
                    .setEntity(EntityBuilder.create().setText(sw.getBuffer().toString())
                            .setContentType(ContentType.TEXT_PLAIN).build())
                    .build();

            try (CloseableHttpResponse response = httpclient.execute(postRequest)) {
                responseStr = EntityUtils.toString(response.getEntity());
                if (!responseStr.contains("success"))
                    throw new Exception("学习 [" + lession.lessionName + "] 课程失败。报文如下：\n" + responseStr);
            }
        }
    }
}
