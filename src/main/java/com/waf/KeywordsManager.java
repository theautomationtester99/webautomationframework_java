package com.waf;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

import com.waf.config.Config;

public class KeywordsManager extends DriverFunctions {
    private String screenshotStrategy;
    private boolean highlightEnabled;
    private int screenshotNo;
    private String screenshotFirstStr;
    private Logger logger;
    private int retryCount;
    protected PdfReportManager repoM;
    private Utils utils;
    private String chromeLogoSrcB64;
    private String edgeLogoSrcB64;
    private String linuxLogoSrcB64;
    private String winLogoSrcB64;
    private String seGridB64;
    private String tempDir;

    public KeywordsManager(String tempDir, int retryCount) {
        super(tempDir);
        this.screenshotStrategy = getScreenshotStrategy();
        this.highlightEnabled = getHighlightElementStrategy();
        this.screenshotNo = 0;
        this.screenshotFirstStr = "";
        this.logger = LogManager.getLogger(KeywordsManager.class);
        this.retryCount = retryCount;
        this.repoM = new PdfReportManager();
        this.repoM.currentRetry = (this.retryCount);
        this.utils = Utils.getInstance();
        this.chromeLogoSrcB64 = Constants.CHROME_LOGO_SRC_B64;
        this.edgeLogoSrcB64 = Constants.EDGE_LOGO_SRC_B64;
        this.linuxLogoSrcB64 = Constants.LINUX_LOGO_SRC_B64;
        this.winLogoSrcB64 = Constants.WIN_LOGO_SRC_B64;
        this.seGridB64 = Constants.SE_GRID_B64;
        this.tempDir = tempDir;
    }

    public void updateRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.repoM.currentRetry = (retryCount);
        this.logger.info("Retry count updated to " + retryCount);
    }

    private String getScreenshotStrategy() {
        String strategy = Config.SCREENSHOT_STRATEGY.toLowerCase();
        if (!strategy.equalsIgnoreCase("always") && !strategy.equalsIgnoreCase("on-error")
                && !strategy.equalsIgnoreCase("never")) {
            this.logger.warn(
                    "Invalid screenshot strategy '" + strategy + "' found in configuration. Defaulting to 'always'.");
            strategy = "always";
        }
        if (strategy.equals("never")) {
            this.logger.info("Mapping 'never' screenshot strategy to 'on-error'.");
            strategy = "on-error";
        }
        return strategy;
    }

    private boolean getHighlightElementStrategy() {
        return Config.HIGHLIGHT_ELEMENTS.equalsIgnoreCase("yes");
    }

    public void geSwitchToDefaultContent() {
        try {
            switchToDefaultContent();
        } catch (Exception e) {
            this.logger.error("An error occurred: " + e.getMessage());
            throw e;
        }
    }

    public void geSwitchToIframe(String locator, String locatorType, String elementName) {
        try {
            this.logger.info("Attempting to switch to iframe '" + elementName + "' using traditional locators.");
            waitForElement(locator, locatorType);
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            switchToIframe(locator, locatorType);
            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Switch to Iframe '" + elementName + "'",
                        "subStepMessage", "Switched to Iframe successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of("subStep", "Switch to Iframe '" + elementName + "'", "subStepMessage",
                        "Switched to Iframe successfully", "subStepStatus", "Pass"));
            }

        } catch (Exception e) {
            this.logger.error("Failed to switch to iframe '" + elementName + "': " + e.getMessage());
            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Switch to Iframe '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of("subStep", "Switch to Iframe '" + elementName + "'", "subStepMessage",
                        "Error Occurred: " + e.getMessage(), "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geClose() {
        this.logger.info("Started creating PDF report");
        this.repoM.createReport();
        this.logger.info("Closing the browser");
        closeBrowser();
    }

    public void geCloseBrowser() {
        this.logger.info("Closing the browser");
        closeBrowser();
    }

    public void geTcid(String tcId) {
        this.logger.info("Setting the test report of the PDF report");
        this.repoM.pageTitle = "Test Report";
        this.repoM.tcId = tcId;
    }

    public void geTcdesc(String tcDesc) {
        this.logger.info("Setting the test description in the PDF report");
        this.repoM.testDescription = tcDesc;
    }

    public void geStep(Map<String, Object> data) {
        this.logger.info("Populating the test details table in the PDF report");
        this.repoM.addReportData(data);
    }

    public void geWaitForSeconds(int howSeconds) {
        this.logger.info("Pausing execution for " + howSeconds + " seconds");
        try {
            Thread.sleep(howSeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.logger.error("Error while waiting: " + e.getMessage());
        }
    }

    public void geScrollPage(String upOrDown) {
        if (upOrDown.equalsIgnoreCase("up") || upOrDown.equalsIgnoreCase("down")) {
            this.logger.info("Scrolling page " + upOrDown + " for 1000 pixels");
            webScroll(upOrDown);
        } else {
            this.logger.warn("Invalid scroll direction: " + upOrDown);
        }
    }

    public void geOpenBrowser(String browserName) {
        try {
            this.logger.info("Launching " + browserName + "...");
            launchBrowser(browserName);

            this.logger.info("Capturing the OS where the browser is running.");

            String osName;
            String hostName;

            if (Config.RUN_IN_SELENIUM_GRID.equalsIgnoreCase("yes")) {
                this.logger.info("Capturing OS where browser is running in Selenium Grid.");
                osName = gridOsInfo;
                hostName = "selenium grid";
                this.repoM.gridImgSrc = (this.seGridB64);
                this.repoM.gridImgAlt = hostName;
            } else {
                this.logger.info("Capturing OS details for local execution.");
                osName = this.utils.detectOS();
                hostName = this.utils.getHostname();
            }

            this.repoM.runningOnHostName = (hostName);

            if (osName.equalsIgnoreCase("linux")) {
                this.logger.info("Setting OS details in PDF report as " + osName);
                this.repoM.osImgSrc = (this.linuxLogoSrcB64);
                this.repoM.osImgAlt = osName;
            } else if (osName.equalsIgnoreCase("windows")) {
                this.logger.info("Setting OS details in PDF report as " + osName);
                this.repoM.osImgSrc = (this.winLogoSrcB64);
                this.repoM.osImgAlt = osName;
            }

            if (browserName.equalsIgnoreCase("chrome")) {
                this.logger.info("Setting browser details in PDF report as Chrome");
                this.repoM.browserImgSrc = (this.chromeLogoSrcB64);
                this.repoM.browserImgAlt = browserName.toUpperCase();
                this.repoM.browserVersion = geBrowserVersion();
            } else if (browserName.equalsIgnoreCase("edge")) {
                this.logger.info("Setting browser details in PDF report as Edge");
                this.repoM.browserImgSrc = (this.edgeLogoSrcB64);
                this.repoM.browserImgAlt = browserName.toUpperCase();
                this.repoM.browserVersion = geBrowserVersion();
            }

            this.logger.info("Populating step result in PDF report.");
            this.screenshotFirstStr = this.repoM.tcId + "_" + this.repoM.browserImgAlt;

            if (isHeadless || isRunningGrid) {
                this.repoM.addReportData(Map.of(
                        "subStep", "Open Browser",
                        "subStepMessage", "The browser opened successfully",
                        "subStepStatus", "Pass"));

            } else {

                if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                    this.screenshotNo++;
                    this.repoM.addReportData(Map.of(
                            "subStep", "Open Browser",
                            "subStepMessage", "The browser opened successfully",
                            "subStepStatus", "Pass",
                            "imageSrc",
                            this.takeScreenshot(this.screenshotFirstStr + "_"
                                    + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                            "imageAlt", this.repoM.browserImgAlt));
                } else {
                    this.repoM.addReportData(Map.of(
                            "subStep", "Open Browser",
                            "subStepMessage", "The browser opened successfully",
                            "subStepStatus", "Pass"));
                }
            }
        } catch (Exception e) {
            this.logger.error("An error occurred while launching browser: " + e.getMessage());
            if (isHeadless || isRunningGrid) {
                this.repoM.addReportData(Map.of(
                        "subStep", "Open Browser",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));

            } else {

                if (this.screenshotStrategy.equalsIgnoreCase("always")
                        || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                    this.screenshotNo++;
                    this.repoM.addReportData(Map.of(
                            "subStep", "Open Browser",
                            "subStepMessage", "Error Occurred: " + e.getMessage(),
                            "subStepStatus", "Fail",
                            "imageSrc",
                            this.takeScreenshot(this.screenshotFirstStr + "_"
                                    + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                            "imageAlt", this.repoM.browserImgAlt));
                } else {
                    this.repoM.addReportData(Map.of(
                            "subStep", "Open Browser",
                            "subStepMessage", "Error Occurred: " + e.getMessage(),
                            "subStepStatus", "Fail"));
                }
            }
            throw e;
        }
    }

    public String geBrowserVersion() {
        this.logger.info("Returning Browser Version.");
        return getBrowserVersion();
    }

    public boolean geIsElementLoaded(String locator, String locatorType) {
        this.logger.info("Checking if element is loaded.");
        try {
            return isElementPresent(locator, locatorType);
        } catch (Exception e) {
            this.logger.info("An error occurred: " + e.getMessage());
            return false;
        }
    }

    public void geIsPageAccessibilityCompliant() {
        try {
            AxeBuilder axeBuilder = new AxeBuilder();
            Results results = axeBuilder.analyze(this.driver);

            String pageTitle = getTitle();
            List<Rule> violations = results.getViolations();

            if (!violations.isEmpty()) {
                this.logger.error("Accessibility check failed with " + violations.size() + " violations.");
                StringBuilder allViolations = new StringBuilder("\n");

                for (Rule violation : violations) {
                    String id = violation.getId();
                    String description = violation.getDescription();
                    String impact = violation.getImpact();
                    String helpUrl = violation.getHelpUrl();

                    this.logger.error("Violation: " + id);
                    this.logger.error("Description: " + description);
                    this.logger.error("Impact: " + impact);
                    this.logger.error("Help URL: " + helpUrl);

                    allViolations.append("\nViolation: ").append(id)
                            .append("\nDescription: ").append(description)
                            .append("\nImpact: ").append(impact)
                            .append("\nHelp URL: ").append(helpUrl);
                }

                this.repoM.addReportData(Map.of(
                        "subStep", "Check if the page '" + pageTitle + "' is accessibility compliant",
                        "subStepMessage",
                        "The element '" + pageTitle + "' is NOT accessibility compliant. Below are the violations: "
                                + allViolations,
                        "subStepStatus", "Fail"));
            } else {
                this.logger.info("Accessibility check passed with no violations.");
                this.repoM.addReportData(Map.of(
                        "subStep", "Check if the page '" + pageTitle + "' is accessibility compliant",
                        "subStepMessage", "The element '" + pageTitle + "' is accessibility compliant",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during accessibility check: " + e.getMessage());
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the page is accessibility compliant",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail"));
            throw e;
        }
    }

    public void geDragAndDrop(String sourceLocator, String sourceLocatorType, String targetLocator,
            String targetLocatorType, String sourceElementName, String targetElementName) {
        this.logger.info("Performing drag-and-drop from '" + sourceElementName + "' to '" + targetElementName + "'.");
        try {
            dragAndDrop(sourceLocator, sourceLocatorType, targetLocator, targetLocatorType);

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                geWaitForSeconds(2);
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Drag and Drop '" + sourceElementName + "' to '" + targetElementName + "'",
                        "subStepMessage", "Drag-and-drop action completed successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Drag and Drop '" + sourceElementName + "' to '" + targetElementName + "'",
                        "subStepMessage", "Drag-and-drop action completed successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during drag-and-drop: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                geWaitForSeconds(2);
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Drag and Drop '" + sourceElementName + "' to '" + targetElementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Drag and Drop '" + sourceElementName + "' to '" + targetElementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geMouseHover(String locator, String locatorType, String elementName) {
        this.logger.info("Performing mouse hover on '" + elementName + "'.");
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            elementHover(locator, locatorType);

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                geWaitForSeconds(2);
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Mouse Hover on '" + elementName + "'",
                        "subStepMessage", "Mouse hover action completed successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Mouse Hover on '" + elementName + "'",
                        "subStepMessage", "Mouse hover action completed successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during mouse hover: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                geWaitForSeconds(2);
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Mouse Hover on '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Mouse Hover on '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geIsElementEnabled(String locator, String locatorType, String elementName) {
        this.logger.info("Checking if element '" + elementName + "' is enabled.");
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            boolean isEnabled = isElementEnabled(locator, locatorType);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is enabled",
                    "subStepMessage", "The element '" + elementName + "' is " + (isEnabled ? "enabled" : "NOT enabled"),
                    "subStepStatus", isEnabled ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while checking element status: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is enabled",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geIsChkRadioElementSelected(String locator, String locatorType, String elementName) {
        this.logger.info("Checking if element '" + elementName + "' is selected.");
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            boolean isSelected = isChkRadioElementSelected(locator, locatorType);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is selected",
                    "subStepMessage",
                    "The element '" + elementName + "' is " + (isSelected ? "selected" : "NOT selected"),
                    "subStepStatus", isSelected ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while checking element selection: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is selected",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geIsChkRadioElementNotSelected(String locator, String locatorType, String elementName) {
        this.logger.info("Checking if element '" + elementName + "' is NOT selected.");
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            boolean isNotSelected = !isChkRadioElementSelected(locator, locatorType);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is NOT selected",
                    "subStepMessage",
                    "The element '" + elementName + "' is " + (isNotSelected ? "NOT selected" : "selected"),
                    "subStepStatus", isNotSelected ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while checking selection status: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is NOT selected",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geIsElementDisabled(String locator, String locatorType, String elementName) {
        this.logger.info("Checking if element '" + elementName + "' is disabled.");
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            boolean isDisabled = !isElementEnabled(locator, locatorType);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is disabled",
                    "subStepMessage",
                    "The element '" + elementName + "' is " + (isDisabled ? "disabled" : "NOT disabled"),
                    "subStepStatus", isDisabled ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while checking disabled status: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is disabled",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geIsElementDisplayed(String locator, String locatorType, String elementName) {
        this.logger.info("Checking if element '" + elementName + "' is displayed.");
        try {
            boolean isDisplayed = isElementPresent(locator, locatorType);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is displayed",
                    "subStepMessage",
                    "The element '" + elementName + "' is " + (isDisplayed ? "displayed" : "NOT displayed"),
                    "subStepStatus", isDisplayed ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while checking display status: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Check if the element '" + elementName + "' is displayed",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geEnterUrl(String url) {
        this.logger.info("Opening URL: " + url);
        try {
            openUrl(url);
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Enter URL '" + url + "'",
                        "subStepMessage", "The URL is entered successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Enter URL '" + url + "'",
                        "subStepMessage", "The URL is entered successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred while entering URL: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Enter URL '" + url + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Enter URL '" + url + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geType(String locator, String locatorType, String textToType, String elementName) {
        this.logger.info("Typing text: " + textToType);
        try {
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            elementClick(locator, locatorType);
            sendKeys(textToType, locator, locatorType);
            geWaitForSeconds(1);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Type the text '" + textToType + "' in '" + elementName + "'",
                        "subStepMessage", "The text should be typed successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Type the text '" + textToType + "' in '" + elementName + "'",
                        "subStepMessage", "The text should be typed successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred while typing text: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Type the text '" + textToType + "' in '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Type the text '" + textToType + "' in '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geVerifyDisplayedText(String locator, String locatorType, String expectedText, String elementName) {
        try {
            waitForElement(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            String actualText = getText(locator, locatorType, elementName);
            boolean isMatched = actualText.equals(expectedText);

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Verifying the text '" + expectedText + "' in '" + elementName + "'",
                    "subStepMessage", "The text is '" + actualText + "'",
                    "subStepStatus", isMatched ? "Pass" : "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
        } catch (Exception e) {
            this.logger.error("An error occurred while verifying displayed text: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep", "Verifying the text '" + expectedText + "' in '" + elementName + "'",
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail",
                    "imageSrc",
                    this.takeScreenshot(
                            this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                    "imageAlt", this.repoM.browserImgAlt));
            throw e;
        }
    }

    public void geVerifyFileDownloaded(String partialFilename) {
        try {
            geWaitForSeconds(2);

            boolean downloadComplete = false;
            long timeout = 300_000; // 300 seconds
            long startTime = System.currentTimeMillis();
            List<String> matchingFiles = this.utils.getMatchingFilesInDir(this.tempDir, partialFilename);

            while (!downloadComplete) {
                this.logger.info("Checking download status in directory: " + this.tempDir);

                if (!matchingFiles.isEmpty()
                        && !this.utils.doFilesWithExtInDir(this.tempDir, Arrays.asList(".crdownload", ".tmp"))) {
                    this.logger.info("Download completed: " + matchingFiles);
                    downloadComplete = true;
                }

                if (System.currentTimeMillis() - startTime > timeout) {
                    this.logger.warn("Download tracking timed out.");
                    break;
                }

                try {

                    Thread.sleep(5000);
                } catch (Exception e) {
                    this.logger.error(e.toString());
                }
            }

            if (downloadComplete && !matchingFiles.isEmpty()) {
                this.repoM.addReportData(Map.of(
                        "subStep",
                        "Check if the file containing '" + partialFilename + "' is downloaded into the directory "
                                + this.tempDir,
                        "subStepMessage", "The file is downloaded successfully",
                        "subStepStatus", "Pass"));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep",
                        "Check if the file containing '" + partialFilename + "' is downloaded into the directory "
                                + this.tempDir,
                        "subStepMessage", "The file is NOT downloaded successfully or timed out",
                        "subStepStatus", "Fail"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred while verifying file download: " + e.getMessage());

            this.screenshotNo++;
            this.repoM.addReportData(Map.of(
                    "subStep",
                    "Check if the file containing '" + partialFilename + "' is downloaded into the directory "
                            + this.tempDir,
                    "subStepMessage", "Error Occurred: " + e.getMessage(),
                    "subStepStatus", "Fail"));
            throw e;
        }
    }

    public void geJsClick(String locator, String locatorType, String elementName) {
        try {
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            elementJsClick(locator, locatorType);
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            // Handle screenshot strategy
            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "The click is successful",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "The click is successful",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during JavaScript click: " + e.getMessage());

            // Handle screenshot strategy for errors
            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geClick(String locator, String locatorType, String elementName) {
        try {
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            elementClick(locator, locatorType);
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            // Handle screenshot strategy
            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "The click is successful",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "The click is successful",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during click: " + e.getMessage());

            // Handle screenshot strategy for errors
            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Click on the element '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geSelectDropdownByValue(String locator, String locatorType, String elementName, String value) {
        try {
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            dropdownSelectElement(locator, locatorType, value, "value");
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during dropdown selection by value: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geSelectDropdownByIndex(String locator, String locatorType, String elementName, int index) {
        try {
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            dropdownSelectElement(locator, locatorType, String.valueOf(index), "index");
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during dropdown selection by index: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geSelectDropdownByVisibleText(String locator, String locatorType, String elementName, String text) {
        try {
            scrollIntoView(locator, locatorType);
            if (this.highlightEnabled) {
                highlightElement(1, "blue", 2, locator, locatorType);
            }
            dropdownSelectElement(locator, locatorType, text, "text");
            geWaitForSeconds(2);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "The selection is successful",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during dropdown selection by visible text: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Select from dropdown '" + elementName + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geSelectFile(String locator, String locatorType, String filePaths, String elementName) {
        try {
            geClick(locator, locatorType, elementName);
            geWaitForSeconds(2);
            fileNameToSelect(filePaths);
            waitForElement(locator, locatorType);
            scrollIntoView(locator, locatorType);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "The file is added successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "The file is added successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during file selection: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geUploadFile(String locator, String locatorType, String filePaths) {
        try {
            scrollIntoView(locator, locatorType);
            fileNameToUpload(filePaths, locator, locatorType);
            geWaitForSeconds(1);
            this.logger.info("Populating the step result details in the PDF report.");

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "The file is added successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "The file is added successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during file upload: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Upload the file '" + filePaths + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

    public void geChooseDateFromDatePicker(Map<String, String> params) {
        String whichCalendar = params.get("which_calendar");
        String expectedDate = params.get("date_to_choose");
        try {
            if (whichCalendar.equals("a")) {
                String[] dateParts = expectedDate.split(" ");
                String monYr = dateParts[1] + " " + dateParts[2];
                int yr = Integer.parseInt(dateParts[2]);
                String mon = dateParts[1];
                int monNbr = this.utils.getMonthNumber(mon);
                String day = dateParts[0];

                while (true) {
                    String displayedMonth = getText(params.get("date_mon_txt_loc"), params.get("locator_mon_type"),
                            params.get("locator_name"));
                    int dispYr = Integer.parseInt(displayedMonth.split(" ")[1]);
                    String dispMon = displayedMonth.split(" ")[0];
                    int dispMonNbr = this.utils.getMonthNumber(dispMon);

                    if (displayedMonth.equals(monYr)) {
                        break;
                    }

                    if (dispYr > yr) {
                        elementClick(params.get("date_pre_button_loc"), params.get("locator_pre_type"));
                    } else if (dispYr < yr) {
                        elementClick(params.get("date_nxt_button_loc"), params.get("locator_nxt_type"));
                    } else {
                        if (dispMonNbr > monNbr) {
                            elementClick(params.get("date_pre_button_loc"), params.get("locator_pre_type"));
                        } else {
                            elementClick(params.get("date_nxt_button_loc"), params.get("locator_nxt_type"));
                        }
                    }
                }

                List<WebElement> dateElements = getElementList(params.get("date_date_list_loc"),
                        params.get("locator_dt_lst_type"));
                for (WebElement dt : dateElements) {
                    String dispDay = dt.getText();
                    String dispDayTwoDig = String.format("%02d", Integer.parseInt(dispDay));
                    if (dispDayTwoDig.equals(day)) {
                        elementJsClick(dt);
                        break;
                    }
                }
            } else if (whichCalendar.equals("b")) {
                String[] dateParts = expectedDate.split(" ");
                String monYr = dateParts[1] + " " + dateParts[2];
                int yr = Integer.parseInt(dateParts[2]);
                String mon = dateParts[1];
                int monNbr = this.utils.getMonthNumber(mon);
                String day = dateParts[0];

                dropdownSelectElement(params.get("date_mon_select_loc"), params.get("locator_mon_select_type"), mon,
                        "text");
                dropdownSelectElement(params.get("date_yr_select_loc"), params.get("locator_yr_select_type"),
                        String.valueOf(yr), "value");

                List<WebElement> dateElements = getElementList(params.get("date_date_list_loc"),
                        params.get("locator_dt_lst_type"));
                for (WebElement dt : dateElements) {
                    String dispDay = dt.getText();
                    String dayAttributeValue = dt.getDomAttribute("aria-label");
                    String dispDayTwoDig = String.format("%02d", Integer.parseInt(dispDay));

                    if (dispDayTwoDig.equals(day) && dayAttributeValue.toLowerCase().contains(mon.toLowerCase())) {
                        elementJsClick(dt);
                        break;
                    }
                }
            }

            if (this.screenshotStrategy.equalsIgnoreCase("always")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Choosing the date '" + expectedDate + "' in '" + params.get("locator_name") + "'",
                        "subStepMessage", "The date is chosen successfully",
                        "subStepStatus", "Pass",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Choosing the date '" + expectedDate + "' in '" + params.get("locator_name") + "'",
                        "subStepMessage", "The date is chosen successfully",
                        "subStepStatus", "Pass"));
            }
        } catch (Exception e) {
            this.logger.error("An error occurred during date selection: " + e.getMessage());

            if (this.screenshotStrategy.equalsIgnoreCase("always")
                    || this.screenshotStrategy.equalsIgnoreCase("on-error")) {
                this.screenshotNo++;
                this.repoM.addReportData(Map.of(
                        "subStep", "Choosing the date '" + expectedDate + "' in '" + params.get("locator_name") + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail",
                        "imageSrc",
                        this.takeScreenshot(
                                this.screenshotFirstStr + "_" + this.utils.formatNumberZeroPad4Char(this.screenshotNo)),
                        "imageAlt", this.repoM.browserImgAlt));
            } else {
                this.repoM.addReportData(Map.of(
                        "subStep", "Choosing the date '" + expectedDate + "' in '" + params.get("locator_name") + "'",
                        "subStepMessage", "Error Occurred: " + e.getMessage(),
                        "subStepStatus", "Fail"));
            }
            throw e;
        }
    }

}
