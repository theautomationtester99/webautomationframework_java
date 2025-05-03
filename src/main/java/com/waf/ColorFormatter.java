package com.waf;

import java.util.logging.*;
import java.util.HashMap;
import java.util.Map;

public class ColorFormatter extends Formatter {

    // ANSI escape codes for colors
    private static final String RESET = "\u001B[0m";
    private static final String BLUE_BRIGHT = "\u001B[94m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED_BRIGHT = "\u001B[91m";
    private static final String MAGENTA_BRIGHT = "\u001B[95m";

    private final String defaultFormat = "%1$tF %1$tT - %2$s - %3$s - [PID:%4$d] %5$s [%6$s:%7$d]";
    private final Map<Level, String> levelColors;

    public ColorFormatter() {
        levelColors = new HashMap<>();
        levelColors.put(Level.FINE, BLUE_BRIGHT);
        levelColors.put(Level.INFO, GREEN);
        levelColors.put(Level.WARNING, YELLOW);
        levelColors.put(Level.SEVERE, RED_BRIGHT);
        levelColors.put(Level.SEVERE, RED_BRIGHT); // Critical-level mapping, equivalent to Python's CRITICAL
    }

    @Override
    public String format(LogRecord record) {
        String color = levelColors.getOrDefault(record.getLevel(), RESET);
        String sourceClassName = record.getSourceClassName();
        String sourceMethodName = record.getSourceMethodName();
        int lineNumber = -1;

        // Try to fetch the line number from the stack trace
        Throwable dummyThrowable = new Throwable();
        for (StackTraceElement element : dummyThrowable.getStackTrace()) {
            if (element.getClassName().equals(sourceClassName) && element.getMethodName().equals(sourceMethodName)) {
                lineNumber = element.getLineNumber();
                break;
            }
        }

        String message = String.format(defaultFormat,
                new java.util.Date(record.getMillis()), // Timestamp
                record.getLoggerName(),
                record.getLevel().getName(),
                Thread.currentThread().getId(), // Replace process ID concept with Thread ID
                record.getMessage(),
                sourceClassName,
                lineNumber);

        // Append exception details if available
        if (record.getThrown() != null) {
            StringBuilder exceptionDetails = new StringBuilder();
            exceptionDetails.append(MAGENTA_BRIGHT);
            for (StackTraceElement element : record.getThrown().getStackTrace()) {
                exceptionDetails.append("\n\tat ").append(element.toString());
            }
            exceptionDetails.append(RESET);
            message += exceptionDetails.toString();
        }

        return color + message + RESET + System.lineSeparator();
    }

}
