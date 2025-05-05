package com.waf;

import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class DriverFunctions extends DriverManager {

    private Utils utils;
    private final Logger logger;

    public DriverFunctions(String tempDir) {
        super(tempDir);
        this.utils = Utils.getInstance();
        this.logger = LogManager.getLogger(DriverFunctions.class);
    }

    public DriverFunctions() {
        super();
        this.utils = Utils.getInstance();
        this.logger = LogManager.getLogger(DriverFunctions.class);
    }

    public void closeBrowser() {
        try {
            logger.info("Closing Browser");
            driver.quit();
            logger.info("Closing Browser SUCCESSFUL");
        } catch (Exception e) {
            logger.info(e.toString());
        }
    }

    public String getBrowserVersion() {
        logger.info("Capturing Browser Version");
        try {
            RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver; // Cast to RemoteWebDriver
            String browserVersion = remoteWebDriver.getCapabilities().getBrowserVersion();
            return browserVersion;
        } catch (Exception e) {
            logger.info(e.toString());
            return "";
        }
    }

    public void openUrl(String url) {
        logger.info("Opening URL " + url);
        driver.get(url);
    }

    public void scrollIntoViewTop(String locator, String locatorType) {
        WebElement element = null;
        try {
            if (locator != null) {
                // If a locator is provided, find the element using the getElement method
                element = getElement(locator, locatorType);
            }
            // Use JavaScript Executor to scroll to the element
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", element);
            logger.info("Scrolling to element with locator: " + locator + " locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error if scrolling fails and rethrow the exception
            logger.error("Cannot send data on the element with locator: " + locator + " locatorType: " + locatorType);
            throw e;
        }
    }

    public void scrollIntoView(String locator, String locatorType) {
        WebElement element = null;
        try {
            if (locator != null) {
                // If a locator is provided, find the element using the getElement method
                element = getElement(locator, locatorType);
            }
            // Use JavaScript Executor to scroll to the element with smooth behavior and
            // centered block
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            logger.info("Scrolling to element with locator: " + locator + " locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error if scrolling fails and rethrow the exception
            logger.error("Cannot send data on the element with locator: " + locator + " locatorType: " + locatorType);
            throw e;
        }
    }

    public String takeScreenshot(String fileStr) {
        try {
            if (isHeadless || isRunningGrid) {
                return takeScreenshotWithBase64Watermark(fileStr);
            } else {
                return utils.takeScreenshotFullSrcTag(fileStr); // Assuming similar functionality exists in Utils class
            }
        } catch (Exception e) {
            logger.error("### Exception Occurred when taking screenshot: " + e.getMessage());
            return "";
        }
    }

    public String takeScreenshotWithBase64Watermark(String inputString) {
        try {
            // Define the base folder
            String baseFolder = utils.getImagesFolder();

            // Extract folder structure and file name from input string
            String[] parts = inputString.split("_");
            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "Input string must contain at least two parts separated by underscores.");
            }

            String folderPath = baseFolder + File.separator + parts[0] + File.separator + parts[1];
            String fileName = inputString + ".png";
            String filePath = folderPath + File.separator + fileName;

            // Create folder structure if it doesn't exist
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Capture screenshot as binary data
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage image = ImageIO.read(screenshotFile);

            // Add the date-time watermark
            String dateTimeStr = utils.getDatetimeString();
            Graphics2D graphics = (Graphics2D) image.getGraphics();

            // Define font and calculate text position
            Font font = new Font("Arial", Font.PLAIN, 12); // Example font, modify as needed
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics(font);
            int textWidth = metrics.stringWidth(dateTimeStr);
            int textHeight = metrics.getHeight();

            int width = image.getWidth();
            int height = image.getHeight();
            int padding = 10;

            // Draw transparent rectangle as background for the watermark
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // Transparency
            graphics.setColor(Color.BLACK);
            graphics.fillRect(width - textWidth - 2 * padding, padding, textWidth + 2 * padding,
                    textHeight + 2 * padding);

            // Draw watermark text
            graphics.setColor(Color.WHITE);
            graphics.drawString(dateTimeStr, width - textWidth - padding, padding + textHeight);

            // Save the watermarked image to the specified folder structure
            ImageIO.write(image, "png", new File(filePath));

            // Encode the image into Base64 format
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Return the Base64 string
            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            logger.error("### Exception Occurred when taking screenshot with Base64 watermark: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getTitle() {
        try {
            logger.info("Getting the page title");
            // Retrieve and return the title of the current browser page
            return driver.getTitle();
        } catch (Exception e) {
            // Log an error if unable to fetch the title
            logger.error("### Exception Occurred when getting page title: " + e.getMessage());
            throw e;
        }
    }

    public void dropdownSelectElement(String locator, String locatorType, String selector, String selectorType) {
        try {
            // Locate the dropdown element
            WebElement element = getElement(locator, locatorType);
            Select dropdown = new Select(element); // Create a Select instance using the located element

            switch (selectorType.toLowerCase()) {
                case "value":
                    dropdown.selectByValue(selector); // Select dropdown option by value
                    Thread.sleep(1000); // Pause for 1 second to simulate delay
                    break;

                case "index":
                    dropdown.selectByIndex(Integer.parseInt(selector)); // Select dropdown option by index
                    Thread.sleep(1000);
                    break;

                case "text":
                    dropdown.selectByVisibleText(selector); // Select dropdown option by visible text
                    Thread.sleep(1000);
                    break;

                default:
                    logger.warn("Selector type not supported: " + selectorType); // Log unsupported selector type
                    throw new IllegalArgumentException("Unsupported selector type: " + selectorType);
            }

            logger.info("Element selected with selector: " + selector + " and selectorType: " + selectorType);
        } catch (Exception e) {
            // Log and rethrow exception if selection fails
            logger.error("Element not selected with selector: " + selector + " and selectorType: " + selectorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public int getDropdownOptionsCount(String locator, String locatorType) {
        try {
            // Locate the dropdown element
            WebElement element = getElement(locator, locatorType);
            Select dropdown = new Select(element); // Create a Select instance
            int optionsCount = dropdown.getOptions().size(); // Get the count of options
            logger.info("Element found with locator: " + locator + " and locatorType: " + locatorType);
            return optionsCount;
        } catch (Exception e) {
            // Log error if element not found and rethrow exception
            logger.error("Element not found with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getDropdownSelectedOptionValue(String locator, String locatorType) {
        try {
            // Locate the dropdown element
            WebElement element = getElement(locator, locatorType);
            Select dropdown = new Select(element); // Create a Select instance
            String selectedOptionValue = dropdown.getFirstSelectedOption().getDomAttribute("value"); 
            logger.info("Return the selected option value of dropdown list with locator: " + locator
                    + " and locatorType: " + locatorType);
            return selectedOptionValue;
        } catch (Exception e) {
            // Log error if unable to retrieve the selected option value
            logger.error("Cannot return the selected option value of dropdown list with locator: " + locator
                    + " and locatorType: " + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getDropdownSelectedOptionText(String locator, String locatorType) {
        try {
            // Locate the dropdown element
            WebElement element = getElement(locator, locatorType);
            Select dropdown = new Select(element); // Create a Select instance
            String selectedOptionText = dropdown.getFirstSelectedOption().getText(); // Get the text of the selected
                                                                                     // option
            logger.info("Return the selected option of dropdown list with locator: " + locator + " and locatorType: "
                    + locatorType);
            return selectedOptionText;
        } catch (Exception e) {
            // Log error if unable to retrieve the selected option
            logger.error("Cannot return the selected option of dropdown list with locator: " + locator
                    + " and locatorType: " + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isElementSelected(String locator, String locatorType) {
        try {
            // Locate the web element
            WebElement element = getElement(locator, locatorType);
            boolean isSelected = element.isSelected(); // Check if the element is selected
            logger.info("Element found with locator: " + locator + " and locatorType: " + locatorType);
            return isSelected;
        } catch (Exception e) {
            // Log error if the element is not found
            logger.error("Element not found with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<WebElement> getElementList(String locator, String locatorType) {
        try {
            // Convert locatorType to lowercase
            locatorType = locatorType.toLowerCase();
            By byType = getByType(locatorType, locator); // Determine the locator type

            // Retrieve a list of web elements using the locator and locatorType
            List<WebElement> elements = driver.findElements(byType);
            logger.info("Element list found with locator: " + locator + " and locatorType: " + locatorType);
            return elements;
        } catch (Exception e) {
            // Log error if the elements could not be found
            logger.error("Element list not found with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void elementClick(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the element using the getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            } else {
                throw new ElementNotInteractableException(locator);
            }
            // Perform the click action
            element.click();
            logger.info("Clicked on element with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error and rethrow exception if the click fails
            logger.error("Cannot click on the element with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void elementJsClick(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the element using the getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }
            // Perform the click action using JavaScript Executor
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            logger.info("Clicked on element using JavaScript with locator: " + locator + " and locatorType: "
                    + locatorType);
        } catch (Exception e) {
            // Log error and rethrow exception if the JavaScript click fails
            logger.error("Cannot click on the element using JavaScript with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void elementJsClick(WebElement element) {
        try {
            // Perform the click action using JavaScript Executor
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            logger.info("Clicked on element using JavaScript");
        } catch (Exception e) {
            // Log error and rethrow exception if the JavaScript click fails
            logger.error("Cannot click on the element using JavaScript - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void elementHover(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the element using getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }
            // Perform hover action using Actions class
            Actions action = new Actions(driver);
            action.moveToElement(element).perform();
            Thread.sleep(2000); // Pause for 2 seconds as in Python logic
            logger.info("Hovered over element with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error and rethrow exception if hover fails
            logger.error("Cannot hover over the element with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void sendKeys(String data, String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the element using getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            } else {
                throw new ElementNotInteractableException(locator);
            }
            // Send keys to the element
            element.sendKeys(data);
            logger.info("Sent data to element with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error and rethrow exception if send keys fails
            logger.error("Cannot send data to the element with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void clearKeys(String locator, String locatorType, WebElement element) {
        try {
            // If a locator is provided, find the element using getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }
            // Clear the text of the element
            element.clear();
            logger.info("Cleared data of element with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error and rethrow exception if clear fails
            logger.error("Cannot clear data of the element with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getText(String locator, String locatorType, String elementName) {
        WebElement element = null;
        try {
            // If a locator is provided, find the element using getElement method
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            } else {
                throw new ElementNotInteractableException(locator);
            }
            logger.info("Before retrieving text from the element.");
            String text = element.getText(); // Retrieve visible text
            logger.info("After retrieving text, size is: " + text.length());

            // If the text is empty, retrieve innerText attribute
            if (text.isEmpty()) {
                text = element.getDomAttribute("innerText");
            }

            // If text exists, log it
            if (!text.isEmpty()) {
                logger.info("Retrieved text on element :: " + elementName);
                logger.info("The text is :: '" + text + "'");
                text = text.trim(); // Trim whitespace
            }
            return text;
        } catch (Exception e) {
            // Log error and rethrow exception if text retrieval fails
            logger.error("Failed to get text on element " + elementName + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void switchToIframe(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the iframe element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }

            // Switch to the default content first, then switch to the specified iframe
            driver.switchTo().defaultContent();
            driver.switchTo().frame(element);

            logger.info("Switched to iframe with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error if unable to switch to iframe
            logger.error("Unable to switch to iframe with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void switchToDefaultContent() {
        try {
            // Switch back to the default content of the page
            driver.switchTo().defaultContent();
            logger.info("Switched to default content.");
        } catch (Exception e) {
            // Log error if unable to switch to default content
            logger.error("Unable to switch to default content - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isElementPresent(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }

            if (element != null) {
                logger.info("Element found with locator: " + locator + " and locatorType: " + locatorType);
                return true;
            } else {
                logger.warn("Element not found with locator: " + locator + " and locatorType: " + locatorType);
                return false;
            }
        } catch (Exception e) {
            // Log error if unable to check element presence
            logger.error("Element not found with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            return false;
        }
    }

    public void applyStyle(String style, WebElement element) {
        try {
            // Apply the given style to the web element using JavaScript Executor
            ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute('style', arguments[1]);", element,
                    style);
            logger.info("Style applied to element: " + style);
        } catch (Exception e) {
            // Log error if unable to apply style
            logger.error("Unable to apply style to element - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void highlightElement(int effectTime, String color, int border, String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                logger.info("In locator condition.");
                element = getElement(locator, locatorType);
            } else {
                throw new ElementNotInteractableException(locator);
            }

            // Retrieve the original style of the element
            String originalStyle = element.getDomAttribute("style");

            // Apply the highlight style
            applyStyle("border: " + border + "px solid " + color + ";", element);
            Thread.sleep(effectTime * 1000); // Convert seconds to milliseconds

            // Revert to the original style
            applyStyle(originalStyle, element);
            logger.info("Highlighted element with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log error if unable to highlight element
            logger.error("Cannot highlight element with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void dragAndDrop(String sourceLocator, String sourceLocatorType, String targetLocator,
            String targetLocatorType) {
        try {
            // Locate the source and target elements
            WebElement sourceElement = getElement(sourceLocator, sourceLocatorType);
            WebElement targetElement = getElement(targetLocator, targetLocatorType);

            // Perform the drag-and-drop action using Actions class
            Actions actions = new Actions(driver);
            actions.dragAndDrop(sourceElement, targetElement).perform();

            logger.info("Successfully performed drag-and-drop from source with locator: " + sourceLocator +
                    " and sourceLocatorType: " + sourceLocatorType + " to target with locator: " + targetLocator +
                    " and targetLocatorType: " + targetLocatorType);
        } catch (Exception e) {
            // Log error if the drag-and-drop action fails
            logger.error("Failed to perform drag-and-drop from source with locator: " + sourceLocator +
                    " and sourceLocatorType: " + sourceLocatorType + " to target with locator: " + targetLocator +
                    " and targetLocatorType: " + targetLocatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isElementDisplayed(String locator, String locatorType, WebElement element) {
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }

            if (element != null && element.isDisplayed()) {
                logger.info("Element is displayed with locator: " + locator + " and locatorType: " + locatorType);
                return true;
            } else {
                logger.warn(
                        "Element is not displayed with locator: " + locator + " and locatorType: " + locatorType);
                return false;
            }
        } catch (Exception e) {
            // Log error if the display check fails
            logger.error("Element is not displayed with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            return false;
        }
    }

    public boolean isElementEnabled(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }

            if (element != null && element.isEnabled()) {
                logger.info("Element with locator: " + locator + " and locatorType: " + locatorType + " is enabled.");
                return true;
            } else if (element != null) {
                logger.warn(
                        "Element with locator: " + locator + " and locatorType: " + locatorType + " is disabled.");
                return false;
            } else {
                logger.warn(
                        "Element is not displayed with locator: " + locator + " and locatorType: " + locatorType);
                return false;
            }
        } catch (Exception e) {
            // Log error if the enable check fails
            logger.error("Element is not displayed with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            return false;
        }
    }

    public boolean isChkRadioElementSelected(String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }
            if (element != null && element.isSelected()) {
                logger.info("Element with locator: " + locator + " and locatorType: " + locatorType + " is selected.");
                return true;
            } else if (element != null) {
                logger.info(
                        "Element with locator: " + locator + " and locatorType: " + locatorType + " is NOT selected.");
                return false;
            } else {
                logger.warn(
                        "Element is not displayed with locator: " + locator + " and locatorType: " + locatorType);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking if element is selected with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            return false;
        }
    }

    public boolean elementPresenceCheck(String locator, String locatorType) {
        try {
            // Convert locatorType to lowercase and retrieve By object
            By byType = getByType(locatorType, locator);

            // Find elements matching the locator
            List<WebElement> elementList = driver.findElements(byType);
            if (!elementList.isEmpty()) {
                logger.info("Element found with locator: " + locator + " and locatorType: " + locatorType);
                return true;
            } else {
                logger.info("Element not found with locator: " + locator + " and locatorType: " + locatorType);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking element presence with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            return false;
        }
    }

    public WebElement waitForElement(String locator, String locatorType) {
        int timeout = 2;
        double pollFrequency = 1;
        try {
            logger.info("Waiting for maximum of " + timeout + " seconds for the element to be clickable.");

            // Define explicit wait with custom timeout and polling frequency
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
            wait.pollingEvery(Duration.ofMillis((long) (pollFrequency * 1000)));
            wait.ignoring(NoSuchElementException.class).ignoring(ElementNotInteractableException.class)
                    .ignoring(ElementNotInteractableException.class);

            // Retrieve the By object for the locator
            By byType = getByType(locatorType, locator);

            // Wait until the element is clickable
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(byType));

            logger.info("Element appeared on the web page.");
            return element;
        } catch (Exception e) {
            logger.error("Element not appeared on the web page with locator: " + locator + " and locatorType: "
                    + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void webScroll(String direction) {
        try {
            if (direction.equalsIgnoreCase("up")) {
                // Scroll upwards
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -1500);");
                logger.info("Scrolled up the webpage.");
            } else if (direction.equalsIgnoreCase("down")) {
                // Scroll downwards
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1500);");
                logger.info("Scrolled down the webpage.");
            } else {
                logger.warn("Invalid scroll direction provided: " + direction);
            }
        } catch (Exception e) {
            logger.error("Failed to scroll the webpage in direction: " + direction + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getUrl() {
        try {
            // Retrieve the current URL of the webpage
            String currentUrl = driver.getCurrentUrl();
            logger.info("Current URL retrieved: " + currentUrl);
            return currentUrl;
        } catch (Exception e) {
            logger.error("Failed to retrieve the current URL - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void pageBack() {
        try {
            // Navigate to the previous page in browser history
            ((JavascriptExecutor) driver).executeScript("window.history.go(-1);");
            logger.info("Navigated back in the browser's history.");
        } catch (Exception e) {
            logger.error("Failed to navigate back in the browser's history - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getAttributeValue(String locator, String locatorType, WebElement element, String attribute) {
        try {
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            }
            // Retrieve the value of the specified attribute
            String attributeValue = element.getDomAttribute(attribute);
            logger.info("Attribute value retrieved: " + attributeValue);
            return attributeValue;
        } catch (Exception e) {
            logger.error("Failed to get attribute '" + attribute + "' for element with locator: " + locator +
                    " and locatorType: " + locatorType + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void refresh() {
        try {
            // Refresh the current webpage
            driver.navigate().refresh();
            logger.info("Refreshed the current webpage.");
        } catch (Exception e) {
            logger.error("Failed to refresh the current webpage - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void waitForSomeTime(int secToWait) {
        try {
            // Pause execution for the specified number of seconds
            Thread.sleep(secToWait * 1000); // Convert seconds to milliseconds
            logger.info("Paused execution for " + secToWait + " seconds.");
        } catch (InterruptedException e) {
            // Handle InterruptedException
            logger.error("Failed to pause execution for " + secToWait + " seconds - Error: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }

    public void fileNameToSelect(String fileName) {
        try {
            Robot robot = new Robot();

            // Type the file name
            for (char c : fileName.toCharArray()) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                if (KeyEvent.CHAR_UNDEFINED == keyCode) {
                    throw new RuntimeException("Key code not found for character: " + c);
                }
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            }

            // Simulate 'Tab' and 'Enter' key presses
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);

            logger.info("File name selected: " + fileName);
        } catch (Exception e) {
            // Handle exceptions
            logger.error("Failed to select file name: " + fileName + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void fileNameToUpload(String fileName, String locator, String locatorType) {
        WebElement element = null;
        try {
            // If a locator is provided, find the web element
            if (locator != null && !locator.isEmpty()) {
                element = getElement(locator, locatorType);
            } else {
                throw new ElementNotInteractableException(locator);
            }

            // Simulate uploading the file by sending the file path to the input element
            element.sendKeys(fileName);
            logger.info("File uploaded with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Handle exceptions
            logger.error("Failed to upload file with locator: " + locator + " and locatorType: " + locatorType
                    + " - Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public WebElement getElement(String locator, String locatorType) {
        WebElement element = null;

        try {
            // Convert locatorType to lowercase
            locatorType = locatorType.toLowerCase();

            // Use the getByType function to determine the locator type
            By byType = getByType(locatorType, locator);

            // Find the WebElement using the locator and its type
            element = driver.findElement(byType);
            logger.info("Element found with locator: " + locator + " and locatorType: " + locatorType);
        } catch (Exception e) {
            // Log an error if the element cannot be found
            logger.info(e.toString());
            throw e; // Rethrow the exception to handle it as needed
        }

        return element; // Return the located WebElement
    }

    public By getByType(String locatorType, String locatorValue) {
        //locatorValue = locatorValue.toLowerCase();

        switch (locatorType.toLowerCase()) {
            case "id":
                return By.id(locatorValue);
            case "name":
                return By.name(locatorValue);
            case "xpath":
                return By.xpath(locatorValue);
            case "css":
                return By.cssSelector(locatorValue);
            case "class":
                return By.className(locatorValue);
            case "link":
                return By.linkText(locatorValue);
            default:
                logger.info("Locator type " + locatorValue + " not correct/supported");
                return null; // Returning null instead of false, following Java conventions
        }
    }
}
