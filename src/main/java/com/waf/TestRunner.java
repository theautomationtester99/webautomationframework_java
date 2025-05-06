package com.waf;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hello world!
 *
 */
public class TestRunner {
    public static void main(String[] args) {

        Logger logger = LogManager.getLogger(TestRunner.class);

        // Provide the path to the file to be tested
        Utils utils = Utils.getInstance();
        String filePath = "D:\\allprojects\\web_automation_htmltopdf\\test_scripts\\QS002_tesQScrip1t.xlsx";
        // String filePath = "D:\\allprojects\\web_automation_htmltopdf\\.gitignore";
        // String filePath =
        // "D:\\allprojects\\web_automation_htmltopdf\\test_scripts\\1t.xls";

        // Create a File object for the path
        File testFile = new File(filePath);

        // Test the isExcelDoc function
        boolean result = utils.isExcelDoc(testFile);

        boolean dv = utils.isDateFormatValid("31 April 2021");

        // Print the result
        if (result) {
            // System.out.println("The file " + filePath + " is an Excel document.");
        } else {
            // System.out.println("The file " + filePath + " is NOT an Excel document.");
        }

        // Print the result
        if (dv) {
            // System.out.println("Valid date");
        } else {
            // System.out.println("Invalid date");
        }
    }
}
