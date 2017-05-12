package com.maeagle.parser.models.clic;

import java.util.List;

/**
 * Created by maeagle on 2017/5/11.
 */
public class AccountInfo {

    private String userName;

    private String password;

    private LogCatalog logCatalog;

    private List<String> logContentList;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LogCatalog getLogCatalog() {
        return logCatalog;
    }

    public void setLogCatalog(LogCatalog logCatalog) {
        this.logCatalog = logCatalog;
    }

    public List<String> getLogContentList() {
        return logContentList;
    }

    public void setLogContentList(List<String> logContentList) {
        this.logContentList = logContentList;
    }
}
