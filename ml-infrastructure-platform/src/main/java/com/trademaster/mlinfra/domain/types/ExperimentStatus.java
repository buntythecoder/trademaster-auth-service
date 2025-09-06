package com.trademaster.mlinfra.domain.types;

/**
 * ML Experiment Status Enumeration
 * 
 * Represents the various states of a machine learning experiment:
 * - RUNNING: Experiment is currently executing
 * - COMPLETED: Experiment finished successfully
 * - FAILED: Experiment terminated with errors
 * - CANCELLED: Experiment was manually cancelled
 * - PAUSED: Experiment execution is paused
 */
public enum ExperimentStatus {
    RUNNING("Experiment is currently running"),
    COMPLETED("Experiment completed successfully"),
    FAILED("Experiment failed with errors"),
    CANCELLED("Experiment was cancelled"),
    PAUSED("Experiment execution is paused");

    private final String description;

    ExperimentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if status represents a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Check if status represents an active state
     */
    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }

    /**
     * Check if status represents a successful completion
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }
}