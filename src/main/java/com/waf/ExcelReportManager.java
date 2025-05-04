package com.waf;

import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.xlsx.XlsxReadOptions;
import tech.tablesaw.io.xlsx.XlsxReader;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExcelReportManager {
    private Logger logger;
    private Lock lock;
    private Utils utils;
    private List<List<String>> allTestResultsList;
    private List<List<String>> allSkippedTestResultsList;
    private String resultFolder;

    public ExcelReportManager(Lock lock) {
        this.logger = LogManager.getLogger(ExcelReportManager.class);
        this.lock = lock;
        this.utils = Utils.getInstance();
        this.resultFolder = utils.getTestResultsFolder();
    }

    public void addRow(List<String> testResult) {
        lock.lock();
        try {
            File outputFile = new File(Paths.get(resultFolder, "output.xlsx").toString());
            if (utils.checkIfFileExists(outputFile.getAbsolutePath())) {
                try {
                    XlsxReader reader = new XlsxReader();
                    XlsxReadOptions options = XlsxReadOptions.builder(outputFile.getAbsolutePath()).build();
                    Table table = reader.read(options);

                    allTestResultsList = new ArrayList<>();

                    for (Row row : table) {
                        List<String> rowData = new ArrayList<>();
                        for (Column<?> col : table.columns()) {
                            rowData.add(row.getString(col.name())); // Convert column value to string
                        }
                        allTestResultsList.add(rowData);
                    }

                    allTestResultsList.add(testResult);
                    exportToExcel();
                } catch (Exception e) {
                    logger.error("Error reading Excel file: " + e.getMessage());
                }
            } else {
                allTestResultsList.add(testResult);
                exportToExcel();
            }
        } finally {
            lock.unlock();
        }
    }

    public void addRowSkippedTC(List<String> testResult) {
        lock.lock();
        try {
            File outputFile = new File(Paths.get(resultFolder, "skipped_tc_report.xlsx").toString());
            this.allSkippedTestResultsList = new ArrayList<>();
            if (utils.checkIfFileExists(outputFile.getAbsolutePath())) {
                try {
                    XlsxReader reader = new XlsxReader();
                    XlsxReadOptions options = XlsxReadOptions.builder(outputFile.getAbsolutePath()).build();
                    Table table = reader.read(options);

                    for (Row row : table) {
                        List<String> rowData = new ArrayList<>();
                        for (Column<?> col : table.columns()) {
                            rowData.add(row.getString(col.name())); // Convert column value to string
                        }
                        allSkippedTestResultsList.add(rowData);
                    }

                    this.allSkippedTestResultsList.add(testResult);
                    exportToExcelSkippedTC();
                } catch (Exception e) {
                    logger.error("Error reading Excel file: " + e.getMessage());
                }
            } else {
                this.allSkippedTestResultsList.add(testResult);
                exportToExcelSkippedTC();
            }
        } finally {
            lock.unlock();
        }
    }

    public void exportToExcel() {
        lock.lock();
        try {
            Table table = Table.create("Test Results")
                    .addColumns(
                            StringColumn.create("tc_id"),
                            StringColumn.create("tc_description"),
                            StringColumn.create("Status"),
                            StringColumn.create("Browser"),
                            StringColumn.create("Executed Date"));

            for (List<String> row : allTestResultsList) {
                int rowIndex = table.rowCount(); // Get the current row index
                table.appendRow();
                table.stringColumn("tc_id").set(rowIndex, row.get(0));
                table.stringColumn("tc_description").set(rowIndex, row.get(1));
                table.stringColumn("Status").set(rowIndex, row.get(2));
                table.stringColumn("Browser").set(rowIndex, row.get(3));
                table.stringColumn("Executed Date").set(rowIndex, row.get(4));
            }

            table.write().csv(Paths.get(resultFolder, "output.csv").toString());
            this.utils.convertCsvToXlsx(Paths.get(resultFolder, "output.csv").toString(),
                    Paths.get(resultFolder, "output.xlsx").toString());
        } catch (Exception e) {
            logger.error("Error exporting to Excel: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void exportToExcelSkippedTC() {
        lock.lock();
        try {
            Table table = Table.create("Skipped Test Cases")
                    .addColumns(
                            StringColumn.create("test_script"),
                            StringColumn.create("why_skipped"),
                            StringColumn.create("Status"));

            for (List<String> row : allSkippedTestResultsList) {
                int rowIndex = table.rowCount(); // Get the current row index
                table.appendRow();
                table.stringColumn("test_script").set(rowIndex, row.get(0));
                table.stringColumn("why_skipped").set(rowIndex, row.get(1));
                table.stringColumn("Status").set(rowIndex, row.get(2));
            }

            logger.warn("Writing skipped tc report csv file");
            logger.warn(allSkippedTestResultsList.toString());

            table.write().csv(Paths.get(resultFolder, "skipped_tc_report.csv").toString());
            this.utils.convertCsvToXlsx(Paths.get(resultFolder, "skipped_tc_report.csv").toString(),
                    Paths.get(resultFolder, "skipped_tc_report.xlsx").toString());
        } catch (Exception e) {
            logger.error("Error exporting to Excel: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
