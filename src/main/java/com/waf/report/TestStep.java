package com.waf.report;

import lombok.Data;
import java.util.Map;

@Data
public class TestStep {
    private String sno;
    private String rowspan;
    private String step;
    private String result;
    private String overallStepStatus;
    private Map<String, SubStep> subSteps;
}