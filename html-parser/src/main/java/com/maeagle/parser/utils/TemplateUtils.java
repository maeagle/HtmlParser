package com.maeagle.parser.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * Created by maeag_000 on 2016/1/18.
 */
public class TemplateUtils {

    private static Configuration config = new Configuration(Configuration.VERSION_2_3_23);

    static {
        try {
            config.setClassLoaderForTemplateLoading(TemplateUtils.class.getClassLoader(),"/ftls");
            config.setDefaultEncoding("UTF-8");
            config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Template getTemplate(String name) {
        try {
            return config.getTemplate(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
