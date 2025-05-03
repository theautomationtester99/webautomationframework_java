package com.waf.report;

import lombok.Data;
import java.util.Map;

@Data
public class TestReport {
    private String pageTitle;
    private String testDescription;
    private String browserImgSrc;
    private String osImgSrc;
    private String osImgAlt;
    private String gridImgSrc;
    private String gridImgAlt;
    private String browserImgAlt;
    private String browserVersion;
    private String executedDate;
    private String overallStatusText;
    private Map<String, RetryData> tableData;
}