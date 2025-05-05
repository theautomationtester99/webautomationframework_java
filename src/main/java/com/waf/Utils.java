package com.waf;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Base64;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.waf.config.Config;

public class Utils {
    private static Utils instance = null;

    private final Logger logger;
    private String dateStr;
    private String timeStr;
    private String testResultsFolder;
    private String recordingsFolder;
    private String imagesFolder;
    private String hostname;
    private String baseFolder;
    private static final String DEFAULT_KEY = "VVxjC-BkcMff4vxqD0RhZcCsf2T9OVotNm-Y4vwAyhM=";

    private Utils() {
        this.logger = LogManager.getLogger(Utils.class);
        this.dateStr = getDateString();
        this.timeStr = getTimeString();
        this.hostname = sanitizeString(getHostname());
        this.baseFolder = System.getProperty("user.dir") + File.separator + "test_results";
        this.testResultsFolder = this.baseFolder + File.separator + this.hostname + File.separator + this.dateStr
                + File.separator + this.timeStr;
        this.recordingsFolder = this.testResultsFolder + File.separator + "recordings";
        this.imagesFolder = this.testResultsFolder + File.separator + "images";
    }

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    public String getDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMMyyyy", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    private String getTimeString() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH-mm-ss");
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return timeFormat.format(new Date());
    }

    public String getHostname() {
        if ((Config.RUN_IN_SELENIUM_GRID).equalsIgnoreCase("yes")) {
            return "selenium_grid";
        } else {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                e.printStackTrace();
                return "UnknownHost";
            }
        }
    }

    private String sanitizeString(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public String getTestResultsFolder() {
        return this.testResultsFolder;
    }

    public String getImagesFolder() {
        return this.imagesFolder;
    }

    public void stopDriverProcesses() {
        // List of driver process names to terminate
        List<String> driverNames = List.of("msedgedriver", "chromedriver", "firefoxdriver", "geckodriver");

        // Iterate over all running processes
        ProcessHandle.allProcesses().forEach(process -> {
            try {
                // Get the process's name
                String processName = process.info().command().orElse("").toLowerCase();

                // Check if the process name matches any of the driver names
                for (String driver : driverNames) {
                    if (processName.contains(driver)) {
                        // System.out.println("Terminating process: " + processName + " (PID: " +
                        // process.pid() + ")");
                        process.destroy(); // Terminate the process
                        break; // Exit the loop once a match is found
                    }
                }
            } catch (Exception e) {
                logger.error("Error handling process: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /* FTP methods */

    /**
     * Connect to FTP server and return the FTPClient object.
     */
    public FTPClient connectToFTP(String host, int port, String username, String password) {
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host, port);
            ftp.login(username, password);
            ftp.enterLocalPassiveMode();
            logger.warn("Connected to FTP server: " + host + ":" + port);
        } catch (IOException e) {
            logger.error("Failed to connect to FTP server: " + e.getMessage());
        }
        return ftp;
    }

    /**
     * Ensure the remote directory exists, create it if necessary.
     */
    public void ensureRemoteDirExists(FTPClient ftp, String remoteDir) {
        try {
            if (!ftp.changeWorkingDirectory(remoteDir)) { // Check if directory exists
                String[] parts = remoteDir.split("/");
                String currentPath = "";
                for (String part : parts) {
                    if (!part.isEmpty()) { // Skip empty parts
                        currentPath += "/" + part;
                        if (!ftp.changeWorkingDirectory(currentPath)) { // Explicit check
                            if (ftp.makeDirectory(currentPath)) { // Create directory
                                logger.warn("Created remote directory: " + currentPath);
                            } else {
                                logger.error("Failed to create remote directory: " + currentPath);
                                return; // Exit if directory creation fails
                            }
                        }
                    }
                }
                ftp.changeWorkingDirectory(remoteDir); // Final change to target directory
            }
            logger.warn("Changed to remote directory: " + remoteDir);
        } catch (IOException ex) {
            logger.error("Failed to ensure remote directory exists: " + ex.getMessage());
        }
    }

    /**
     * Upload a single file to the FTP server.
     */
    public void uploadFile(FTPClient ftp, String localFile, String remoteFile) {
        try {
            File file = new File(localFile);
            if (!file.exists()) {
                logger.error("Local file does not exist: " + localFile);
                return;
            }

            if (ftp.listFiles(remoteFile).length > 0) {
                logger.warn("File exists on server, skipping upload: " + remoteFile);
            } else {
                FileInputStream inputStream = new FileInputStream(file);
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                if (ftp.storeFile(remoteFile, inputStream)) {
                    // System.out.println("Uploaded file: " + remoteFile);
                } else {
                    logger.error("Failed to upload file: " + remoteFile);
                }
                inputStream.close();
            }
        } catch (IOException e) {
            logger.error("Error uploading file: " + e.getMessage());
        }
    }

    /**
     * Upload all files and subdirectories from a local directory.
     */
    public void uploadDirectory(FTPClient ftp, String localDir, String remoteDir) {
        try {
            ensureRemoteDirExists(ftp, remoteDir);
            File localDirectory = new File(localDir);
            for (File item : localDirectory.listFiles()) {
                String remotePath = remoteDir + "/" + item.getName();
                if (item.isDirectory()) {
                    uploadDirectory(ftp, item.getAbsolutePath(), remotePath); // Recursively upload subdirectories
                } else {
                    uploadFile(ftp, item.getAbsolutePath(), remotePath); // Upload files
                }
            }
        } catch (Exception e) {
            logger.error("Error uploading directory: " + e.getMessage());
        }
    }

    /**
     * Main function to upload a folder to the FTP server.
     */
    public void uploadFolderToFTP() {
        boolean uploadTr = "yes".equalsIgnoreCase(Config.UPLOAD_TEST_RESULTS);
        if (uploadTr) {
            String host = Config.FTP_HOST;
            int port = Integer.parseInt(Config.FTP_PORT);
            String username = Config.FTP_USER;
            String password = Config.FTP_PASSWORD;
            String localFolder = this.baseFolder;
            String remoteFolder = Config.FTP_USER_HOME;
            FTPClient ftp = connectToFTP(host, port, username, password);
            if (ftp != null && ftp.isConnected()) {
                try {
                    uploadDirectory(ftp, localFolder, remoteFolder);
                    ftp.logout();
                    ftp.disconnect();
                    // System.out.println("FTP connection closed.");
                } catch (IOException e) {
                    logger.error("Error closing FTP connection: " + e.getMessage());
                }
            }
        }
    }

    /* end ftp methods */

    /**
     * Removes an empty directory.
     */
    public void removeEmptyDir(String directory) {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            // Check if the directory is empty
            if (dir.listFiles() == null || dir.listFiles().length == 0) {
                if (dir.delete()) {
                    // System.out.println(directory + " has been removed.");
                } else {
                    logger.error("Failed to remove directory: " + directory);
                }
            } else {
                // System.out.println(directory + " is not empty and was not removed.");
            }
        } else {
            logger.error(directory + " does not exist or is not a directory.");
        }
    }

    /**
     * Checks if there are files with specified extensions in the directory.
     */
    public boolean doFilesWithExtInDir(String directory, List<String> tempExtensions) {
        try {
            File dir = new File(directory);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        for (String extension : tempExtensions) {
                            if (file.getName().endsWith(extension)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while checking for temporary files: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds files matching a partial filename in the specified directory.
     */
    public List<String> getMatchingFilesInDir(String tempDir, String partialFilename) {
        if (partialFilename == null || partialFilename.isEmpty()) {
            logger.error("Partial filename is blank.");
            return new ArrayList<>();
        }
        List<String> matchingFiles = new ArrayList<>();
        try {
            File dir = new File(tempDir);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains(partialFilename)) {
                            matchingFiles.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while checking file existence: " + e.getMessage());
        }
        return matchingFiles;
    }

    /**
     * Checks if an exact file exists in the directory.
     */
    public boolean doFileExistInDir(String tempDir, String exactFilename) {
        if (exactFilename == null || exactFilename.isEmpty()) {
            logger.error("Filename is blank.");
            return false;
        }
        try {
            File dir = new File(tempDir);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().equals(exactFilename)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while checking file existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Merge PDFs in parts, respecting a size threshold.
     */
    public void mergePdfsInParts() {
        String folderPath = this.testResultsFolder;
        String outputBase = folderPath + File.separator + "consolidated";
        String outputFileBase = outputBase + File.separator + "Test_Results_" + getDatetimeString();

        // Create output directory if it doesn't exist
        new File(outputBase).mkdirs();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".pdf"));

        if (files == null) {
            logger.error("No PDF files found in folder: " + folderPath);
            return;
        }

        // Separate test summary files and other PDF files
        List<File> testSummary = new ArrayList<>();
        List<File> pdfFiles = new ArrayList<>();

        for (File file : files) {
            if (file.getName().startsWith("Test_Summary_Results")) {
                testSummary.add(file);
            } else {
                pdfFiles.add(file);
            }
        }

        // System.out.println("Test Summary Files: " + testSummary);
        // System.out.println("Other PDF Files: " + pdfFiles);

        // Group PDFs by prefix (e.g., QS001, QS002)
        Map<String, List<File>> groupedPdfs = new TreeMap<>(); // TreeMap for sorted order by keys
        Pattern pattern = Pattern.compile("(QS\\d+)_", Pattern.CASE_INSENSITIVE);

        for (File file : pdfFiles) {
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.find()) {
                String prefix = matcher.group(1).toUpperCase();
                groupedPdfs.putIfAbsent(prefix, new ArrayList<>());
                groupedPdfs.get(prefix).add(file);
            }
        }

        // Sort the files within each group (optional, ensures consistent order)
        for (List<File> pdfList : groupedPdfs.values()) {
            pdfList.sort(Comparator.comparing(File::getName));
        }

        // Merge order: Test Summary files first, then grouped files in order
        List<File> mergeOrder = new ArrayList<>(testSummary);
        groupedPdfs.values().forEach(mergeOrder::addAll);

        // System.out.println("Final Merge Order: " + mergeOrder);

        // Total size of the PDFs
        long totalSize = mergeOrder.stream().mapToLong(File::length).sum();

        long maxSize = 100L * 1024 * 1024; // 100MB threshold

        if (totalSize <= maxSize) {
            // System.out.println("All PDFs are within the size limit. Merging into a single
            // file.");
            mergePDFs(mergeOrder, outputFileBase + ".pdf");
        } else {
            int partNumber = 1;
            long currentSize = 0;
            List<File> currentBatch = new ArrayList<>();

            for (File pdf : mergeOrder) {
                currentSize += pdf.length();
                currentBatch.add(pdf);

                if (currentSize >= maxSize) {
                    mergePDFs(currentBatch, outputFileBase + "_part" + partNumber + ".pdf");
                    partNumber++;
                    currentBatch.clear();
                    currentSize = 0;
                }
            }

            // Merge any remaining PDFs
            if (!currentBatch.isEmpty()) {
                mergePDFs(currentBatch, outputFileBase + "_part" + partNumber + ".pdf");
            }

            // System.out.println("Merged PDFs saved in parts under: " + outputBase);
        }
    }

    /**
     * Helper method to merge a list of PDFs into a single PDF file.
     */
    private void mergePDFs(List<File> pdfFiles, String outputFile) {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        try {
            for (File pdf : pdfFiles) {
                pdfMerger.addSource(pdf);
            }
            pdfMerger.setDestinationFileName(outputFile);
            pdfMerger.mergeDocuments(null);
            // System.out.println("Merged PDF saved at: " + outputFile);
        } catch (IOException e) {
            logger.error("Error during PDF merge: " + e.getMessage());
        }
    }

    public String getTestRecordingsFolder() {
        return this.recordingsFolder;
    }

    /**
     * Deletes a file at the specified path if it exists.
     */
    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                // System.out.println("File " + filePath + " has been deleted.");
            } else {
                logger.error("Failed to delete file: " + filePath);
            }
        } else {
            logger.error("File " + filePath + " does not exist.");
        }
    }

    /**
     * Deletes a folder and its contents at the specified path.
     */
    public void deleteFolderAndContents(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            deleteDirectoryRecursively(folder);
            // System.out.println("Folder and its contents have been removed: " +
            // folderPath);
        } else {
            logger.error("Folder " + folderPath + " does not exist or is not a directory.");
        }
    }

    private void deleteDirectoryRecursively(File directory) {
        File[] contents = directory.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Deletes subfolders within the specified folder while leaving files intact.
     */
    public void deleteSubfolders(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] contents = folder.listFiles();
            if (contents != null) {
                for (File item : contents) {
                    if (item.isDirectory()) {
                        deleteDirectoryRecursively(item);
                        // System.out.println("Subfolder " + item.getName() + " has been removed.");
                    }
                }
            }
            // System.out.println("All subfolders within " + folderPath + " have been
            // removed successfully.");
        } else {
            logger.error("Folder " + folderPath + " does not exist or is not a directory.");
        }
    }

    /**
     * Gets the name of the currently logged-in user.
     *
     * @return The name of the logged-in user.
     */
    public String getLoggedInUserName() {
        return System.getProperty("user.name"); // Retrieves the name of the logged-in user
    }

    /**
     * Creates directories for images, recordings, and test results if they don't
     * already exist.
     */
    public void createImageAndTestResultsFolders() {
        createFolderIfNotExists(this.imagesFolder);
        createFolderIfNotExists(this.recordingsFolder);
        createFolderIfNotExists(this.testResultsFolder);
    }

    /**
     * Helper method to create a folder if it does not already exist.
     */
    private void createFolderIfNotExists(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                // System.out.println("Created folder: " + folderPath);
            } else {
                logger.error("Failed to create folder: " + folderPath);
            }
        }
    }

    /**
     * Generates a formatted string representing the current date and time.
     * Format: DayMonthYear_HourMinuteSecondMicrosecond (e.g.,
     * 04Apr2025_014238123456)
     *
     * @return The formatted date and time string.
     */
    public String getDatetimeString() {
        LocalDateTime now = LocalDateTime.now(TimeZone.getTimeZone("UTC").toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyyyy_hh'h'mm'm'ss's'SSSSSS");
        return now.format(formatter);
    }

    public String takeScreenshotFullSrcTag(String fileName) throws Exception {
        // Split the fileName to create the folder structure
        String[] parts = fileName.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid fileName format. Expected format: 'tc1_chrome_001'");
        }

        // Define the base directory and folder structure
        String folder1 = parts[0]; // e.g., 'tc1'
        String folder2 = parts[1]; // e.g., 'chrome'
        String imageName = fileName + ".png"; // e.g., 'tc1_chrome_001.png'
        String folderPath = this.imagesFolder + File.separator + folder1 + File.separator + folder2;

        // Create the folders if they don't exist
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Define the full file path
        String filePath = folderPath + File.separator + imageName;

        // Take a screenshot using Robot
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        // Save the screenshot as a PNG file
        File outputFile = new File(filePath);
        ImageIO.write(screenshot, "png", outputFile);

        // Convert the screenshot to a base64-encoded string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenshot, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Return the base64 image string
        return "data:image/png;base64," + base64Image;
    }

    public List<String> getAbsoluteFilePathsInDir(File directory) {
        List<String> absolutePaths = new ArrayList<>();

        // Walk through the directory
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    absolutePaths.addAll(getAbsoluteFilePathsInDir(file));
                } else {
                    absolutePaths.add(file.getAbsolutePath());
                }
            }
        }
        return absolutePaths;
    }

    public List<String> getListAbsPathsOfDirAndSubDir(File folder) {
        List<String> folderPaths = new ArrayList<>();
        folderPaths.add(folder.getAbsolutePath());

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    folderPaths.add(file.getAbsolutePath());
                }
            }
        }
        return folderPaths;
    }

    public String getAbsPathFolderMatchingStringWithinFolder(File rootFolder, String searchFolderName) {
        List<String> folderPaths = getListAbsPathsOfDirAndSubDir(rootFolder); // Get all directories
        for (String folderPath : folderPaths) {
            File folder = new File(folderPath);
            if (folder.getName().equalsIgnoreCase(searchFolderName)) {
                return folder.getAbsolutePath();
            }
        }
        return "";
    }

    public boolean checkIfTwoFolderContainSameFiles(File folder1, File folder2) {
        File[] files1 = folder1.listFiles();
        File[] files2 = folder2.listFiles();

        if (files1 == null || files2 == null) {
            return false;
        }

        for (File file1 : files1) {
            for (File file2 : files2) {
                if (file1.getName().equals(file2.getName())) {
                    return true; // Common files found
                }
            }
        }
        return false;
    }

    public boolean isExcelDoc(File inputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // Attempt to create a Workbook (handles both .xls and .xlsx files)
            Workbook workbook = WorkbookFactory.create(fis);
            workbook.close();
            return true; // If no exception, it's a valid Excel file
        } catch (Exception e) {
            return false; // Not a valid Excel file
        }
    }

    public int getMonthNumber(String monthName) {
        Map<String, Integer> months = new HashMap<>();
        months.put("January", 1);
        months.put("February", 2);
        months.put("March", 3);
        months.put("April", 4);
        months.put("May", 5);
        months.put("June", 6);
        months.put("July", 7);
        months.put("August", 8);
        months.put("September", 9);
        months.put("October", 10);
        months.put("November", 11);
        months.put("December", 12);

        if (months.containsKey(monthName)) {
            return months.get(monthName);
        } else {
            throw new IllegalArgumentException("Not a valid month");
        }
    }

    public boolean checkIfFileExists(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    public boolean isDateFormatValid(String enteredDate) {
        try {
            // Define the formatter to parse dates in 'DD MMMM yyyy' format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

            // Split the entered date into components
            String[] parts = enteredDate.split(" ");
            if (parts.length != 3) {
                // System.out.println("Invalid date format.");
                return false;
            }

            String dayStr = parts[0];
            String month = parts[1];
            String yearStr = parts[2];

            // Convert day and year to integers for validation
            int day = Integer.parseInt(dayStr);
            int year = Integer.parseInt(yearStr);

            // Check if the year is within the valid range
            if (year < 2019 || year > 2025) {
                // System.out.println("Year is outside the valid range.");
                return false;
            }

            // Validate the month and day explicitly
            int maxDaysInMonth = switch (month.toLowerCase()) {
                case "january", "march", "may", "july", "august", "october", "december" -> 31;
                case "april", "june", "september", "november" -> 30;
                case "february" -> (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
                default -> {
                    // System.out.println("Invalid month name.");
                    yield -1; // Indicates an invalid month
                }
            };

            if (maxDaysInMonth == -1 || day < 1 || day > maxDaysInMonth) {
                // System.out.println("Invalid day for the given month.");
                return false;
            }

            // Validate the entire date format by parsing
            formatter.parse(enteredDate); // Ensures format validity

            return true;

        } catch (NumberFormatException e) {
            logger.error("Invalid number format: " + e.getMessage());
            return false;
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format: " + e.getMessage());
            return false;
        }
    }

    /* ENCRYPTION RELATED FUNCTIONS */
    public String encryptString(String inputString) throws Exception {
        // Call the method with DEFAULT_KEY
        return encryptString(inputString, DEFAULT_KEY);
    }

    public String decryptString(String encryptedString) throws Exception {
        // Call the method with DEFAULT_KEY
        return decryptString(encryptedString, DEFAULT_KEY);
    }

    public String encryptFile(String filePath, String outputFile) throws Exception {
        // Call the method with DEFAULT_KEY
        return encryptFile(filePath, outputFile, DEFAULT_KEY);
    }

    public String decryptFile(String encryptedFilePath) throws Exception {
        // Call the method with DEFAULT_KEY
        return decryptFile(encryptedFilePath, DEFAULT_KEY);
    }

    public String decryptFile(String encryptedFilePath, String decryptionKey) throws Exception {
        // Read the Base64-encoded encrypted content from the file
        String encodedEncryptedContent = new String(Files.readAllBytes(Paths.get(encryptedFilePath)));

        // Decrypt the content (input is Base64 string)
        return decryptString(encodedEncryptedContent, decryptionKey);
    }

    public String encryptFile(String filePath, String outputFile, String encryptionKey) throws Exception {
        // Read the file content
        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));

        // Encrypt the file content (returns Base64-encoded string)
        String encryptedContent = encryptString(fileContent, encryptionKey);

        // Save the Base64-encoded encrypted content to a file
        Files.write(Paths.get(outputFile), encryptedContent.getBytes());
        // System.out.println("File encrypted successfully and saved in readable
        // format!");

        return encryptedContent; // Return readable encrypted content
    }

    public String encryptString(String inputString, String encryptionKey) throws Exception {
        // Decode the encryption key
        byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Generate a random IV
        byte[] iv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Initialize cipher with AES and CBC mode
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Encrypt the input string
        byte[] encryptedBytes = cipher.doFinal(inputString.getBytes());

        // Combine IV and encrypted content (needed for decryption)
        byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);

        // Encode as Base64 for storage
        return Base64.getEncoder().encodeToString(ivAndEncrypted);
    }

    public String decryptString(String encryptedString, String encryptionKey) throws Exception {
        // Decode the encryption key
        byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Decode Base64 input
        byte[] ivAndEncrypted = Base64.getDecoder().decode(encryptedString);

        // Extract IV from the input
        byte[] iv = new byte[16];
        System.arraycopy(ivAndEncrypted, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted content
        byte[] encryptedBytes = new byte[ivAndEncrypted.length - iv.length];
        System.arraycopy(ivAndEncrypted, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Decrypt the content
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }
    /* END ENCRYPTION RELATED FUNCTIONS */

    public String detectOS() {
        // Get the name of the operating system
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (osName.contains("linux")) {
            return "Linux";
        } else if (osName.contains("windows")) {
            return "Windows";
        } else {
            return "Operating System detected: " + osName;
        }
    }

    public String formatElapsedTime(double elapsedTime) {
        // Convert milliseconds to seconds
        int totalMilliseconds = (int) elapsedTime;
        int milliseconds = totalMilliseconds % 1000;
        int totalSeconds = totalMilliseconds / 1000;

        // Calculate seconds, minutes, and hours
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        // Return formatted time
        return hours + "hrs:" + minutes + "min:" + seconds + "sec:" + milliseconds + "ms";
    }

    public void convertCsvToXlsx(String csvFilePath, String xlsxFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath));
                FileOutputStream fileOut = new FileOutputStream(xlsxFilePath)) {

            Workbook workbook = WorkbookFactory.create(true); // Create .xlsx format
            Sheet sheet = workbook.createSheet("Data");

            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                Row row = sheet.createRow(rowIndex++);

                // Properly handle quoted fields using regex
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                for (int colIndex = 0; colIndex < values.length; colIndex++) {
                    Cell cell = row.createCell(colIndex);

                    // Preserve actual new lines inside Excel cells
                    String cellValue = values[colIndex].trim().replace("\\n", "\n");
                    cell.setCellValue(cellValue);

                    // **Set cell style to preserve multi-line text formatting**
                    CellStyle style = workbook.createCellStyle();
                    style.setWrapText(true); // Enables word wrap in Excel cells
                    cell.setCellStyle(style);
                }
            }

            // Auto-size columns for better visibility
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to Excel file
            workbook.write(fileOut);
            workbook.close();
            // System.out.println("Converted: " + xlsxFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Delete CSV after conversion
            Files.deleteIfExists(Paths.get(csvFilePath));
            // System.out.println("Deleted: " + csvFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String formatNumberZeroPad4Char(int number) {
        return String.format("%04d", number);
    }

    public void openHelpHtml(String htmlFilePathString) {
        try {
            // Decrypt the help document
            String htmlContent = decryptFile(htmlFilePathString);

            // Create a temporary HTML file
            File tempFile = File.createTempFile("help_doc", ".html");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(htmlContent);
                writer.flush();
            }

            // Open the file in the default browser
            java.awt.Desktop.getDesktop().browse(tempFile.toURI());

            logger.info("Temporary file created at: " + tempFile.getAbsolutePath());
            logger.info("Please close the browser manually when you're done.");

        } catch (Exception e) {
            logger.error("Error creating or opening the temporary help file: " + e.getMessage());
        }
    }

    public List<String> getListAbsPathsOfDirAndSubDir(String folderPath) {
        List<String> absolutePaths = new ArrayList<>();

        // Get the absolute path of the provided folder
        Path absolutePath = Paths.get(folderPath).toAbsolutePath();
        absolutePaths.add(absolutePath.toString());

        // Retrieve subdirectories
        File folder = absolutePath.toFile();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        absolutePaths.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return absolutePaths;
    }

    public void createDirInPath(String directoryPath) {
        Path path = Paths.get(directoryPath);

        try {
            Files.createDirectories(path);
            // System.out.println("Directories created successfully: " + directoryPath);
        } catch (IOException e) {
            logger.error("Failed to create directories: " + e.getMessage());
        }
    }
}
