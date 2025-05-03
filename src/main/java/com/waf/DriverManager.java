package com.waf;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.waf.config.Config;

import org.openqa.selenium.Dimension;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import org.json.JSONArray;
import org.json.JSONObject;

public class DriverManager {
    /**
     * Manages and instantiates WebDriver instances with configurations for browser
     * settings.
     */

    private Logger logger;
    private String tempDir;
    protected WebDriver driver;
    private boolean isInPrivate;
    protected boolean isHeadless;
    protected boolean isRunningGrid;
    private String gridUrl;
    private String appiumUrl;
    protected String gridOsInfo;

    public DriverManager(Logger logger, String tempDir) {
        /**
         * Initializes a new instance of the DriverManager class with configuration
         * settings.
         */
        this.logger = logger;
        this.tempDir = tempDir;
        this.isInPrivate = isBrowserInPrivate();
        this.isHeadless = isBrowserHeadless();
        this.isRunningGrid = isRunningGrid();
        this.gridUrl = Config.GRID_URL;
        this.appiumUrl = Config.APPIUM_URL;
        this.gridOsInfo = "";
    }

    public DriverManager(Logger logger) {
        /**
         * Initializes a new instance of the DriverManager class with configuration
         * settings.
         */
        this.logger = logger;
        this.tempDir = "";
        this.isInPrivate = isBrowserInPrivate();
        this.isHeadless = isBrowserHeadless();
        this.isRunningGrid = isRunningGrid();
        this.gridUrl = Config.GRID_URL;
        this.appiumUrl = Config.APPIUM_URL;
        this.gridOsInfo = "";
    }

    private boolean isBrowserInPrivate() {
        /**
         * Retrieves the browser's private mode setting from the Config class.
         * 
         * @return true if the browser is configured to run in private mode.
         */
        return Config.INPRIVATE.equalsIgnoreCase("yes");
    }

    private boolean isBrowserHeadless() {
        /**
         * Retrieves the browser's headless mode setting from the Config class.
         * 
         * @return true if the browser is configured to run in headless mode.
         */
        return Config.HEADLESS.equalsIgnoreCase("yes");
    }

    private boolean isRunningGrid() {
        /**
         * Retrieves the Selenium Grid execution preference from the Config class.
         * 
         * @return true if the Selenium Grid execution is enabled.
         */
        return Config.RUN_IN_SELENIUM_GRID.equalsIgnoreCase("yes");
    }

    public void launchBrowser(String browserName) {
        /**
         * Launches a web browser based on the provided browser name and configuration
         * settings.
         * 
         * @param browserName The name of the browser to launch.
         */
        logger.info("Temporary download directory: " + tempDir);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", tempDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);

        if (isRunningGrid) {
            if ("chrome".equalsIgnoreCase(browserName)) {
                ChromeOptions options = new ChromeOptions();
                options.setExperimentalOption("prefs", prefs);
                if (isInPrivate)
                    options.addArguments("--incognito");
                if (isHeadless)
                    options.addArguments("--headless");
                try {
                    driver = new RemoteWebDriver(new URL(gridUrl), options);
                } catch (Exception e) {
                    e.printStackTrace(); // Handle the exception here
                }

                driver.manage().window().maximize();
                gridOsInfo = fetchSeleniumGridStatus(logger);
            }
            if ("edge".equalsIgnoreCase(browserName)) {
                EdgeOptions options = new EdgeOptions();
                options.setExperimentalOption("prefs", prefs); // Setting preferences
                if (isInPrivate)
                    options.addArguments("--inprivate"); // Private browsing
                if (isHeadless)
                    options.addArguments("--headless"); // Headless mode

                try {
                    // Initializing Remote WebDriver for Edge
                    driver = new RemoteWebDriver(new URL(gridUrl), options);
                } catch (Exception e) {
                    e.printStackTrace(); // Handle exceptions related to driver initialization
                }

                // Maximize the browser window
                driver.manage().window().maximize();

                // Fetch Selenium Grid status
                gridOsInfo = fetchSeleniumGridStatus(logger);
            }
        } else {
            switch (browserName.toLowerCase()) {
                case "chrome":
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.setExperimentalOption("prefs", prefs);
                    if (isInPrivate)
                        chromeOptions.addArguments("--incognito");
                    if (isHeadless)
                        chromeOptions.addArguments("--headless");
                    driver = new ChromeDriver(chromeOptions);
                    driver.manage().window().setSize(new Dimension(1920, 1080));
                    break;

                case "edge":
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setExperimentalOption("prefs", prefs);
                    if (isInPrivate)
                        edgeOptions.addArguments("--inprivate");
                    if (isHeadless)
                        edgeOptions.addArguments("--headless");
                    driver = new EdgeDriver(edgeOptions);
                    driver.manage().window().setSize(new Dimension(1920, 1080));
                    break;

                case "firefox":
                    driver = new FirefoxDriver();
                    driver.manage().window().maximize();
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported browser: " + browserName);
            }
        }
    }

    public String fetchSeleniumGridStatus(Logger logger) {
        try {
            // Create a connection to the Selenium Grid status URL
            URL url = new URL(Config.GRID_URL + "/status");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Check if the response code indicates success
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON response
                JSONObject gridStatus = new JSONObject(response.toString());
                JSONArray nodes = gridStatus.getJSONObject("value").getJSONArray("nodes");

                String osInfo = "";

                for (int i = 0; i < nodes.length(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    osInfo = node.getJSONObject("osInfo").optString("name", "Unknown OS");
                    String osVersion = node.getJSONObject("osInfo").optString("version", "Unknown Version");
                    String osArch = node.getJSONObject("osInfo").optString("arch", "Unknown Architecture");
                    this.gridOsInfo = osInfo;

                    // Log the node information
                    logger.info("Node OS: " + osInfo + ", Version: " + osVersion + ", Architecture: " + osArch);
                }
                return osInfo;
            } else {
                logger.info("Failed to fetch Selenium Grid status. HTTP response code: " + responseCode);
                return "Unknown Version";
            }
        } catch (Exception e) {
            logger.info("Failed to fetch Selenium Grid status: " + e.getMessage());
            return "Unknown Version";
        }
    }
}
