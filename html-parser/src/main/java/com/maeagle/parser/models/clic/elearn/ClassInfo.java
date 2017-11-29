package com.maeagle.parser.models.clic.elearn;

import java.util.List;
import java.util.Map;

public class ClassInfo {

    public String className;

    public String accessUrl;

    public String rco_id;

    public String icr_id;

    public String tbc_id;

    public String class_id;

    public List<LessionInfo> lessions;

    @Override
    public String toString() {
        return "ClassInfo{" +
                "className='" + className + '\'' +
                ", accessUrl='" + accessUrl + '\'' +
                ", rco_id='" + rco_id + '\'' +
                ", lessions=" + lessions +
                '}';
    }
}
