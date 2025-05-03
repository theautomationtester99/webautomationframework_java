package com.waf.report;

import lombok.Data;

@Data
public class SubStep {
    private String subStep;
    private String subStepMessage;
    private String imageSrc;
    private String imageAlt;
    private String subStepStatus;
}