package com.waf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReportManager {
    private Logger logger;
    private Lock lock;
    private Utils utils;
    private String resultFolder;

    public ExcelReportManager(Lock lock) {
        this.logger = LogManager.getLogger(ExcelReportManager.class);
        this.lock = lock;
        this.utils = Utils.getInstance();
        this.resultFolder = utils.getTestResultsFolder();
    }

    // public void addRow(List<String> testResult) {
    // lock.lock();
    // try {
    // File outputFile = new File(Paths.get(resultFolder,
    // "output.xlsx").toString());
    // Workbook workbook;

    // // Open existing workbook or create a new one
    // if (outputFile.exists()) {
    // try (FileInputStream fis = new FileInputStream(outputFile)) {
    // workbook = WorkbookFactory.create(fis);
    // }
    // } else {
    // workbook = new XSSFWorkbook();
    // }

    // Sheet sheet = workbook.getSheet("Test Results");
    // if (sheet == null) {
    // sheet = workbook.createSheet("Test Results");
    // }

    // // Append the new row directly
    // int rowCount = sheet.getPhysicalNumberOfRows();
    // Row newRow = sheet.createRow(rowCount);
    // for (int colIndex = 0; colIndex < testResult.size(); colIndex++) {
    // newRow.createCell(colIndex).setCellValue(testResult.get(colIndex));
    // }

    // // Write back to the file
    // try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
    // workbook.write(fileOut);
    // }

    // workbook.close();

    // } catch (Exception e) {
    // logger.error("Error reading or writing Excel file: " + e.getMessage());
    // } finally {
    // lock.unlock();
    // }
    // }

    // public void addRowSkippedTC(List<String> testResult) {
    // lock.lock();
    // try {
    // File outputFile = new File(Paths.get(resultFolder,
    // "skipped_tc_report.xlsx").toString());
    // Workbook workbook;

    // // Open existing workbook or create a new one
    // if (outputFile.exists()) {
    // try (FileInputStream fis = new FileInputStream(outputFile)) {
    // workbook = WorkbookFactory.create(fis);
    // }
    // } else {
    // workbook = new XSSFWorkbook();
    // }

    // Sheet sheet = workbook.getSheet("Skipped Test Cases");
    // if (sheet == null) {
    // sheet = workbook.createSheet("Skipped Test Cases");
    // }

    // // Append the new row directly
    // int rowCount = sheet.getPhysicalNumberOfRows();
    // Row newRow = sheet.createRow(rowCount);
    // for (int colIndex = 0; colIndex < testResult.size(); colIndex++) {
    // newRow.createCell(colIndex).setCellValue(testResult.get(colIndex));
    // }

    // // Write back to the file
    // try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
    // workbook.write(fileOut);
    // }

    // workbook.close();

    // } catch (Exception e) {
    // logger.error("Error reading or writing Excel file: " + e.getMessage());
    // } finally {
    // lock.unlock();
    // }
    // }

    public void addRowToExcel(String fileName, String sheetName, List<String> testResult) {
        lock.lock();
        try {
            File outputFile = new File(Paths.get(resultFolder, fileName).toString());
            Workbook workbook;
    
            // Open existing workbook or create a new one
            if (outputFile.exists()) {
                try (FileInputStream fis = new FileInputStream(outputFile)) {
                    workbook = WorkbookFactory.create(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
            }
    
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
    
                // Determine headers based on file name
                String[] headers;
                if (fileName.equalsIgnoreCase("output.xlsx")) {
                    headers = new String[]{"tc_id", "tc_description", "Status", "Browser", "Executed Date"};
                } else if (fileName.equalsIgnoreCase("skipped_tc_report.xlsx")) {
                    headers = new String[]{"test_script", "skipped_reason", "status"};
                } else {
                    headers = new String[]{"Column1", "Column2", "Column3"}; // Default headers if needed
                }
    
                // Add header row
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    CellStyle style = workbook.createCellStyle();
                    Font font = workbook.createFont();
                    font.setBold(true);
                    style.setFont(font);
                    cell.setCellStyle(style);
                }
            }
    
            // Append the new row
            int rowCount = sheet.getPhysicalNumberOfRows();
            Row newRow = sheet.createRow(rowCount);
            for (int colIndex = 0; colIndex < testResult.size(); colIndex++) {
                newRow.createCell(colIndex).setCellValue(testResult.get(colIndex));
            }
    
            // Adjust column widths dynamically
            for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                sheet.autoSizeColumn(i);
    
                int maxWidth = 5000; // Limit excessive column width
                if (sheet.getColumnWidth(i) > maxWidth) {
                    sheet.setColumnWidth(i, maxWidth);
                }
            }
    
            // Write back to the file
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }
    
            workbook.close();
    
        } catch (Exception e) {
            logger.error("Error reading or writing Excel file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
    

    // public void exportToExcel() {
    // lock.lock();
    // try {
    // File excelFile = new File(resultFolder + "/output.xlsx");
    // Workbook workbook;

    // // If the file exists, open it; otherwise, create a new workbook
    // if (excelFile.exists()) {
    // try (FileInputStream fis = new FileInputStream(excelFile)) {
    // workbook = WorkbookFactory.create(fis);
    // }
    // } else {
    // workbook = new XSSFWorkbook(); // Creating a new workbook
    // }

    // Sheet sheet = workbook.getSheet("Test Results");
    // if (sheet == null) {
    // sheet = workbook.createSheet("Test Results");
    // }

    // // Creating header row if sheet is empty
    // if (sheet.getPhysicalNumberOfRows() == 0) {
    // Row headerRow = sheet.createRow(0);
    // String[] headers = { "tc_id", "tc_description", "Status", "Browser",
    // "Executed Date" };
    // for (int i = 0; i < headers.length; i++) {
    // Cell cell = headerRow.createCell(i);
    // cell.setCellValue(headers[i]);
    // CellStyle style = workbook.createCellStyle();
    // Font font = workbook.createFont();
    // font.setBold(true);
    // style.setFont(font);
    // cell.setCellStyle(style);
    // }
    // }

    // // Populating rows with data
    // int rowIndex = sheet.getPhysicalNumberOfRows();
    // for (List<String> row : allTestResultsList) {
    // Row excelRow = sheet.createRow(rowIndex++);
    // for (int colIndex = 0; colIndex < row.size(); colIndex++) {
    // excelRow.createCell(colIndex).setCellValue(row.get(colIndex));
    // }
    // }

    // // Adjust column widths dynamically
    // for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
    // sheet.autoSizeColumn(i);

    // int maxWidth = 5000; // Limit excessive column width
    // if (sheet.getColumnWidth(i) > maxWidth) {
    // sheet.setColumnWidth(i, maxWidth);
    // }
    // }

    // // Writing to file
    // try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
    // workbook.write(fileOut);
    // }

    // workbook.close();

    // } catch (IOException | InvalidFormatException e) {
    // logger.error("Error exporting to Excel: " + e.getMessage());
    // } finally {
    // lock.unlock();
    // }
    // }

    // public void exportToExcelSkippedTC() {
    // lock.lock();
    // try {
    // Table table = Table.create("Skipped Test Cases")
    // .addColumns(
    // StringColumn.create("test_script"),
    // StringColumn.create("why_skipped"),
    // StringColumn.create("Status"));

    // for (List<String> row : allSkippedTestResultsList) {
    // int rowIndex = table.rowCount(); // Get the current row index
    // table.appendRow();
    // table.stringColumn("test_script").set(rowIndex, row.get(0));
    // table.stringColumn("why_skipped").set(rowIndex, row.get(1));
    // table.stringColumn("Status").set(rowIndex, row.get(2));
    // }

    // logger.warn("Writing skipped tc report csv file");
    // logger.warn(allSkippedTestResultsList.toString());

    // table.write().csv(Paths.get(resultFolder,
    // "skipped_tc_report.csv").toString());
    // this.utils.convertCsvToXlsx(Paths.get(resultFolder,
    // "skipped_tc_report.csv").toString(),
    // Paths.get(resultFolder, "skipped_tc_report.xlsx").toString());
    // } catch (Exception e) {
    // logger.error("Error exporting to Excel: " + e.getMessage());
    // } finally {
    // lock.unlock();
    // }
    // }
}
