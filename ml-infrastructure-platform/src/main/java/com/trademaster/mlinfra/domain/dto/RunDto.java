package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Run DTO
 * 
 * Data transfer object for MLflow run information following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record RunDto(
    @NotBlank(message = "Run ID cannot be blank")
    String runId,
    
    @NotBlank(message = "Experiment ID cannot be blank") 
    String experimentId,
    
    @NotNull(message = "Status cannot be null")
    RunStatus status,
    
    @NotBlank(message = "User ID cannot be blank")
    String userId,
    
    String name,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant startTime,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant endTime,
    
    Map<String, String> tags,
    
    Map<String, Object> params,
    
    Map<String, Double> metrics,
    
    String artifactUri,
    
    String lifecycleStage
) {
    
    /**
     * Run status enumeration
     */
    public enum RunStatus {
        RUNNING, SCHEDULED, FINISHED, FAILED, KILLED
    }
    
    /**
     * Builder pattern for RunDto
     */
    public static RunDtoBuilder builder() {
        return new RunDtoBuilder();
    }

    /**
     * Factory method for active run
     */
    public static RunDto active(String runId, String experimentId, String userId, String name) {
        return new RunDto(
            runId,
            experimentId,
            RunStatus.RUNNING,
            userId,
            name,
            Instant.now(),
            null,
            Map.of(),
            Map.of(),
            Map.of(),
            null,
            "active"
        );
    }
    
    /**
     * Check if run is active
     */
    public boolean isActive() {
        return status == RunStatus.RUNNING || status == RunStatus.SCHEDULED;
    }
    
    /**
     * Check if run is completed
     */
    public boolean isCompleted() {
        return status == RunStatus.FINISHED || status == RunStatus.FAILED || status == RunStatus.KILLED;
    }
    
    /**
     * Get duration if run is completed
     */
    public java.time.Duration getDuration() {
        return (startTime != null && endTime != null) 
            ? java.time.Duration.between(startTime, endTime)
            : java.time.Duration.ZERO;
    }

    /**
     * Builder class for RunDto
     */
    public static class RunDtoBuilder {
        private String runId;
        private String experimentId;
        private RunStatus status = RunStatus.RUNNING;
        private String userId;
        private String name;
        private Instant startTime = Instant.now();
        private Instant endTime;
        private Map<String, String> tags = Map.of();
        private Map<String, Object> params = Map.of();
        private Map<String, Double> metrics = Map.of();
        private String artifactUri;
        private String lifecycleStage = "active";

        public RunDtoBuilder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public RunDtoBuilder experimentId(String experimentId) {
            this.experimentId = experimentId;
            return this;
        }

        public RunDtoBuilder status(RunStatus status) {
            this.status = status;
            return this;
        }

        public RunDtoBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RunDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RunDtoBuilder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public RunDtoBuilder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public RunDtoBuilder tags(Map<String, String> tags) {
            this.tags = tags != null ? Map.copyOf(tags) : Map.of();
            return this;
        }

        public RunDtoBuilder params(Map<String, Object> params) {
            this.params = params != null ? Map.copyOf(params) : Map.of();
            return this;
        }

        public RunDtoBuilder metrics(Map<String, Double> metrics) {
            this.metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
            return this;
        }

        public RunDtoBuilder artifactUri(String artifactUri) {
            this.artifactUri = artifactUri;
            return this;
        }

        public RunDtoBuilder lifecycleStage(String lifecycleStage) {
            this.lifecycleStage = lifecycleStage;
            return this;
        }

        public RunDto build() {
            return new RunDto(runId, experimentId, status, userId, name, 
                           startTime, endTime, tags, params, metrics, 
                           artifactUri, lifecycleStage);
        }
    }
}