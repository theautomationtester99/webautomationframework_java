package com.waf;

import java.io.*;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hubspot.jinjava.Jinjava;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;

public class PdfReporting {
    // Attributes
    private Utils utils;
    private String tcId;
    private String logoPath;
    private String base64Logo;
    private String htmlContent;
    private String headTemplate;
    private String footerTemplate;
    private String documentNamePdf;
    private final Logger logger;

    // Constructor
    public PdfReporting(String logoPath, String encryptedTemplateFilePath, Map<String, Object> data,
            String tcId, String documentName) {
        this.logger = LogManager.getLogger(PdfReporting.class);
        this.utils = Utils.getInstance();
        this.tcId = tcId;
        this.logoPath = logoPath;
        this.base64Logo = encodeLogo();
        this.htmlContent = generateHtml(encryptedTemplateFilePath, data);
        this.headTemplate = createHeaderTemplate();
        this.footerTemplate = createFooterTemplate();
        this.documentNamePdf = utils.getTestResultsFolder() + File.separator + documentName + ".pdf";
    }

    // Private methods
    private String encodeLogo() {
        logger.info("Encoding the logo png file");
        try (InputStream imageFile = new FileInputStream(logoPath)) {
            byte[] imageBytes = imageFile.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.error(e.toString());
            return null;
        }
    }

    private String createHeaderTemplate() {
        logger.info("Creating the PDF report header template");
        return String.format(
                "<div style=\"display: flex; width: 100%%; height: auto; justify-content: space-between; align-items: center; border-bottom: 2px solid #413a97;\">"
                        + "<div style=\"flex: 0; margin-left: 5px;\">"
                        + "<img id=\"logo\" src=\"data:image/png;base64,%s\" alt=\"Logo\" style=\"max-height: 60px; object-fit: contain;\">"
                        + "</div>"
                        + "<div id=\"tcid\" style=\"flex: 0; font-size: 10px; white-space: nowrap; margin-right: 20px;\">"
                        + "<h1>%s</h1>"
                        + "</div>"
                        + "</div>",
                base64Logo, tcId);
    }

    private String createFooterTemplate() {
        logger.info("Creating the PDF report footer template");
        return "<div style='font-size:10px; width:100%; text-align:center;'>"
                + "Page <span class=\"pageNumber\"></span> of <span class=\"totalPages\"></span>"
                + "</div>";
    }

    private String generateHtml(String encryptedTemplateFilePath, Map<String, Object> data) {
        logger.info("Decrypting the PDF report template");
        try {
            String decryptedTemplate = utils.decryptFile(encryptedTemplateFilePath);
            Jinjava jinjava = new Jinjava();
            return jinjava.render(decryptedTemplate, data);
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
    }

    public void generatePdf() {
        logger.info("Starting creation of PDF report from template with populated data");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // Set the HTML content
            page.setContent(htmlContent);

            // Generate the PDF
            page.pdf(new Page.PdfOptions()
                    .setPath(Paths.get(documentNamePdf))
                    .setFormat("Letter")
                    .setPrintBackground(true)
                    .setLandscape(true)
                    .setMargin(new Margin()
                            .setTop("85px")
                            .setBottom("25px")
                            .setLeft("25px")
                            .setRight("25px"))
                    .setDisplayHeaderFooter(true)
                    .setHeaderTemplate(headTemplate)
                    .setFooterTemplate(footerTemplate));

            logger.info("Completed creation of PDF report from template with populated data");
            browser.close();
        } catch (Exception e) {
            logger.info(e.toString());
        }
    }

    // public static void main(String[] args) throws IOException {
    // // Creating sub-steps for retry data
    // SubStep subStep1 = new SubStep();
    // subStep1.setSubStep("Navigate to URL");
    // subStep1.setSubStepMessage("Navigated to https://example.com");
    // subStep1.setImageSrc(
    // "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAg0lEQVR4nO2XQQrAMAgEfUb3ofGhPqP3ni0l9J4QKQ3MgFdBNLsbMwCAXyKPXK2319Hiquo1DAM4G0hOSDziDirkyGhgZMKJJyFKOFEiiRIqUg61OB83XimD3VDhl7Ky1zAM4GwgOSHxiDuokCOjgZEJJ56EKOFEiSRKaOcoAQBgX3AD2Km4gQP8in4AAAAASUVORK5CYII=");
    // subStep1.setImageAlt("Step Image");
    // subStep1.setSubStepStatus("Pass");

    // SubStep subStep2 = new SubStep();
    // subStep2.setSubStep("Verify page title");
    // subStep2.setSubStepMessage("Expected title: 'Example Domain', Actual title:
    // 'Example Domain'");
    // subStep2.setSubStepStatus("Pass");

    // Map<String, SubStep> subSteps1 = new HashMap<>();
    // subSteps1.put("1", subStep1);
    // subSteps1.put("2", subStep2);

    // // Creating test steps
    // TestStep step1 = new TestStep();
    // step1.setSno("1");
    // step1.setRowspan("3");
    // step1.setStep("Open Browser");
    // step1.setResult("Browser opened successfully");
    // step1.setOverallStepStatus("Pass");
    // step1.setSubSteps(subSteps1);

    // TestStep step2 = new TestStep();
    // step2.setSno("2");
    // step2.setRowspan("2");
    // step2.setStep("Login to Application");
    // step2.setResult("Failed to login");
    // step2.setOverallStepStatus("Fail");

    // Map<String, SubStep> subSteps2 = new HashMap<>();
    // SubStep loginSubStep = new SubStep();
    // loginSubStep.setSubStep("Enter username and password");
    // loginSubStep.setSubStepMessage("Invalid credentials provided");
    // //
    // loginSubStep.setImageSrc("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAg0lEQVR4nO2XQQrAMAgEfUb3ofGhPqP3ni0l9J4QKQ3MgFdBNLsbMwCAXyKPXK2319Hiquo1DAM4G0hOSDziDirkyGhgZMKJJyFKOFEiiRIqUg61OB83XimD3VDhl7Ky1zAM4GwgOSHxiDuokCOjgZEJJ56EKOFEiSRKaOcoAQBgX3AD2Km4gQP8in4AAAAASUVORK5CYII=");
    // // loginSubStep.setImageAlt("Step Image");
    // loginSubStep.setSubStepStatus("Fail");
    // subSteps2.put("1", loginSubStep);

    // step2.setSubSteps(subSteps2);

    // Map<String, TestStep> retry1Steps = new HashMap<>();
    // retry1Steps.put("1", step1);
    // retry1Steps.put("2", step2);

    // // Creating retry data
    // RetryData retry1 = new RetryData();
    // retry1.setRstatus("Fail");
    // retry1.setRerror("Error Occurred: Message: no such element: Unable to locate
    // element.");
    // retry1.setSteps(retry1Steps);

    // //////////////////////////////
    // ///
    // ///
    // SubStep subStepr21 = new SubStep();
    // subStepr21.setSubStep("Navigate to URL");
    // subStepr21.setSubStepMessage("Navigated to https://example.com");
    // subStepr21.setImageSrc(
    // "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAg0lEQVR4nO2XQQrAMAgEfUb3ofGhPqP3ni0l9J4QKQ3MgFdBNLsbMwCAXyKPXK2319Hiquo1DAM4G0hOSDziDirkyGhgZMKJJyFKOFEiiRIqUg61OB83XimD3VDhl7Ky1zAM4GwgOSHxiDuokCOjgZEJJ56EKOFEiSRKaOcoAQBgX3AD2Km4gQP8in4AAAAASUVORK5CYII=");
    // subStepr21.setImageAlt("Step Image");
    // subStepr21.setSubStepStatus("Pass");

    // SubStep subStepr22 = new SubStep();
    // subStepr22.setSubStep("Verify page title");
    // subStepr22.setSubStepMessage("Expected title: 'Example Domain', Actual title:
    // 'Example Domain'");
    // subStepr22.setSubStepStatus("Pass");

    // Map<String, SubStep> subStepsr21 = new HashMap<>();
    // subStepsr21.put("1", subStepr21);
    // subStepsr21.put("2", subStepr22);

    // // Creating test steps
    // TestStep stepr21 = new TestStep();
    // stepr21.setSno("1");
    // stepr21.setRowspan("3");
    // stepr21.setStep("Open Browser");
    // stepr21.setResult("Browser opened successfully");
    // stepr21.setOverallStepStatus("Pass");
    // stepr21.setSubSteps(subStepsr21);

    // TestStep stepr22 = new TestStep();
    // stepr22.setSno("2");
    // stepr22.setRowspan("2");
    // stepr22.setStep("Login to Application");
    // stepr22.setResult("Failed to login");
    // stepr22.setOverallStepStatus("Fail");

    // Map<String, SubStep> subStepsr22 = new HashMap<>();
    // SubStep loginSubStepr2 = new SubStep();
    // loginSubStepr2.setSubStep("Enter username and password");
    // loginSubStepr2.setSubStepMessage("Invalid credentials provided");
    // //
    // loginSubStep.setImageSrc("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAg0lEQVR4nO2XQQrAMAgEfUb3ofGhPqP3ni0l9J4QKQ3MgFdBNLsbMwCAXyKPXK2319Hiquo1DAM4G0hOSDziDirkyGhgZMKJJyFKOFEiiRIqUg61OB83XimD3VDhl7Ky1zAM4GwgOSHxiDuokCOjgZEJJ56EKOFEiSRKaOcoAQBgX3AD2Km4gQP8in4AAAAASUVORK5CYII=");
    // // loginSubStep.setImageAlt("Step Image");
    // loginSubStepr2.setSubStepStatus("Fail");
    // subStepsr22.put("1", loginSubStepr2);

    // stepr22.setSubSteps(subStepsr22);

    // Map<String, TestStep> retry1Stepsr2 = new HashMap<>();
    // retry1Stepsr2.put("1", stepr21);
    // retry1Stepsr2.put("2", stepr22);

    // // Creating retry data
    // RetryData retry1r2 = new RetryData();
    // retry1r2.setRstatus("Fail");
    // retry1r2.setRerror("Error Occurred: Message: no such element: Unable to
    // locate element.");
    // retry1r2.setSteps(retry1Stepsr2);

    // Map<String, RetryData> tableData = new HashMap<>();
    // tableData.put("retry_1", retry1);
    // tableData.put("retry_2", retry1r2);

    // // Creating the root object
    // TestReport testReport = new TestReport();
    // testReport.setPageTitle("Test Report");
    // testReport.setTestDescription("Sample test description goes here.");
    // testReport.setBrowserImgSrc(Constants.CHROME_LOGO_SRC_B64);
    // testReport.setOsImgSrc(Constants.LINUX_LOGO_SRC_B64);
    // testReport.setOsImgAlt("linux");
    // testReport.setBrowserImgAlt("Browser Logo");
    // testReport.setBrowserVersion("Version 110.0.1.1.1122");
    // testReport.setExecutedDate("25MAR2025");
    // testReport.setOverallStatusText("FAILED");
    // testReport.setTableData(tableData);

    // // Print a message
    // //System.out.println("TestReport object created successfully!");

    // // //System.out.println(testReport);
    // // Convert TestReport to JSON and print
    // ObjectMapper objectMapper = new ObjectMapper();
    // // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    // // false);
    // // try {
    // // String json =
    // //
    // objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testReport);
    // // //System.out.println(json);
    // // } catch (JsonProcessingException e) {
    // // e.printStackTrace();
    // // }

    // // Convert to Map<String, Object>
    // Map<String, Object> testReportMap = objectMapper.convertValue(testReport,
    // Map.class);
    // //System.out.println(testReportMap);

    // String json =
    // objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testReport);

    // PdfReporting pdfReport = new
    // PdfReporting("D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\logo.png",
    // "D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\encrypted_jinjava_file.jinjav",testReportMap,
    // "String tcId", "documentName");

    // pdfReport.generatePdf();
    // }
}
