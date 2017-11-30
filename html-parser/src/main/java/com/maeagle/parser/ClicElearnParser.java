package com.maeagle.parser;

import com.halo.core.common.util.PropertiesUtils;
import com.maeagle.parser.business.clic.ElearnExecutor;
import com.maeagle.parser.models.clic.elearn.AccountInfo;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClicElearnParser {

    private static Logger logger = LoggerFactory.getLogger(ClicElearnParser.class);

    public static void main(String[] args) throws Exception {

        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = null;

        try {
            cookieStore.clear();
            httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();

            AccountInfo account = new AccountInfo();
            account.userName = PropertiesUtils.getProperty("parser.clic.elearn.account.username");
            account.password = PropertiesUtils.getProperty("parser.clic.elearn.account.password");

            ElearnExecutor executor = new ElearnExecutor(httpclient, cookieStore, account);
            executor.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
            }
        }
    }

}
