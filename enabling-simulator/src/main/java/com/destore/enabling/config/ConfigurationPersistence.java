package com.destore.enabling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
@Slf4j
public class ConfigurationPersistence {
    
    private static final String CONFIG_FILE = "enabling-simulator-config.properties";
    private final Path configPath;
    
    public ConfigurationPersistence() {
        // Store config in /app/config directory (for Docker) or current directory
        String configDir = System.getProperty("user.home");
        this.configPath = Paths.get(configDir, CONFIG_FILE);
        log.info("Configuration file path: {}", configPath.toAbsolutePath());
    }
    
    public void saveConfiguration(BigDecimal approvalThreshold, long processingDelayMs, boolean autoApproveEnabled) {
        Properties props = new Properties();
        props.setProperty("approvalThreshold", approvalThreshold.toString());
        props.setProperty("processingDelayMs", String.valueOf(processingDelayMs));
        props.setProperty("autoApproveEnabled", String.valueOf(autoApproveEnabled));
        
        try (OutputStream output = new FileOutputStream(configPath.toFile())) {
            props.store(output, "Enabling Simulator Configuration");
            log.info("Configuration saved successfully to {}", configPath);
        } catch (IOException e) {
            log.error("Failed to save configuration to file", e);
        }
    }
    
    public Properties loadConfiguration() {
        Properties props = new Properties();
        
        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                props.load(input);
                log.info("Configuration loaded successfully from {}", configPath);
            } catch (IOException e) {
                log.error("Failed to load configuration from file", e);
            }
        } else {
            log.info("No persisted configuration file found at {}", configPath);
        }
        
        return props;
    }
}
