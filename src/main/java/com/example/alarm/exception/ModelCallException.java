package com.example.alarm.exception;

public class ModelCallException extends RuntimeException {
    private final String modelName;

    public ModelCallException(String modelName, String message) {
        super(message);
        this.modelName = modelName;
    }

    public String getModelName() { return modelName; }
}
