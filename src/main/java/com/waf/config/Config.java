package com.waf.config;

import java.util.Optional;

public class Config {
    // Load environment variables with defaults
    public static final String DELETE_TEST_RESULTS = Optional.ofNullable(System.getenv("DELETE_TEST_RESULTS_IMAGES_RECORDINGS_FOLDERS_BEFORE_START")).orElse("yes");
    public static final String SCREENSHOT_STRATEGY = Optional.ofNullable(System.getenv("SCREENSHOT_STRATEGY")).orElse("always");
    public static final String HIGHLIGHT_ELEMENTS = Optional.ofNullable(System.getenv("HIGHLIGHT_ELEMENTS")).orElse("no");
    public static final int MAX_RETRIES = Integer.parseInt(Optional.ofNullable(System.getenv("MAX_RETRIES")).orElse("0"));
    public static final String UPLOAD_TEST_RESULTS = Optional.ofNullable(System.getenv("UPLOAD_TEST_RESULTS")).orElse("no");
    public static final String SEND_TEST_RESULTS_EMAIL = Optional.ofNullable(System.getenv("SEND_TEST_RESULTS_EMAIL")).orElse("no");
    public static final String SENDER_EMAIL = Optional.ofNullable(System.getenv("SENDER_EMAIL")).orElse("theautomationtester99@gmail.com");
    public static final String SENDER_EMAIL_PASSWORD = Optional.ofNullable(System.getenv("SENDER_EMAIL_PASSWORD")).orElse("gAAAAABoAjriMu91TSpf6VycE89O8wv2gpUMx4MkmxAtvFn-Auz4g5RhnPUZJ13FRHkvoM_JnpWau3GoHgcumOmp14Aecg7AKKVORICG87IPFKFX4I6r2xY=");
    public static final String RECIPIENT_EMAILS = Optional.ofNullable(System.getenv("RECIPIENT_EMAILS")).orElse("theautomationtester@hotmail.com,theautomationtester99@gmail.com");

    public static final String RUN_IN_SELENIUM_GRID = Optional.ofNullable(System.getenv("RUN_IN_SELENIUM_GRID")).orElse("no");
    public static final String GRID_URL = Optional.ofNullable(System.getenv("GRID_URL")).orElse("http://localhost:30805/wd/hub");
    public static final String RUN_IN_APPIUM_GRID = Optional.ofNullable(System.getenv("RUN_IN_APPIUM_GRID")).orElse("no");
    public static final String APPIUM_URL = Optional.ofNullable(System.getenv("APPIUM_URL")).orElse("http://192.168.1.8:4723/wd/hub");

    public static final String LOG_LEVEL = Optional.ofNullable(System.getenv("LOG_LEVEL")).orElse("warning");

    public static final String INPRIVATE = Optional.ofNullable(System.getenv("INPRIVATE")).orElse("no");
    public static final String HEADLESS = Optional.ofNullable(System.getenv("HEADLESS")).orElse("yes");

    public static final int NO_THREADS = Integer.parseInt(Optional.ofNullable(System.getenv("NO_THREADS")).orElse("4"));
    public static final String PARALLEL_EXECUTION = Optional.ofNullable(System.getenv("PARALLEL_EXECUTION")).orElse("yes");
    public static final String FTP_UPLOAD = Optional.ofNullable(System.getenv("FTP_UPLOAD")).orElse("yes");
    public static final String FTP_HOST = Optional.ofNullable(System.getenv("FTP_HOST")).orElse("localhost");
    public static final String FTP_USER = Optional.ofNullable(System.getenv("FTP_USER")).orElse("user1");
    public static final String FTP_PASSWORD = Optional.ofNullable(System.getenv("FTP_PASSWORD")).orElse("password1");
    public static final String FTP_USER_HOME = Optional.ofNullable(System.getenv("FTP_USER_HOME")).orElse("/home/user1");
}

