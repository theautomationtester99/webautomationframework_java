package com.waf;

import java.util.logging.Logger;
import com.microsoft.playwright.*;

public class SMBrowserDownloader extends DriverFunctions {
    /**
     * A manager class extending DriverFunctions that provides utilities to manage
     * browser interactions,
     * generate PDF reports, and perform common testing tasks for any application
     * under test.
     *
     * Attributes:
     * - logger: Logger instance for logging debug, info, and error messages.
     * - repo_m: PdfReportManager instance to manage and generate PDF reports.
     * - utils: Utility methods instance for OS detection and other operations.
     * - chromeLogoSrcB64: Base64 encoded string for Chrome logo image.
     * - edgeLogoSrcB64: Base64 encoded string for Edge logo image.
     * - linuxLogoSrcB64: Base64 encoded string for Linux logo image.
     * - winLogoSrcB64: Base64 encoded string for Windows logo image.
     */

    private Logger logger;

    public SMBrowserDownloader(Logger logger) {
        super(logger);
        this.logger = logger;
    }

    public void setupSmBrowsers(String browserName) {
        /**
         * Launches the specified browser, captures OS and browser details,
         * and logs information in the PDF report.
         *
         * @param browserName The name of the browser to launch (e.g., "Chrome",
         *                    "Edge").
         * @throws Exception If any error occurs while launching the browser.
         */
        try {
            logger.info("Launching " + browserName + " .......");
            launchBrowser(browserName);
        } catch (Exception e) {
            logger.severe("An error occurred: " + e.getMessage());
            throw e;
        }
    }

    public void closeSmBrowsers() {
        /**
         * Closes the browser and logs information in the PDF report.
         *
         * @throws Exception If any error occurs while closing the browser.
         */
        try {
            closeBrowser();
        } catch (Exception e) {
            logger.severe("An error occurred: " + e.getMessage());
            throw e;
        }
    }

    public void setupPlaywrightBrowser() {
        /**
         * Checks whether Playwright's browser launches successfully.
         */
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent("<html><body><div>test</div></body></html>");
            logger.info("Playwright browser launched successfully!");
            browser.close();
            logger.info("Playwright browser closed successfully!");
        } catch (Exception e) {
            logger.severe("Failed to launch Playwright browser: " + e.getMessage());
        }
    }
}
