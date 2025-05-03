package com.waf;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waf.report.RetryData;
import com.waf.report.SubStep;
import com.waf.report.TestReport;
import com.waf.report.TestStep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.xlsx.XlsxReadOptions;

public class PdfReportManager {
    // LoggerConfig instance for logging
    private Logger logger;
    private Utils utils;

    protected String tcId;
    private List<Object> allStepsList;
    private int stepNo;
    private int subStepNo;
    private int rowSpan;
    private TestReport reportData;
    private Map<String, RetryData> tableData;
    protected int currentRetry;

    protected String pageTitle;
    protected String testDescription;
    protected String browserImgSrc;
    protected String browserImgAlt;
    protected String osImgSrc;
    protected String osImgAlt;
    protected String runningOnHostName;
    protected String browserVersion;
    protected String gridImgSrc;
    protected String gridImgAlt;
    protected String executedDate;
    protected String overallStatusText;

    public PdfReportManager(Logger logger) {
        this.logger = logger;
        this.utils = Utils.getInstance(logger);
        this.tcId = "";
        this.allStepsList = new ArrayList<>();
        this.stepNo = 0;
        this.subStepNo = 0;
        this.rowSpan = 0;
        this.reportData = new TestReport();
        this.tableData = new HashMap<>();
        this.currentRetry = 1;
        this.pageTitle = "";
        this.testDescription = "";
        this.browserImgSrc = "";
        this.browserImgAlt = "";
        this.osImgSrc = "";
        this.osImgAlt = "";
        this.runningOnHostName = "";
        this.browserVersion = "";
        this.gridImgSrc = "";
        this.gridImgAlt = "";
        this.executedDate = utils.getDateString();
        this.overallStatusText = "PASSED";
    }

    public void addReportData(Map<String, Object> data) {
        String retryKey = "retry_" + currentRetry;
        RetryData retryData = null;

        if (!tableData.containsKey(retryKey)) {
            retryData = new RetryData();
            retryData.setRstatus("Pass");
            tableData.put(retryKey, retryData);
            stepNo = 0;
            overallStatusText = "PASSED";

            TestStep testStep = null;
            Map<String, TestStep> retryStep = null;
            Map<String, SubStep> allSubSteps = null;

            if (data.containsKey("step")) {
                testStep = new TestStep();
                retryStep = new HashMap<>();
                logger.info("Step is captured to be added to PDF report.");
                stepNo++;
                rowSpan = 1;

                testStep.setSno(String.valueOf(stepNo));
                testStep.setRowspan(String.valueOf(rowSpan));
                testStep.setStep(data.get("step").toString());
                testStep.setResult(data.get("result").toString());
                testStep.setOverallStepStatus("Pass");

                retryStep.put(String.valueOf(stepNo), testStep);
                retryData.setSteps(retryStep);
                tableData.put(retryKey, retryData);
            } else {
                testStep = tableData.get(retryKey).getSteps().get(String.valueOf(stepNo));
                SubStep subStep = new SubStep();
                if (testStep.getSubSteps() != null && !testStep.getSubSteps().isEmpty()) {
                    allSubSteps = new HashMap<>();
                } else {
                    allSubSteps = testStep.getSubSteps();
                }
                logger.info("Sub Step is captured to be added to PDF report.");
                subStepNo++;
                rowSpan++;

                subStep.setSubStep(data.get("subStep").toString());
                subStep.setSubStepMessage(data.get("subStepMessage").toString());
                subStep.setSubStepStatus(data.get("subStepStatus").toString());

                if (data.containsKey("imageSrc")) {
                    subStep.setImageSrc(data.get("imageSrc").toString());
                    subStep.setImageAlt(data.get("imageAlt").toString());
                }
                allSubSteps.put(String.valueOf(subStepNo), subStep);
                testStep.setRowspan(String.valueOf(rowSpan));

                if (data.get("subStepStatus").toString().equalsIgnoreCase("Fail")) {
                    overallStatusText = "FAILED";
                    retryData.setRstatus("Fail");
                }
            }
        } else {
            retryData = tableData.get(retryKey);
            stepNo = 0;

            TestStep testStep = null;
            Map<String, TestStep> retryStep = null;
            Map<String, SubStep> allSubSteps = null;

            if (data.containsKey("step")) {
                testStep = new TestStep();
                retryStep = new HashMap<>();
                logger.info("Step is captured to be added to PDF report.");
                stepNo++;
                rowSpan = 1;

                testStep.setSno(String.valueOf(stepNo));
                testStep.setRowspan(String.valueOf(rowSpan));
                testStep.setStep(data.get("step").toString());
                testStep.setResult(data.get("result").toString());
                testStep.setOverallStepStatus("Pass");

                retryStep.put(String.valueOf(stepNo), testStep);
                retryData.setSteps(retryStep);
                tableData.put(retryKey, retryData);
            } else {
                testStep = tableData.get(retryKey).getSteps().get(String.valueOf(stepNo));
                SubStep subStep = new SubStep();
                if (testStep.getSubSteps() != null && !testStep.getSubSteps().isEmpty()) {
                    allSubSteps = new HashMap<>();
                } else {
                    allSubSteps = testStep.getSubSteps();
                }
                logger.info("Sub Step is captured to be added to PDF report.");
                subStepNo++;
                rowSpan++;

                subStep.setSubStep(data.get("subStep").toString());
                subStep.setSubStepMessage(data.get("subStepMessage").toString());
                subStep.setSubStepStatus(data.get("subStepStatus").toString());

                if (data.containsKey("imageSrc")) {
                    subStep.setImageSrc(data.get("imageSrc").toString());
                    subStep.setImageAlt(data.get("imageAlt").toString());
                }
                allSubSteps.put(String.valueOf(subStepNo), subStep);
                testStep.setRowspan(String.valueOf(rowSpan));

                if (data.get("subStepStatus").toString().equalsIgnoreCase("Fail")) {
                    overallStatusText = "FAILED";
                    retryData.setRstatus("Fail");
                }
            }
        }

    }

    public void createReport() {
        logger.info("Finalizing the data to be added to PDF report.");

        reportData.setPageTitle(pageTitle);
        reportData.setTestDescription(testDescription);
        reportData.setBrowserImgSrc(browserImgSrc);
        reportData.setOsImgSrc(osImgSrc);
        reportData.setOsImgAlt(osImgAlt);
        reportData.setBrowserImgAlt(browserImgAlt);
        reportData.setBrowserVersion(browserVersion);
        reportData.setExecutedDate(executedDate);
        reportData.setOverallStatusText(overallStatusText);
        reportData.setTableData(tableData);

        if (!gridImgSrc.isEmpty()) {
            reportData.setGridImgSrc(gridImgSrc);
            reportData.setGridImgAlt(gridImgAlt);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> testReportMap = objectMapper.convertValue(reportData, Map.class);

        String logoPath = "resources/logo.png";
        String templatePath = "resources/encrypted_file.jinja2";

        String testId = tcId + "-" + runningOnHostName + "-" + browserImgAlt + "-" + osImgAlt;
        String fileName = tcId + "_" + browserImgAlt + "_" + overallStatusText + "_" + utils.getDateString();

        PdfReporting pdf = new PdfReporting(this.logger, logoPath, templatePath, testReportMap, testId, fileName);

        pdf.generatePdf();
    }

    public void generateTestSummaryPdf() {
        logger.info("Checking if output.xlsx file exists before creating the test summary PDF report.");
        Path baseDir = Paths.get(System.getProperty("user.dir"));
        String trFolder = utils.getTestResultsFolder();
        Path summaryResultsFile = Paths.get(trFolder, "output.xlsx");

        if (utils.checkIfFileExists(summaryResultsFile.toString())) {
            logger.info("Output.xlsx exists and starting to create test summary PDF report.");

            try {
                Table table = Table.read().usingOptions(XlsxReadOptions.builder(summaryResultsFile.toFile()).build());
                Map<String, Object> tableData = new HashMap<>();

                List<String> columnNames = table.columnNames(); // Get all column headers

                List<Map<String, Object>> rows = table.stream().map(row -> {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (String column : columnNames) {
                        rowMap.put(column, row.getObject(column)); // Extract values for each column
                    }
                    return rowMap;
                }).collect(Collectors.toList());

                for (int i = 0; i < rows.size(); i++) {
                    tableData.put(String.valueOf(i + 1), rows.get(i)); // Use row index as the key
                }

                Map<String, Object> finalTableData = new HashMap<>();
                finalTableData.put("data", tableData);

                System.out.println(tableData);

                PdfTsReporting tsPdf = new PdfTsReporting(
                        logger,
                        baseDir.resolve("src/main/resources/logo.png").toString(),
                        baseDir.resolve("src/main/resources/encrypted_jinjava_ts_file.jinjav").toString(),
                        finalTableData,
                        "Test_Summary_Results_" + utils.getDateString());

                tsPdf.generatePdf();
            } catch (Exception e) {
                logger.severe("Error generating test summary PDF: " + e.getMessage());
            }
        }
    }
}
