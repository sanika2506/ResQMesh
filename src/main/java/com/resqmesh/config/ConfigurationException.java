package com.resqmesh.config;

/**
 * Custom exception thrown when a network configuration file or JSON payload
 * fails parsing, schema validation, or graph consistency constraints.
 */
public class ConfigurationException extends Exception {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
