package com.waf;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

public class LoggerConfig {

    private final String logFile;
    private final Level logLevel;
    private final BlockingQueue<LogRecord> logQueue;
    private QueueListener listener;

    public LoggerConfig(String logFile) throws IOException {
        String defaultLogFile = "logs/waf.log";
        this.logFile = logFile != null && !logFile.isEmpty() ? logFile : defaultLogFile;
        this.logLevel = getLogLevelFromConfig();
        this.logQueue = new LinkedBlockingQueue<>();
        //this.logger = setupLogger();
    }

    private Level getLogLevelFromConfig() {
        // Replace this with actual config retrieval logic
        String logLevelStr = "INFO"; // Example log level from configuration
        return Level.parse(logLevelStr);
    }

    public Logger setupLogger() throws IOException {
        Logger logger = Logger.getLogger("WafLogger");
        logger.setLevel(logLevel);

        // Avoid duplicate handlers
        if (logger.getHandlers().length == 0) {
            // QueueHandler to send log messages to the listener
            QueueHandler queueHandler = new QueueHandler(logQueue);
            logger.addHandler(queueHandler);
        }
        return logger;
    }

    public void startListener() throws IOException {
        // Create log directory if it doesn't exist
        String logDir = logFile.substring(0, logFile.lastIndexOf('/'));
        new java.io.File(logDir).mkdirs();

        // Rotating File Handler
        FileHandler fileHandler = new FileHandler(logFile, 5 * 1024 * 1024, 5, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        // Console handler with a custom formatter
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(logLevel);
        consoleHandler.setFormatter(new ColorFormatter());

        // Queue Listener
        listener = new QueueListener(logQueue, fileHandler, consoleHandler);
        listener.start();
    }

    public void stopListener() {
        if (listener != null) {
            listener.stopListener(); // Gracefully stop the QueueListener
        }
    }

    private static class QueueHandler extends Handler {
        private final BlockingQueue<LogRecord> logQueue;

        public QueueHandler(BlockingQueue<LogRecord> logQueue) {
            this.logQueue = logQueue;
        }

        @Override
        public void publish(LogRecord record) {
            logQueue.offer(record);
        }

        @Override
        public void flush() {
            // No-op for this handler
        }

        @Override
        public void close() throws SecurityException {
            // No-op for this handler
        }
    }

    private static class QueueListener extends Thread {
        private final BlockingQueue<LogRecord> logQueue;
        private final Handler[] handlers;
        private volatile boolean running = true;

        public QueueListener(BlockingQueue<LogRecord> logQueue, Handler... handlers) {
            this.logQueue = logQueue;
            this.handlers = handlers;
        }

        public void stopListener() {
            running = false;
            interrupt(); // Interrupt blocking operations
        }

        public void run() {
            try {
                while (running) {
                    LogRecord record = logQueue.take();
                    for (Handler handler : handlers) {
                        handler.publish(record);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        LoggerConfig loggerConfig = new LoggerConfig("");
        loggerConfig.startListener();

        // Call setupLogger to configure the logger
        Logger logger = loggerConfig.setupLogger();

        logger.info("This is an INFO message.");
        logger.warning("This is a WARNING message.");

        loggerConfig.stopListener();
    }
}
