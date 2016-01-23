package com.maeagle.utils;

import org.apache.http.util.Asserts;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by maeag_000 on 2016/1/18.
 */
public class PropertiesUtils {

    private static final String PROPERTIES_FILE = "config.properties";

    private static Properties properties = new Properties();

    static {
        InputStream is = null;
        try {
            Enumeration<URL> urls = PropertiesUtils.class.getClassLoader()
                    .getResources(PROPERTIES_FILE);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                URLConnection con = url.openConnection();
                con.setUseCaches(false);
                is = con.getInputStream();
                properties.load(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    public static String getProperty(String name) {
        Asserts.notNull(properties, "找不到 " + PROPERTIES_FILE + " 文件");
        return properties.getProperty(name);
    }

}
