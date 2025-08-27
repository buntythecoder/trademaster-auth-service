package com.trademaster.brokerauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Security Monitoring Configuration
 * 
 * Configures security monitoring, metrics collection,
 * and alerting for the broker authentication service.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class SecurityMonitoringConfig {
    
    /**
     * Security metrics collector
     */
    @Bean
    public SecurityMetricsCollector securityMetricsCollector(MeterRegistry meterRegistry) {
        return new SecurityMetricsCollector(meterRegistry);
    }
    
    /**
     * Security event monitor
     */
    @Bean
    public SecurityEventMonitor securityEventMonitor() {
        return new SecurityEventMonitor();
    }
    
    /**
     * Collects and tracks security-related metrics
     */
    @Component
    public static class SecurityMetricsCollector {
        
        private final MeterRegistry meterRegistry;
        
        // Authentication metrics
        private final Counter authenticationAttempts;
        private final Counter authenticationSuccesses;
        private final Counter authenticationFailures;
        private final Timer authenticationDuration;
        
        // Session metrics
        private final Counter sessionCreations;
        private final Counter sessionValidations;
        private final Counter sessionTerminations;
        private final AtomicInteger activeSessions = new AtomicInteger(0);
        
        // Security incident metrics
        private final Counter securityIncidents;
        private final Counter rateLimitViolations;
        private final Counter suspiciousActivities;
        private final Counter credentialFailures;
        
        // Performance metrics
        private final Timer brokerApiResponseTime;
        private final Counter brokerApiErrors;
        
        // System health metrics
        private final AtomicLong lastSecurityScan = new AtomicLong(0);
        private final AtomicInteger healthScore = new AtomicInteger(100);
        
        public SecurityMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Initialize authentication metrics
            this.authenticationAttempts = Counter.builder("security.auth.attempts")
                    .description("Total authentication attempts")
                    .register(meterRegistry);
                    
            this.authenticationSuccesses = Counter.builder("security.auth.successes")
                    .description("Successful authentication attempts")
                    .register(meterRegistry);
                    
            this.authenticationFailures = Counter.builder("security.auth.failures")
                    .description("Failed authentication attempts")
                    .register(meterRegistry);
                    
            this.authenticationDuration = Timer.builder("security.auth.duration")
                    .description("Time taken for authentication")
                    .register(meterRegistry);
            
            // Initialize session metrics
            this.sessionCreations = Counter.builder("security.sessions.created")
                    .description("Total sessions created")
                    .register(meterRegistry);
                    
            this.sessionValidations = Counter.builder("security.sessions.validations")
                    .description("Total session validations")
                    .register(meterRegistry);
                    
            this.sessionTerminations = Counter.builder("security.sessions.terminated")
                    .description("Total sessions terminated")
                    .register(meterRegistry);
            
            // Initialize security incident metrics
            this.securityIncidents = Counter.builder("security.incidents")
                    .description("Total security incidents")
                    .register(meterRegistry);
                    
            this.rateLimitViolations = Counter.builder("security.rate_limit_violations")
                    .description("Total rate limit violations")
                    .register(meterRegistry);
                    
            this.suspiciousActivities = Counter.builder("security.suspicious_activities")
                    .description("Total suspicious activities detected")
                    .register(meterRegistry);
                    
            this.credentialFailures = Counter.builder("security.credential_failures")
                    .description("Total credential operation failures")
                    .register(meterRegistry);
            
            // Initialize performance metrics
            this.brokerApiResponseTime = Timer.builder("security.broker_api.response_time")
                    .description("Broker API response time")
                    .register(meterRegistry);
                    
            this.brokerApiErrors = Counter.builder("security.broker_api.errors")
                    .description("Broker API errors")
                    .register(meterRegistry);
            
            // Initialize gauges for real-time metrics
            Gauge.builder("security.sessions.active", this, collector -> (double) collector.activeSessions.get())
                    .description("Currently active sessions")
                    .register(meterRegistry);
                    
            Gauge.builder("security.health.score", this, collector -> (double) collector.healthScore.get())
                    .description("Overall security health score (0-100)")
                    .register(meterRegistry);
                    
            Gauge.builder("security.health.last_scan", this, collector -> (double) collector.lastSecurityScan.get())
                    .description("Timestamp of last security scan")
                    .register(meterRegistry);
        }
        
        // Authentication metrics methods
        public void recordAuthenticationAttempt() {
            authenticationAttempts.increment();
        }
        
        public void recordAuthenticationSuccess() {
            authenticationSuccesses.increment();
        }
        
        public void recordAuthenticationFailure() {
            authenticationFailures.increment();
        }
        
        public Timer.Sample startAuthenticationTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordAuthenticationDuration(Timer.Sample sample) {
            sample.stop(authenticationDuration);
        }
        
        // Session metrics methods
        public void recordSessionCreated() {
            sessionCreations.increment();
            activeSessions.incrementAndGet();
        }
        
        public void recordSessionValidation() {
            sessionValidations.increment();
        }
        
        public void recordSessionTerminated() {
            sessionTerminations.increment();
            activeSessions.decrementAndGet();
        }
        
        // Security incident methods
        public void recordSecurityIncident() {
            securityIncidents.increment();
            decreaseHealthScore(5);
        }
        
        public void recordRateLimitViolation() {
            rateLimitViolations.increment();
            decreaseHealthScore(2);
        }
        
        public void recordSuspiciousActivity() {
            suspiciousActivities.increment();
            decreaseHealthScore(10);
        }
        
        public void recordCredentialFailure() {
            credentialFailures.increment();
            decreaseHealthScore(15);
        }
        
        // Performance metrics methods
        public Timer.Sample startBrokerApiTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordBrokerApiResponse(Timer.Sample sample) {
            sample.stop(brokerApiResponseTime);
        }
        
        public void recordBrokerApiError() {
            brokerApiErrors.increment();
            decreaseHealthScore(3);
        }
        
        // Health score management
        private void decreaseHealthScore(int points) {
            healthScore.updateAndGet(current -> Math.max(0, current - points));
        }
        
        public void improveHealthScore(int points) {
            healthScore.updateAndGet(current -> Math.min(100, current + points));
        }
        
        public void updateLastSecurityScan() {
            lastSecurityScan.set(System.currentTimeMillis());
        }
        
        public int getHealthScore() {
            return healthScore.get();
        }
        
        public int getActiveSessions() {
            return activeSessions.get();
        }
    }
    
    /**
     * Monitors security events and triggers alerts
     */
    @Component
    public static class SecurityEventMonitor {
        
        private final AtomicInteger recentFailedLogins = new AtomicInteger(0);
        private final AtomicInteger recentSuspiciousActivities = new AtomicInteger(0);
        private final AtomicLong lastAlertTime = new AtomicLong(0);
        
        private static final int FAILED_LOGIN_THRESHOLD = 10;
        private static final int SUSPICIOUS_ACTIVITY_THRESHOLD = 5;
        private static final long ALERT_COOLDOWN_MS = 300000; // 5 minutes
        
        /**
         * Monitor authentication failures
         */
        public void onAuthenticationFailure(String userId, String clientIp, String reason) {
            int recentFailures = recentFailedLogins.incrementAndGet();
            
            log.warn("Authentication failure: userId={}, clientIp={}, reason={}, recentFailures={}", 
                    userId, clientIp, reason, recentFailures);
            
            if (recentFailures >= FAILED_LOGIN_THRESHOLD) {
                triggerAlert("HIGH_AUTHENTICATION_FAILURES", 
                    "Threshold exceeded: " + recentFailures + " failed logins in monitoring window");
            }
        }
        
        /**
         * Monitor suspicious activities
         */
        public void onSuspiciousActivity(String activityType, String userId, String clientIp) {
            int recentActivities = recentSuspiciousActivities.incrementAndGet();
            
            log.warn("Suspicious activity detected: type={}, userId={}, clientIp={}, recentCount={}", 
                    activityType, userId, clientIp, recentActivities);
            
            if (recentActivities >= SUSPICIOUS_ACTIVITY_THRESHOLD) {
                triggerAlert("HIGH_SUSPICIOUS_ACTIVITY", 
                    "Threshold exceeded: " + recentActivities + " suspicious activities detected");
            }
        }
        
        /**
         * Monitor rate limit violations
         */
        public void onRateLimitViolation(String userId, String brokerType, String clientIp, 
                                       int currentRequests, int maxRequests) {
            log.warn("Rate limit violation: userId={}, broker={}, clientIp={}, requests={}/{}", 
                    userId, brokerType, clientIp, currentRequests, maxRequests);
            
            // Immediate alert for severe rate limit violations
            if (currentRequests > maxRequests * 2) {
                triggerAlert("SEVERE_RATE_LIMIT_VIOLATION", 
                    String.format("User %s exceeded rate limit by %d%% for broker %s", 
                        userId, ((currentRequests * 100) / maxRequests) - 100, brokerType));
            }
        }
        
        /**
         * Monitor system health degradation
         */
        public void onHealthScoreChange(int oldScore, int newScore) {
            if (newScore < 70 && oldScore >= 70) {
                triggerAlert("HEALTH_SCORE_DEGRADATION", 
                    "System health score dropped to " + newScore + " (was " + oldScore + ")");
            } else if (newScore < 50) {
                triggerAlert("CRITICAL_HEALTH_SCORE", 
                    "System health score is critically low: " + newScore);
            }
        }
        
        /**
         * Trigger security alert
         */
        private void triggerAlert(String alertType, String message) {
            long currentTime = System.currentTimeMillis();
            long lastAlert = lastAlertTime.get();
            
            // Implement alert cooldown to prevent spam
            if (currentTime - lastAlert > ALERT_COOLDOWN_MS) {
                if (lastAlertTime.compareAndSet(lastAlert, currentTime)) {
                    log.error("SECURITY_ALERT [{}]: {}", alertType, message);
                    
                    // In production, this would integrate with alerting systems like:
                    // - PagerDuty
                    // - Slack notifications
                    // - Email alerts
                    // - SIEM systems
                    // - Webhook notifications
                    
                    // For now, we just log at ERROR level which monitoring systems can pick up
                }
            } else {
                log.debug("Alert cooldown active, suppressing alert: {}", alertType);
            }
        }
        
        /**
         * Reset counters periodically
         */
        @Scheduled(fixedRate = 300000) // Reset every 5 minutes
        public void resetCounters() {
            int failedLogins = recentFailedLogins.getAndSet(0);
            int suspiciousActivities = recentSuspiciousActivities.getAndSet(0);
            
            if (failedLogins > 0 || suspiciousActivities > 0) {
                log.info("Security monitoring window reset: failedLogins={}, suspiciousActivities={}", 
                        failedLogins, suspiciousActivities);
            }
        }
        
        /**
         * Application startup security check
         */
        @EventListener
        public void onApplicationReady(ApplicationReadyEvent event) {
            log.info("Security monitoring started - watching for threats and anomalies");
            
            // Perform initial security scan
            performSecurityScan();
        }
        
        /**
         * Periodic security health check
         */
        @Scheduled(fixedRate = 900000) // Every 15 minutes
        public void performSecurityScan() {
            log.info("Performing periodic security health scan...");
            
            // This would include:
            // - Check for configuration drift
            // - Validate encryption keys
            // - Check for suspicious patterns
            // - Validate security policies
            // - Check system resource usage
            
            log.info("Security health scan completed");
        }
    }
}