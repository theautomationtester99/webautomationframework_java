package com.waf;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.waf.config.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Runner {
    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger(Runner.class);
        try {
            Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            Lock lock = new ReentrantLock();

            Utils utils = Utils.getInstance();

            logger.info("Execution Started ----------------.");
            logger.info("Parsing the input arguments.");

            // Define CLI options with correct "--" prefixes
            Options options = new Options();
            options.addOption(new Option(null, "start", false, "Start the execution."));
            options.addOption(new Option(null, "start-parallel", false, "Start parallel execution."));
            options.addOption(new Option(null, "version", false, "Display the Version."));
            options.addOption(
                    new Option(null, "delete-tr-google-drive", false, "Deletes test results from Google Drive."));
            options.addOption(new Option(null, "encrypt-file", true, "Encrypts the file."));
            options.addOption(new Option(null, "encrypt-str", true, "Encrypts the string."));
            options.addOption(new Option(null, "output-file", true, "Specify the output file name."));
            options.addOption(
                    new Option(null, "help-html", false, "Open the default browser to display dynamic help."));

            // Parse the command-line arguments
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd;
            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                logger.error("Error parsing arguments: " + e.getMessage());
                System.exit(1);
                return;
            }

            // Ensure only one argument is active
            int activeArgs = (cmd.hasOption("start") ? 1 : 0) +
                    (cmd.hasOption("start-parallel") ? 1 : 0) +
                    (cmd.hasOption("version") ? 1 : 0) +
                    (cmd.hasOption("encrypt-file") ? 1 : 0) +
                    (cmd.hasOption("delete-tr-google-drive") ? 1 : 0) +
                    (cmd.hasOption("help-html") ? 1 : 0) +
                    (cmd.hasOption("encrypt-str") ? 1 : 0);

            if (activeArgs > 1) {
                logger.error(
                        "Only one of '--start', '--start-parallel', '--version', '--encrypt-file', '--encrypt-str', '--delete-tr-google-drive', or '--help-html' can be used at a time.");
                System.exit(1);
            }

            // Determine if parallel execution is enabled via environment variable
            boolean parallelExecution = "yes".equalsIgnoreCase(Config.PARALLEL_EXECUTION);
            boolean headless = "yes".equalsIgnoreCase(Config.HEADLESS);
            boolean runInSeleniumGrid = "yes".equalsIgnoreCase(Config.RUN_IN_SELENIUM_GRID);

            // Start execution
            if (cmd.hasOption("start") && parallelExecution) {
                logger.info("Parallel Execution is enabled in config. So starting parallel test execution...");
                if (runInSeleniumGrid || headless) {
                    logger.info("Starting test execution...");
                    ExecutionManager.startExecution(utils, lock, true);
                } else {
                    logger.error("To perform parallel execution, headless should be set to YES");
                }
            } else if (cmd.hasOption("start")) {
                logger.info("Starting test execution...");
                ExecutionManager.startExecution(utils, lock, false);
            }

            if (cmd.hasOption("start-parallel")) {
                if (runInSeleniumGrid || headless) {
                    logger.info("Starting test execution...");
                    ExecutionManager.startExecution(utils, lock, cmd.hasOption("start-parallel"));
                } else {
                    logger.error("To perform parallel execution, headless should be set to YES");
                }
            }

            if (cmd.hasOption("version")) {
                logger.info("Version: 3.0");
                // System.out.println("Version: 3.0");
                System.exit(0);
            }

            if (cmd.hasOption("help-html")) {
                logger.info("Displaying help documentation in browser...");
                utils.openHelpHtml(baseDir.resolve("resources/enc_help_doc.enc").toString());
            }

            if (cmd.hasOption("encrypt-file") || cmd.hasOption("output-file")) {
                if (!(cmd.hasOption("encrypt-file") && cmd.hasOption("output-file"))) {
                    logger.error("Both '--encrypt-file' and '--output-file' options must be provided together.");
                    System.exit(1);
                }

                String inputFile = cmd.getOptionValue("encrypt-file");
                String outputFile = cmd.getOptionValue("output-file");

                if (inputFile == null || inputFile.trim().isEmpty()) {
                    logger.error("Invalid input file provided for encryption.");
                    System.exit(1);
                }

                if (outputFile == null || outputFile.trim().isEmpty()) {
                    logger.error("Invalid output file name specified.");
                    System.exit(1);
                }

                utils.encryptFile(inputFile, outputFile);
                logger.info("File encrypted successfully.");
                System.exit(0);
            }

            if (cmd.hasOption("encrypt-str")) {
                String inputStr = cmd.getOptionValue("encrypt-str");

                // Check if the input string is null, empty, or just spaces
                if (inputStr == null || inputStr.trim().isEmpty()) {
                    logger.error("Invalid input string provided for encryption.");
                    System.exit(1);
                }

                String encryptedStr = utils.encryptString(inputStr);
                logger.info("Encrypted string: " + encryptedStr);
                System.exit(0);
            }

        } catch (Exception e) {
            logger.error("Unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
