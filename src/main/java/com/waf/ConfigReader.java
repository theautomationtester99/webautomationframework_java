package com.waf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private String configFile;
    private Properties properties;

    /**
     * A utility class to read and manage configuration files using the Properties class.
     *
     * This class provides methods to load configuration files and retrieve properties.
     *
     * @param configFile The path to the configuration file.
     * @throws IOException If the configuration file does not exist or cannot be read.
     */
    public ConfigReader(String configFile) throws IOException {
        this.configFile = configFile;
        this.properties = loadConfig();
    }

    private Properties loadConfig() throws IOException {
        Properties props = new Properties();
        File file = new File(configFile);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                props.load(reader);
            }
        }
        return props;
    }

    /**
     * Retrieves the value of a property from the configuration file.
     *
     * This method allows fetching a property value. If the property does not exist,
     * the method returns the specified fallback value.
     *
     * @param key The name of the property to retrieve.
     * @param fallback The value to return if the property does not exist.
     * @return The value of the property, or the fallback value if the property does not exist.
     */
    public String getProperty(String key, String fallback) {
        return properties.getProperty(key, fallback);
    }

    /**
     * Retrieves the value of a property from all keys if no specific key is provided.
     *
     * @param propertyName The name of the property to search for.
     * @param fallback The value to return if the property does not exist.
     * @return The value of the property, or the fallback value if the property does not exist.
     */
    public String getPropertyFromAnySection(String propertyName, String fallback) {
        for (Object key : properties.keySet()) {
            if (key.toString().equalsIgnoreCase(propertyName)) {
                return properties.getProperty(key.toString());
            }
        }
        return fallback;
    }
}
