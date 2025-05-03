package com.waf;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.hubspot.jinjava.Jinjava;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.xlsx.XlsxReadOptions;

public class PdfTsReporting {
    private Logger logger;
    private Utils utils;
    private String logoPath;
    private String base64Logo;
    private String htmlContent;
    private String headTemplate;
    private String footerTemplate;
    private String documentNamePdf;

    public PdfTsReporting(Logger logger, String logoPath, String encryptedTemplateFilePath, Map<String, Object> data,
            String documentName) {
        this.logger = logger;
        this.utils = Utils.getInstance(logger);
        this.logoPath = logoPath;
        this.base64Logo = encodeLogo();
        this.htmlContent = generateHtml(encryptedTemplateFilePath, data);
        this.headTemplate = createHeaderTemplate();
        this.footerTemplate = createFooterTemplate();
        this.documentNamePdf = utils.getTestResultsFolder() + File.separator + documentName + ".pdf";
    }

    private String encodeLogo() {
        logger.info("Encoding the logo PNG file");
        try (InputStream imageFile = new FileInputStream(logoPath)) {
            byte[] imageBytes = imageFile.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.severe(e.toString());
            return null;
        }
    }

    private String createHeaderTemplate() {
        logger.info("Creating the PDF report header template");
        return String.format(
                "<div style=\"display: flex; width: 100%%; height: auto; justify-content: space-between; align-items: center; border-bottom: 2px solid #413a97;\">"
                        + "<div style=\"flex: 0; margin-left: 5px;\">"
                        + "<img id=\"logo\" src=\"data:image/png;base64,%s\" alt=\"Logo\" style=\"max-height: 99px; object-fit: contain;\">"
                        + "</div>"
                        + "<div id=\"tcid\" style=\"flex: 0; font-size: 20px; white-space: nowrap; margin-right: 20px;\">"
                        + "<h1>Test Summary Report</h1>"
                        + "</div>"
                        + "</div>",
                base64Logo);
    }

    private String createFooterTemplate() {
        logger.info("Creating the PDF report footer template");
        return "<div style='font-size:10px; width:100%%; text-align:center;'>"
                + "Page <span class=\"pageNumber\"></span> of <span class=\"totalPages\"></span>"
                + "</div>";
    }

    private String generateHtml(String encryptedTemplateFilePath, Map<String, Object> data) {
        logger.info("Decrypting the PDF report template");
        try {
            String decryptedTemplate = utils.decryptFile(encryptedTemplateFilePath);
            Jinjava jinjava = new Jinjava();
            System.out.println("====================================");
            System.out.println(data);

            return jinjava.render(decryptedTemplate, data);
        } catch (Exception e) {
            logger.severe(e.toString());
            return null;
        }
    }

    public void generatePdf() {
        logger.info("Starting creation of PDF report from template with populated data");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            page.setContent(htmlContent);

            page.pdf(new Page.PdfOptions()
                    .setPath(Paths.get(documentNamePdf))
                    .setFormat("Letter")
                    .setPrintBackground(true)
                    .setLandscape(true)
                    .setMargin(new Margin()
                            .setTop("125px")
                            .setBottom("25px")
                            .setLeft("25px")
                            .setRight("25px"))
                    .setDisplayHeaderFooter(true)
                    .setHeaderTemplate(headTemplate)
                    .setFooterTemplate(footerTemplate));

            logger.info("Completed creation of PDF report from template with populated data");
            browser.close();
        } catch (Exception e) {
            logger.severe(e.toString());
        }
    }

    public static void main(String[] args) throws IOException {
        // Initialize logger
        LoggerConfig loggerConfig = new LoggerConfig("");
        loggerConfig.startListener();

        // Call setupLogger to configure the logger
        Logger logger = loggerConfig.setupLogger();
        Utils utils = Utils.getInstance(logger);

        // Define file paths
        String logoPath = "D:\\\\allprojects\\\\java-projects\\\\webautomationframework\\\\src\\\\main\\\\resources\\\\logo.png"; // Update
                                                                                                                                  // with
                                                                                                                                  // actual
                                                                                                                                  // path
        String encryptedTemplateFilePath = "D:\\\\allprojects\\\\java-projects\\\\webautomationframework\\\\src\\\\main\\\\resources\\\\encrypted_jinjava_td_file.jinjav"; // Update
                                                                                                                                                                           // with
                                                                                                                                                                           // actual
                                                                                                                                                                           // path
        String documentName = "TestSummaryReport";

        Path baseDir = Paths.get(System.getProperty("user.dir"));
        String trFolder = utils.getTestResultsFolder();
        Path summaryResultsFile = Paths
                .get("D:\\allprojects\\java-projects\\webautomationframework\\test_results\\output.xlsx");

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
        System.out.println("PDF report generated successfully!");
        loggerConfig.stopListener();
    }
}
