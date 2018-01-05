package com.maeagle.parser.models.clic.itlog;

import java.util.Date;

/**
 * Created by maeagle on 2017/5/11.
 */
public class LogInfo {

    private String content;

    private Date startTime;

    private Date endTime;

    private LogCatalog logCatalog;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public LogCatalog getLogCatalog() {
        return logCatalog;
    }

    public void setLogCatalog(LogCatalog logCatalog) {
        this.logCatalog = logCatalog;
    }
}
