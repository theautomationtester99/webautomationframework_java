package com.waf;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoWriter;

import com.waf.config.Config;

public class ExecutionManager {
    private static final String DEFAULT_PID_FILE = "processes.txt";

    public static void startExecution(Logger logger, Utils utils, Lock lock, boolean isParallel) throws Exception {
        try {
            logger.info("Setting up test execution...");
            Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            ConfigReader objectRepoReader = new ConfigReader(
                    baseDir.resolve("resources/object_repository.properties").toString());
            PdfReportManager prm = new PdfReportManager(logger);
            boolean runInSeleniumGrid = "yes".equalsIgnoreCase(Config.RUN_IN_SELENIUM_GRID);
            if (!runInSeleniumGrid) {
                setupDrivers(logger);
                utils.stopDriverProcesses();
                List<Long> pids = readPidsFromFile(logger);
                stopRunningProcesses(pids, logger);
                clearPidFile(logger);
                long pid = ProcessHandle.current().pid();
                writePidToFile(pid, logger);
            }

            if (isParallel) {
                logger.warning("Parallel execution is enabled. Initializing parallel execution.");
                // Implement logic for parallel execution setup
            } else {
                logger.info("Starting standard execution.");
                startExecutionSingle(logger, lock, utils, objectRepoReader, prm);
            }

        } catch (Exception e) {
            logger.severe(e.getMessage());
            throw e;
        }
    }

    private static void writePidToFile(long pid, Logger logger) {
        Path scriptDir = Paths.get(System.getProperty("user.dir"));
        Path pidFilePath = scriptDir.resolve(DEFAULT_PID_FILE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pidFilePath.toFile(), true))) {
            writer.write(pid + "\n");
            writer.flush();
            logger.info("Saved PID: " + pid);
        } catch (IOException e) {
            logger.severe("Error writing PID to file: " + e.getMessage());
        }
    }

    public static void clearPidFile(Logger logger) {
        Path scriptDir = Paths.get(System.getProperty("user.dir"));
        Path pidFilePath = scriptDir.resolve(DEFAULT_PID_FILE);

        File pidFile = pidFilePath.toFile();
        if (pidFile.exists()) {
            try (FileWriter writer = new FileWriter(pidFile, false)) {
                writer.write(""); // Clears file contents
                writer.flush();
                logger.info("PID file cleared successfully.");
            } catch (IOException e) {
                logger.severe("Error clearing PID file: " + e.getMessage());
            }
        } else {
            logger.warning("PID file does not exist.");
        }
    }

    private static List<Long> readPidsFromFile(Logger logger) {
        Path scriptDir = Paths.get(System.getProperty("user.dir"));
        Path pidFilePath = scriptDir.resolve(DEFAULT_PID_FILE);
        List<Long> pids = new ArrayList<>();

        if (!pidFilePath.toFile().exists()) {
            logger.warning("PID file does not exist.");
            return pids;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(pidFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    pids.add(Long.parseLong(line.trim()));
                } catch (NumberFormatException e) {
                    logger.warning("Skipping invalid PID entry: " + line);
                }
            }
        } catch (IOException e) {
            logger.severe("Error reading PID file: " + e.getMessage());
        }

        return pids;
    }

    private static void stopRunningProcesses(List<Long> pids, Logger logger) {
        for (Long pid : pids) {
            try {
                ProcessHandle process = ProcessHandle.of(pid).orElseThrow();
                process.destroy();
                logger.info("Stopped process with PID: " + pid);
            } catch (Exception e) {
                logger.warning("No process found with PID: " + pid);
            }
        }
    }

    private static void setupDrivers(Logger logger) {
        SMBrowserDownloader smbd = new SMBrowserDownloader(logger);

        // Setup ChromeDriver
        try {
            smbd.setupSmBrowsers("chrome");
            logger.warning("ChromeDriver is ready!");
            smbd.closeSmBrowsers();
        } catch (Exception e) {
            logger.severe("Error setting up ChromeDriver: " + e.getMessage());
            throw e;
        }

        // Setup EdgeDriver
        try {
            smbd.setupSmBrowsers("edge");
            logger.warning("EdgeDriver is ready!");
            smbd.closeSmBrowsers();
        } catch (Exception e) {
            logger.severe("Error setting up EdgeDriver: " + e.getMessage());
            throw e;
        }

        // Setup Playwrite browser

        try {
            smbd.setupPlaywrightBrowser();
        } catch (Exception e) {
            logger.severe("Error setting up EdgeDriver: " + e.getMessage());
            throw e;
        }
    }

    public static void startExecutionSingle(Logger logger, Lock lock, Utils utils, ConfigReader objectRepoReader,
            PdfReportManager prm) {
        long startTime = System.currentTimeMillis();

        boolean runHeadless = "yes".equalsIgnoreCase(Config.HEADLESS);
        boolean runInGrid = "yes".equalsIgnoreCase(Config.RUN_IN_SELENIUM_GRID);
        boolean runInAppium = "yes".equalsIgnoreCase(Config.RUN_IN_APPIUM_GRID);
        boolean uploadTr = "yes".equalsIgnoreCase(Config.UPLOAD_TEST_RESULTS);

        logger.info("Gathering all files present in the test_scripts folder.");
        Path scriptDir = Path.of(System.getProperty("user.dir"), "test_scripts");

        List<String> filePaths = utils.getListAbsPathsOfDirAndSubDir(scriptDir.toString());

        Pattern pattern = Pattern.compile("^qs[a-zA-Z0-9_]*_testscript\\.xlsx$", Pattern.CASE_INSENSITIVE);

        for (String filePath : filePaths) {
            File file = new File(filePath);
            String fileName = file.getName();
            String parentDir = file.getParentFile().getName().toLowerCase();

            logger.info("Processing file: " + filePath);

            if (fileName.endsWith("testscript.xlsx") && pattern.matcher(fileName).matches()) {
                logger.info("Valid test script found: " + fileName);

                if ("chrome".equals(parentDir) && !runInGrid) {
                    startRunnerProcess(filePath, logger, lock, objectRepoReader, utils, "chrome");
                } else if ("edge".equals(parentDir) && !runInGrid) {
                    startRunnerProcess(filePath, logger, lock, objectRepoReader, utils, "edge");
                } else if ("test_scripts".equals(parentDir) && !runInGrid) {
                    startRunnerProcess(filePath, logger, lock, objectRepoReader, utils, "");
                } else if ("grid_edge".equals(parentDir) && runInGrid) {
                    startRunnerProcess(filePath, logger, lock, objectRepoReader, utils, "edge");
                } else if ("grid_chrome".equals(parentDir) && runInGrid) {
                    startRunnerProcess(filePath, logger, lock, objectRepoReader, utils, "chrome");
                }
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.warning(utils.formatElapsedTime(elapsedTime));

        prm.generateTestSummaryPdf();
        // prm.generateSkippedTestSummaryPdf();
        utils.mergePdfsInParts();
        // utils.sendEmailWithAttachment();
        if (uploadTr) {
            utils.uploadFolderToFTP();
        }
    }

    public static void startRunnerProcess(String filePath, Logger logger, Lock lock, ConfigReader objectRepoReader,
            Utils utils, String browser) {

        try {
            // Start execution process
            Thread executionThread = startExecutionThread(filePath, logger, lock, objectRepoReader, utils, browser);
            executionThread.start();

            // Start recording process
            Thread recordingThread = startRecordingThread(executionThread,
                    Paths.get(filePath).getFileName().toString().replace("testscript.xlsx", ""), logger, utils);
            recordingThread.start();

            // Wait for both processes to complete
            executionThread.join();
            recordingThread.join();

            logger.info("Execution and recording completed successfully.");
        } catch (Exception e) {
            logger.severe("Error during execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Thread startExecutionThread(String testScriptFile, Logger logger, Lock lock,
            ConfigReader objectRepoReader, Utils utils, String launchBrowser) {
        Thread executionThread = new Thread(() -> {
            try {
                logger.info("Starting test script execution...");
                logger.info("Test script file: " + testScriptFile);
                logger.info("Launch browser parameter: " + launchBrowser);

                boolean runInSeleniumGrid = Config.RUN_IN_SELENIUM_GRID.equalsIgnoreCase("yes");
                boolean runInAppiumGrid = Config.RUN_IN_APPIUM_GRID.equalsIgnoreCase("yes");

                if (runInSeleniumGrid && runInAppiumGrid) {
                    logger.severe(
                            "Both 'run_in_appium_grid' and 'run_in_selenium_grid' are set to 'Yes'. Only one should be set to 'Yes'.");
                    throw new IllegalArgumentException(
                            "Only one grid execution mode should be enabled.");
                }

                logger.info("Initializing Excel Report Manager...");
                ExcelReportManager eReport = new ExcelReportManager(logger, lock);

                if (testScriptFile.endsWith("testscript.xlsx")) {
                    logger.info("Valid test script found: " + testScriptFile);
                    List<List<String>> df = validateTestScript(testScriptFile, lock, objectRepoReader, logger, utils,
                            launchBrowser);

                    int retryCount = 1;
                    logger.info("Instantiating Keyword Manager...");

                    long processId = Thread.currentThread().getId(); // Using thread ID instead
                    long timestamp = System.currentTimeMillis();
                    String uniqueId = "thread_" + processId + "_time_" + timestamp;
                    String baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString();
                    String tempDirectory = Paths.get(baseDir, "test_files", uniqueId).toString();

                    utils.createDirInPath(tempDirectory);

                    KeywordsManager km = new KeywordsManager(logger, tempDirectory, retryCount);
                    int retries = 0;
                    int maxRetries = getMaxRetries(logger);

                    while (retries <= maxRetries) {
                        km.updateRetryCount(retries + 1);
                        try {
                            executeTestScript(df, logger, km, objectRepoReader, utils, launchBrowser);
                            logger.info("Test script executed successfully: " + testScriptFile);
                            break;
                        } catch (Exception e) {
                            logger.severe("Test script failed on attempt " + retries + ". Error: " + e.getMessage());
                            if (retries >= maxRetries) {
                                logger.severe("Test script failed after " + (retries + 1) + " attempts.");
                                break;
                            } else {
                                km.closeBrowser();
                            }
                            retries++;
                        }
                    }

                    String loggedUserName = utils.getLoggedInUserName();
                    List<String> testResult = new ArrayList<>();
                    testResult.add(km.repoM.tcId);
                    testResult.add(km.repoM.testDescription);
                    testResult.add(km.repoM.overallStatusText);
                    testResult.add(km.repoM.osImgAlt + " " + km.repoM.browserImgAlt + " "
                            + km.repoM.browserVersion + " ( User: " + loggedUserName + " )");
                    testResult.add(km.repoM.executedDate);

                    logger.info("Adding row to test summary results.");
                    eReport.addRow(testResult);

                    logger.info("Closing test and generating test result PDF.");
                    km.geClose();
                    utils.removeEmptyDir(tempDirectory);
                }

            } catch (Exception e) {
                logger.severe("Error during test execution: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return executionThread;
    }

    private static int getMaxRetries(Logger logger) {
        int maxRetries = 0; // Default to 0
    
        try {
            maxRetries = Config.MAX_RETRIES;
        } catch (NumberFormatException e) {
            logger.severe("Invalid value for MAX_RETRIES in properties file. Defaulting to 0.");
            maxRetries = 0;
        }
    
        // Enforce the maximum limit of 2
        if (maxRetries > 2) {
            maxRetries = 2;
        }
    
        return maxRetries;
    }

    //////////////////// Executor Functions ////////////////////////////////////

    public static void executeTestScript(List<List<String>> df, Logger logger, KeywordsManager km, ConfigReader objectRepoReader, Utils utils, String launchBrowser) {
        logger.info("Starting test script execution.");

        // Define keyword dispatch mapping with `km` included
        Map<String, KeywordExecutor> keywordDispatch = new HashMap<>();
        keywordDispatch.put("hover_mouse", row -> hoverMouse(row, logger, km, objectRepoReader));
        keywordDispatch.put("drag_drop", row -> dragDrop(row, logger, km, objectRepoReader));
        keywordDispatch.put("open_browser", row -> openBrowser(row, logger, km, launchBrowser));
        keywordDispatch.put("wait_for_seconds", row -> waitForSeconds(row, logger, km));
        keywordDispatch.put("switch_to_default_content", row -> switchToDefaultContent(row, logger, km));
        keywordDispatch.put("switch_to_iframe", row -> switchToIframe(row, logger, km, objectRepoReader));
        keywordDispatch.put("tc_id", row -> tcId(row, logger, km));
        keywordDispatch.put("tc_desc", row -> tcDesc(row, logger, km));
        keywordDispatch.put("step", row -> step(row, logger, km));
        keywordDispatch.put("enter_url", row -> enterUrl(row, logger, km));
        keywordDispatch.put("type", row -> typeKeyword(row, logger, km, objectRepoReader, utils));
        keywordDispatch.put("check_element_enabled", row -> checkElementEnabled(row, logger, km, objectRepoReader));
        keywordDispatch.put("check_element_disabled", row -> checkElementDisabled(row, logger, km, objectRepoReader));
        keywordDispatch.put("check_element_displayed", row -> checkElementDisplayed(row, logger, km, objectRepoReader));
        keywordDispatch.put("verify_displayed_text", row -> verifyDisplayedText(row, logger, km, objectRepoReader));
        keywordDispatch.put("verify_file_downloaded", row -> verifyFileDownloaded(row, logger, km));
        keywordDispatch.put("click", row -> click(row, logger, km, objectRepoReader));
        keywordDispatch.put("js_click", row -> jsClick(row, logger, km, objectRepoReader));
        keywordDispatch.put("select_file", row -> selectFile(row, logger, km, objectRepoReader));
        keywordDispatch.put("upload_file", row -> uploadFile(row, logger, km, objectRepoReader));
        keywordDispatch.put("choose_date_from_datepicker", row -> chooseDateFromDatepicker(row, logger, km, objectRepoReader));
        keywordDispatch.put("check_radio_chk_selected", row -> checkRadioChkSelected(row, logger, km, objectRepoReader));
        keywordDispatch.put("check_radio_chk_not_selected", row -> checkRadioChkNotSelected(row, logger, km, objectRepoReader));
        keywordDispatch.put("check_page_accessibility", row -> checkPageAccessibility(row, logger, km));
        keywordDispatch.put("select_dropdown_by_value", row -> selectDropdownByValue(row, logger, km, objectRepoReader));
        keywordDispatch.put("select_dropdown_by_index", row -> selectDropdownByIndex(row, logger, km, objectRepoReader));
        keywordDispatch.put("select_dropdown_by_visible_text", row -> selectDropdownByVisibleText(row, logger, km, objectRepoReader));
        keywordDispatch.put("scroll_page", row -> scrollPage(row, logger, km));

        for (int index = 0; index < df.size(); index++) {
            List<String> row = df.get(index);
            String keyword = row.get(0).trim();

            logger.info("Processing row " + (index + 2) + ": Keyword='" + keyword + "'");

            try {
                if (keywordDispatch.containsKey(keyword)) {
                    keywordDispatch.get(keyword).execute(row);
                } else {
                    logger.warning("Keyword '" + keyword + "' at row " + (index + 2) + " is not implemented.");
                }
            } catch (Exception e) {
                logger.severe("Error executing keyword '" + keyword + "' at row " + (index + 2) + ": " + e.getMessage());
                throw new RuntimeException("Keyword execution failed.", e);
            }
        }

        logger.info("Test script execution completed successfully.");
    }

    @FunctionalInterface
    public interface KeywordExecutor {
        void execute(List<String> row) throws Exception;
    }

    public static void hoverMouse(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'hover_mouse' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geMouseHover(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void dragDrop(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorTypeS = "xpath"; // Default locator type for source element
        String locatorTypeD = "xpath"; // Default locator type for destination element
    
        String ddElementNameData = row.get(1).trim();
        String ddElementLocatorData = row.get(2).trim();
    
        // Split element names and locators
        String[] ddElementNameDataLst = ddElementNameData.split(";");
        String[] ddElementLocatorDataLst = ddElementLocatorData.split(";");
    
        // Determine locator type for source element
        if (ddElementLocatorDataLst[0].toLowerCase().contains("_css")) {
            locatorTypeS = "css";
        } else if (ddElementLocatorDataLst[0].toLowerCase().contains("_id")) {
            locatorTypeS = "id";
        }
    
        // Determine locator type for destination element
        if (ddElementLocatorDataLst[1].toLowerCase().contains("_css")) {
            locatorTypeD = "css";
        } else if (ddElementLocatorDataLst[1].toLowerCase().contains("_id")) {
            locatorTypeD = "id";
        }
    
        logger.info("Executing 'drag_drop' at row " + row);
    
        // Retrieve locator values from object repository
        String sourceLocator = objectRepoReader.getProperty(ddElementLocatorDataLst[0], "No");
        String destinationLocator = objectRepoReader.getProperty(ddElementLocatorDataLst[1], "No");
    
        // Perform drag and drop action
        km.geDragAndDrop(sourceLocator, locatorTypeS, destinationLocator, locatorTypeD, ddElementNameDataLst[0], ddElementNameDataLst[1]);
    }

    public static void checkPageAccessibility(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'check_page_accessibility' at row " + row);
    
        // Calls the accessibility compliance check method
        km.geIsPageAccessibilityCompliant();
    }

    public static void openBrowser(List<String> row, Logger logger, KeywordsManager km, String launchBrowser) {
        String browser = launchBrowser.isEmpty() ? row.get(3).trim() : launchBrowser;
        logger.info("Executing 'open_browser' at row " + row + ". Launching browser: " + browser);
        km.geOpenBrowser(browser);
    }

    public static void waitForSeconds(List<String> row, Logger logger, KeywordsManager km) {
        int waitTime = Integer.parseInt(row.get(3).trim());
        logger.info("Executing 'wait_for_seconds' at row " + row + ". Waiting for " + waitTime + " seconds.");
        km.geWaitForSeconds(waitTime);
    }

    public static void switchToDefaultContent(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'switch_to_default_content' at row " + row);
        km.geSwitchToDefaultContent();
    }

    public static void switchToIframe(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'switch_to_iframe' at row " + row);
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
        
        km.geSwitchToIframe(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void tcId(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'tc_id' at row " + row);
        km.geTcid(row.get(3).trim());
    }

    public static void tcDesc(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'tc_desc' at row " + row);
        km.geTcdesc(row.get(3).trim());
    }

    public static void step(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'step' at row " + row);
    
        // Creating Map<String, Object> to hold step data
        Map<String, Object> stepData = new HashMap<>();
        stepData.put("step", row.get(1).trim());
        stepData.put("result", row.get(2).trim());
    
        // Passing the map to geStep
        km.geStep(stepData);
    }
    
    public static void enterUrl(List<String> row, Logger logger, KeywordsManager km) {
        String url = row.get(3).trim();
        logger.info("Executing 'enter_url' at row " + row + ". URL: " + url);
        km.geEnterUrl(url);
    }

    public static void typeKeyword(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader, Utils utils) {
        String locatorType = "xpath"; // Default locator type
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'type' at row " + row);
    
        // Retrieve locator from object repository
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        // Prepare input value
        String inputValue = row.get(3).trim();
    
        // Execute the type action
        km.geType(resolvedLocator, locatorType, inputValue, row.get(1).trim());
    }

    public static void checkElementEnabled(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'check_element_enabled' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geIsElementEnabled(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void checkElementDisabled(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'check_element_disabled' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geIsElementDisabled(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void checkElementDisplayed(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'check_element_displayed' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geIsElementDisplayed(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void verifyDisplayedText(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'verify_displayed_text' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geVerifyDisplayedText(resolvedLocator, locatorType, row.get(3).trim(), row.get(1).trim());
    }
    
    public static void click(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'click' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geClick(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void scrollPage(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'scroll_page' at row " + row);
        km.geScrollPage(row.get(3).trim());
    }

    public static void selectDropdownByValue(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'select_dropdown_by_value' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geSelectDropdownByValue(resolvedLocator, locatorType, row.get(1).trim(), row.get(3).trim());
    }

    public static void selectDropdownByIndex(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'select_dropdown_by_index' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geSelectDropdownByIndex(resolvedLocator, locatorType, row.get(1).trim(), Integer.parseInt(row.get(3).trim()));
    }

    public static void selectDropdownByVisibleText(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'select_dropdown_by_visible_text' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geSelectDropdownByVisibleText(resolvedLocator, locatorType, row.get(1).trim(), row.get(3).trim());
    }

    public static void jsClick(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'js_click' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geJsClick(resolvedLocator, locatorType, row.get(1).trim());
    }

    public static void uploadFile(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'upload_file' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");

        String baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString();
    
        // Build file path dynamically
        String upFilePath = Paths.get(baseDir, "test_files", row.get(3).trim()).toString();
    
        km.geUploadFile(resolvedLocator, locatorType, upFilePath);
    }

    public static void selectFile(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'select_file' at row " + row);

        String baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString();
    
        // Build file path dynamically
        String upFilePath = Paths.get(baseDir, "test_files", row.get(3).trim()).toString();
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geSelectFile(resolvedLocator, locatorType, upFilePath, row.get(1).trim());
    }

    public static void chooseDateFromDatepicker(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        logger.info("Executing 'choose_date_from_datepicker' at row " + row);
    
        String whichCalendar = row.get(1).trim();
        String dateToChoose = row.get(3).trim();
        String[] locators = row.get(2).trim().split(";");
    
        Map<String, String> params = new HashMap<>();
        params.put("which_calendar", whichCalendar);
        params.put("date_to_choose", dateToChoose);
    
        if (whichCalendar.equalsIgnoreCase("a") && locators.length == 4) {
            String[] locatorTypes = determineLocatorTypes(locators);
    
            params.put("locator_mon_type", locatorTypes[0]);
            params.put("locator_pre_type", locatorTypes[1]);
            params.put("locator_nxt_type", locatorTypes[2]);
            params.put("locator_dt_lst_type", locatorTypes[3]);
    
            params.put("date_mon_txt_loc", objectRepoReader.getProperty(locators[0], "No"));
            params.put("date_pre_button_loc", objectRepoReader.getProperty(locators[1], "No"));
            params.put("date_nxt_button_loc", objectRepoReader.getProperty(locators[2], "No"));
            params.put("date_date_list_loc", objectRepoReader.getProperty(locators[3], "No"));
    
            params.put("locator_name", whichCalendar);
        } else if (whichCalendar.equalsIgnoreCase("b") && locators.length == 3) {
            String[] locatorTypes = determineLocatorTypes(locators);
    
            params.put("locator_mon_select_type", locatorTypes[0]);
            params.put("locator_yr_select_type", locatorTypes[1]);
            params.put("locator_dt_lst_type", locatorTypes[2]);
    
            params.put("date_mon_select_loc", objectRepoReader.getProperty(locators[0], "No"));
            params.put("date_yr_select_loc", objectRepoReader.getProperty(locators[1], "No"));
            params.put("date_date_list_loc", objectRepoReader.getProperty(locators[2], "No"));
    
            params.put("locator_name", whichCalendar);
        } else {
            throw new IllegalArgumentException("Invalid locator data '" + row.get(2) + "' for calendar type '" + whichCalendar + "'. Ensure correct number of locators.");
        }
    
        // Pass the constructed parameters map to geChooseDateFromDatePicker
        km.geChooseDateFromDatePicker(params);
    }
    
    private static String[] determineLocatorTypes(String[] locators) {
        String[] types = new String[locators.length];
        for (int i = 0; i < locators.length; i++) {
            types[i] = locators[i].toLowerCase().contains("_css") ? "css" 
                    : locators[i].toLowerCase().contains("_id") ? "id" 
                    : "xpath";
        }
        return types;
    }
    
    public static void checkRadioChkSelected(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'check_radio_chk_selected' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geIsChkRadioElementSelected(resolvedLocator, locatorType, row.get(1).trim());
    }
    
    public static void checkRadioChkNotSelected(List<String> row, Logger logger, KeywordsManager km, ConfigReader objectRepoReader) {
        String locatorType = "xpath";
        String locatorInput = row.get(2).trim().toLowerCase();
    
        if (locatorInput.contains("_css")) {
            locatorType = "css";
        } else if (locatorInput.contains("_id")) {
            locatorType = "id";
        }
    
        logger.info("Executing 'check_radio_chk_not_selected' at row " + row);
    
        String resolvedLocator = objectRepoReader.getProperty(locatorInput, "No");
    
        km.geIsChkRadioElementNotSelected(resolvedLocator, locatorType, row.get(1).trim());
    }
    
    public static void verifyFileDownloaded(List<String> row, Logger logger, KeywordsManager km) {
        logger.info("Executing 'verify_file_downloaded' at row " + row);
        
        String filePath = row.get(3).trim();
    
        km.geVerifyFileDownloaded(filePath);
    }
        

    //////////////////// End Executor Functions ////////////////////////////////

    /////////////////// Validate Test Script Functions /////////////////////////

    public static List<List<String>> validateTestScript(String testScriptFile, Lock lock, ConfigReader objectRepo,
            Logger logger, Utils utils, String launchBrowser) {
        ExcelReportManager exReport = new ExcelReportManager(logger, lock);
        logger.info("Validating the test script: " + testScriptFile);

        File testScriptFileObj = new File(testScriptFile);

        if (!utils.isExcelDoc(testScriptFileObj)) {
            logger.severe("The test script Excel file is not in the correct format.");
            exReport.addRowSkippedTC(
                    List.of(testScriptFile, "The test script Excel file is not in the correct format.", "Skipped"));
            throw new IllegalArgumentException("The test script Excel file is not in the correct format.");
        }

        List<List<String>> rows = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(new File(testScriptFile));
                Workbook workbook = WorkbookFactory.create(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            logger.info("Reading test script data from Excel.");

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(cell.toString().trim());
                }
                rows.add(rowData);
            }

            if (rows.isEmpty() || rows.get(0).isEmpty()) {
                logger.severe("The 'Keyword' column contains empty values.");
                exReport.addRowSkippedTC(List.of(testScriptFile, "The 'Keyword' column contains empty values.", "Skipped"));
                throw new IllegalArgumentException("The 'Keyword' column contains empty values.");
            }

            // Validate required first five rows (must-have keywords)
            validateSequenceKeywords(rows, testScriptFile, exReport, logger);

            // Validate each row
            for (int index = 0; index < rows.size(); index++) {
                String keyword = rows.get(index).get(0).trim();
                logger.info("Validating row " + (index + 2) + ": Keyword='" + keyword + "'");

                if (!Constants.VALID_KEYWORDS.contains(keyword)) {
                    logger.severe("Invalid keyword '" + keyword + "' at row " + (index + 2));
                    exReport.addRowSkippedTC(List.of(testScriptFile,
                            "Invalid keyword '" + keyword + "' at row " + (index + 2), "Skipped"));
                    throw new IllegalArgumentException("Invalid keyword '" + keyword + "' in the test script.");
                }

                validateKeywordSpecificRules(index, rows.get(index), testScriptFile, exReport, objectRepo, logger);
            }

        } catch (Exception e) {
            logger.severe("Error processing Excel file: " + e.getMessage());
            throw new RuntimeException("Error processing test script file.", e);
        }
        logger.info("Validation of test script file is successful.");
        return rows;
    }

    private static void validateSequenceKeywords(List<List<String>> rows, String testScriptFile,
            ExcelReportManager eReport, Logger logger) {
        String[] requiredKeywords = { "tc_id", "tc_desc", "step", "open_browser", "enter_url" };
        for (int i = 0; i < requiredKeywords.length; i++) {
            if (!rows.get(i).get(0).trim().equals(requiredKeywords[i])) {
                logger.severe("Mandatory keyword missing: " + requiredKeywords[i]);
                eReport.addRowSkippedTC(List.of(testScriptFile,
                        "The " + (i + 1) + "th keyword must be '" + requiredKeywords[i] + "'", "Skipped"));
                throw new IllegalArgumentException(
                        "The " + (i + 1) + "th keyword must be '" + requiredKeywords[i] + "'");
            }
        }
    }

    private static void validateKeywordSpecificRules(int index, List<String> rowData, String testScriptFile,
            ExcelReportManager eReport, ConfigReader objectRepo, Logger logger) {
        String keyword = rowData.get(0).trim();

        if (List.of("upload_file", "select_file", "type", "click", "verify_displayed_text",
                "choose_date_from_datepicker").contains(keyword)) {
            String locatorId = rowData.get(2).trim();
            if (locatorId.isEmpty()) {
                logger.severe("Locator ID missing at row " + (index + 2));
                eReport.addRowSkippedTC(
                        List.of(testScriptFile, "Locator ID missing at row " + (index + 2), "Skipped"));
                throw new IllegalArgumentException("Locator ID missing at row " + (index + 2));
            }

            if (objectRepo.getPropertyFromAnySection(locatorId, "No").equalsIgnoreCase("No")) {
                logger.severe("Locator does not exist in object repository at row " + (index + 2));
                eReport.addRowSkippedTC(List.of(testScriptFile,
                        "Locator does not exist in object repository at row " + (index + 2), "Skipped"));
                throw new IllegalArgumentException("Locator does not exist in object repository at row " + (index + 2));
            }
        }

        if (keyword.equals("enter_url")) {
            if (rowData.get(3).trim().isEmpty()) {
                logger.severe("Empty URL at row " + (index + 2));
                eReport.addRowSkippedTC(List.of(testScriptFile, "Empty URL at row " + (index + 2), "Skipped"));
                throw new IllegalArgumentException("Empty URL at row " + (index + 2));
            }
        }

        if (keyword.equals("choose_date_from_datepicker")) {
            validateChooseDateFromDatepicker(index, rowData, testScriptFile, eReport, objectRepo, logger, null);
        }

        if (keyword.equals("drag_drop")) {
            validateDragDrop(index, rowData, testScriptFile, eReport, logger);
        }

        if (keyword.equals("wait_for_seconds") && !rowData.get(3).trim().matches("\\d+")) {
            logger.severe("Invalid wait time at row " + (index + 2));
            eReport.addRowSkippedTC(List.of(testScriptFile, "Invalid wait time at row " + (index + 2), "Skipped"));
            throw new IllegalArgumentException("Invalid wait time at row " + (index + 2));
        }
    }

    private static void validateDragDrop(int index, List<String> rowData, String testScriptFile, ExcelReportManager skipEReport, Logger logger) {
        logger.info("Validating 'drag_drop' inputs at row " + (index + 2) + ".");
    
        String ddElementNameData = rowData.get(1).trim();
        String ddElementLocatorData = rowData.get(2).trim();
    
        List<String> ddElementNames = Arrays.stream(ddElementNameData.split(";"))
                                            .map(String::trim)
                                            .filter(name -> !name.isEmpty())
                                            .collect(Collectors.toList());
    
        List<String> ddElementLocators = Arrays.stream(ddElementLocatorData.split(";"))
                                               .map(String::trim)
                                               .filter(locator -> !locator.isEmpty())
                                               .collect(Collectors.toList());
    
        if (ddElementNames.size() != 2) {
            logger.severe("Invalid 'Input1' data '" + ddElementNameData + "' for 'drag_drop' at row " + (index + 2));
            skipEReport.addRowSkippedTC(List.of(testScriptFile, "'Input1' for 'drag_drop' must contain exactly 2 values separated by a semicolon (';').", "Skipped"));
            throw new IllegalArgumentException("'Input1' for 'drag_drop' must contain exactly 2 values separated by a semicolon (';').");
        }
    
        if (ddElementLocators.size() != 2) {
            logger.severe("Invalid 'Input2' data '" + ddElementLocatorData + "' for 'drag_drop' at row " + (index + 2));
            skipEReport.addRowSkippedTC(List.of(testScriptFile, "'Input2' for 'drag_drop' must contain exactly 2 values separated by a semicolon (';').", "Skipped"));
            throw new IllegalArgumentException("'Input2' for 'drag_drop' must contain exactly 2 values separated by a semicolon (';').");
        }
    
        logger.info("'drag_drop' validation passed successfully at row " + (index + 2) + ".");
    }

    private static void validateChooseDateFromDatepicker(int index, List<String> rowData, String testScriptFile, ExcelReportManager eReport, ConfigReader objectRepo, Logger logger, Utils utils) {
        String calendarType = rowData.get(1).trim();
        String cdLoc = rowData.get(2).trim();
        List<String> cdLids = Arrays.stream(cdLoc.split(";"))
                                    .map(String::trim)
                                    .filter(locator -> !locator.isEmpty())
                                    .collect(Collectors.toList());

        if (cdLids.isEmpty()) {
            logger.severe("No valid locator IDs provided at row " + (index + 2));
            eReport.addRowSkippedTC(List.of(testScriptFile, "No valid locator IDs provided at row " + (index + 2), "Skipped"));
            throw new IllegalArgumentException("No valid locator IDs provided at row " + (index + 2));
        }

        logger.info("Validating date format for calendar '" + calendarType + "' at row " + (index + 2));

        if ("a".equals(calendarType)) {
            if (cdLids.size() != 4) {
                logger.severe("Number of locator IDs at row " + (index + 2) + " is not 4. Expected: 'date_mon_txt_xpath;date_pre_button_xpath;date_nxt_button_xpath;date_date_list_xpath'.");
                eReport.addRowSkippedTC(List.of(testScriptFile, "Number of locator IDs is not 4.", "Skipped"));
                throw new IllegalArgumentException("Number of locator IDs is not 4.");
            }

            if (!utils.isDateFormatValid(rowData.get(3).trim())) {
                logger.severe("Invalid date format '" + rowData.get(3) + "' for calendar type 'a' at row " + (index + 2) + ". Expected format: '01 December 2022'.");
                eReport.addRowSkippedTC(List.of(testScriptFile, "Invalid date format.", "Skipped"));
                throw new IllegalArgumentException("Invalid date format.");
            }

            validateLocatorSuffixes(cdLids, index, testScriptFile, eReport, logger);
        } else if ("b".equals(calendarType)) {
            if (cdLids.size() != 3) {
                logger.severe("Number of locator IDs at row " + (index + 2) + " is not 3. Expected: 'date_mon_select_xpath;date_yr_select_xpath;date_date_list_xpath'.");
                eReport.addRowSkippedTC(List.of(testScriptFile, "Number of locator IDs is not 3.", "Skipped"));
                throw new IllegalArgumentException("Number of locator IDs is not 3.");
            }

            if (!utils.isDateFormatValid(rowData.get(3).trim())) {
                logger.severe("Invalid date format '" + rowData.get(3) + "' for calendar type 'b' at row " + (index + 2) + ". Expected format: '01 December 2022'.");
                eReport.addRowSkippedTC(List.of(testScriptFile, "Invalid date format.", "Skipped"));
                throw new IllegalArgumentException("Invalid date format.");
            }

            validateLocatorSuffixes(cdLids, index, testScriptFile, eReport, logger);
        }
    }

    private static void validateLocatorSuffixes(List<String> cdLids, int index, String testScriptFile, ExcelReportManager eReport, Logger logger) {
        List<String> validSuffixes = List.of("_css", "_id", "_xpath");

        if (cdLids.stream().allMatch(locator -> validSuffixes.stream().anyMatch(locator::endsWith))) {
            logger.info("All locators at row " + (index + 2) + " end with valid suffixes.");
        } else {
            logger.severe("Not all locators at row " + (index + 2) + " have valid suffixes.");
            eReport.addRowSkippedTC(List.of(testScriptFile, "Invalid locator suffixes.", "Skipped"));
            throw new IllegalArgumentException("Invalid locator suffixes.");
        }
    }

    //////////////////////////// End Validate test script Funtions
    //////////////////////////// //////////////////////////////

    //////////////////////////////////////// Recording functions
    //////////////////////////////////////// /////////////////////////////////////

    public static Thread startRecordingThread(Thread executionThread, String recordName, Logger logger, Utils utils) {
        Thread recordingThread = new Thread(() -> {
            try {
                logger.info("Starting screen recording...");

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int width = screenSize.width;
                int height = screenSize.height;
                int fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
                int fps = 60;

                String outputPath = Paths.get(utils.getTestRecordingsFolder(),
                        recordName + "_" + utils.getDatetimeString() + ".mp4").toString();

                VideoWriter videoWriter = new VideoWriter(outputPath, fourcc, fps,
                        new org.opencv.core.Size(width, height), true);

                if (!videoWriter.isOpened()) {
                    logger.severe("Failed to initialize video writer.");
                    return;
                }

                while (executionThread.isAlive()) {
                    BufferedImage screenshot = new Robot().createScreenCapture(new Rectangle(screenSize));
                    Mat frame = bufferedImageToMat(screenshot);
                    videoWriter.write(frame);
                    TimeUnit.MILLISECONDS.sleep(16); // Approximate delay for ~60 FPS
                }

                videoWriter.release();
                logger.info("Finished recording.");
            } catch (Exception e) {
                logger.severe("Error during recording: " + e.getMessage());
            }
        });

        return recordingThread; // Now returning the actual Thread object
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                byte[] data = new byte[] { (byte) (rgb & 0xFF), (byte) ((rgb >> 8) & 0xFF),
                        (byte) ((rgb >> 16) & 0xFF) };
                mat.put(y, x, data);
            }
        }
        return mat;
    }

    //////////////////////////////////////// End Recording functions
    //////////////////////////////////////// /////////////////////////////////////

}
