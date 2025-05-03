package com.waf.report;

import lombok.Data;
import java.util.Map;

@Data
public class RetryData {
    private String rstatus;
    private String rerror;
    private Map<String, TestStep> steps;
}