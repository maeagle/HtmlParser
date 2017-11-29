package com.maeagle.parser.models.clic.elearn;

import java.util.Map;

public class LessionInfo {

    public String lessionId;

    public String lessionName;

    public String accessUrl;

    @Override
    public String toString() {
        return "LessionInfo{" +
                "lessionId='" + lessionId + '\'' +
                ", lessionName='" + lessionName + '\'' +
                ", accessUrl='" + accessUrl + '\'' +
                '}';
    }
}
