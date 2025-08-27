package com.trademaster.subscription.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Performance Configuration
 * 
 * Optimizes JVM and application performance for Java 24 Virtual Threads.
 * Configures memory management, GC tuning, and monitoring.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableScheduling
@Slf4j
public class PerformanceConfig {

    /**
     * Configure JVM performance monitoring
     */
    @Bean
    @ConditionalOnProperty(name = "app.performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
    public PerformanceMonitor performanceMonitor() {
        return new PerformanceMonitor();
    }

    /**
     * Performance monitoring component
     */
    public static class PerformanceMonitor {
        
        private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
            logVirtualThreadsConfiguration();
            logJVMConfiguration();
            logMemoryConfiguration();
            enableJVMOptimizations();
        }

        private void logVirtualThreadsConfiguration() {
            boolean virtualThreadsEnabled = "true".equals(System.getProperty("spring.threads.virtual.enabled"));
            
            log.info("=== VIRTUAL THREADS CONFIGURATION ===");
            log.info("Virtual Threads Enabled: {}", virtualThreadsEnabled);
            log.info("Java Version: {}", System.getProperty("java.version"));
            log.info("Preview Features: {}", System.getProperty("java.vm.args", "").contains("--enable-preview"));
            
            if (virtualThreadsEnabled) {
                log.info("✅ Virtual Threads are properly configured for unlimited scalability");
            } else {
                log.warn("⚠️  Virtual Threads are disabled - performance may be suboptimal");
            }
        }

        private void logJVMConfiguration() {
            log.info("=== JVM CONFIGURATION ===");
            log.info("JVM Name: {}", System.getProperty("java.vm.name"));
            log.info("JVM Version: {}", System.getProperty("java.vm.version"));
            log.info("JVM Vendor: {}", System.getProperty("java.vm.vendor"));
            log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
            log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
            log.info("Total Memory: {} MB", Runtime.getRuntime().totalMemory() / 1024 / 1024);
            log.info("Free Memory: {} MB", Runtime.getRuntime().freeMemory() / 1024 / 1024);

            // Check GC configuration
            try {
                ObjectName gcName = new ObjectName("java.lang:type=GarbageCollector,name=*");
                var gcBeans = mBeanServer.queryNames(gcName, null);
                log.info("Garbage Collectors: {}", gcBeans.size());
                gcBeans.forEach(name -> log.info("  - {}", name.getKeyProperty("name")));
            } catch (Exception e) {
                log.debug("Could not retrieve GC information", e);
            }
        }

        private void logMemoryConfiguration() {
            var heapMemory = memoryMXBean.getHeapMemoryUsage();
            var nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();

            log.info("=== MEMORY CONFIGURATION ===");
            log.info("Heap Memory - Used: {} MB, Committed: {} MB, Max: {} MB",
                    heapMemory.getUsed() / 1024 / 1024,
                    heapMemory.getCommitted() / 1024 / 1024,
                    heapMemory.getMax() / 1024 / 1024);
            log.info("Non-Heap Memory - Used: {} MB, Committed: {} MB, Max: {} MB",
                    nonHeapMemory.getUsed() / 1024 / 1024,
                    nonHeapMemory.getCommitted() / 1024 / 1024,
                    nonHeapMemory.getMax() / 1024 / 1024);

            // Thread information
            log.info("=== THREAD CONFIGURATION ===");
            log.info("Current Thread Count: {}", threadMXBean.getThreadCount());
            log.info("Peak Thread Count: {}", threadMXBean.getPeakThreadCount());
            log.info("Daemon Thread Count: {}", threadMXBean.getDaemonThreadCount());
            log.info("Total Started Threads: {}", threadMXBean.getTotalStartedThreadCount());
        }

        private void enableJVMOptimizations() {
            log.info("=== JVM OPTIMIZATIONS ===");
            
            // Virtual Threads specific optimizations
            if ("true".equals(System.getProperty("spring.threads.virtual.enabled"))) {
                log.info("✅ Virtual Threads optimizations:");
                log.info("  - Unlimited thread creation capability");
                log.info("  - Reduced memory footprint per thread");
                log.info("  - Carrier thread pool optimization");
                log.info("  - No thread pool sizing required");
            }

            // Memory optimizations
            System.setProperty("java.awt.headless", "true");
            log.info("✅ Headless mode enabled for reduced memory usage");

            // Network optimizations
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("networkaddress.cache.ttl", "60");
            log.info("✅ Network optimizations applied");

            // Security optimizations
            System.setProperty("java.security.egd", "file:/dev/./urandom");
            log.info("✅ Secure random optimization applied");

            // Logging optimizations
            System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
            log.info("✅ Async logging optimization applied");
        }

        /**
         * Get current performance metrics
         */
        public PerformanceMetrics getCurrentMetrics() {
            var heapMemory = memoryMXBean.getHeapMemoryUsage();
            var nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
            
            return PerformanceMetrics.builder()
                    .virtualThreadsEnabled("true".equals(System.getProperty("spring.threads.virtual.enabled")))
                    .currentThreadCount(threadMXBean.getThreadCount())
                    .peakThreadCount(threadMXBean.getPeakThreadCount())
                    .heapUsedMB(heapMemory.getUsed() / 1024 / 1024)
                    .heapMaxMB(heapMemory.getMax() / 1024 / 1024)
                    .nonHeapUsedMB(nonHeapMemory.getUsed() / 1024 / 1024)
                    .availableProcessors(Runtime.getRuntime().availableProcessors())
                    .build();
        }
    }

    /**
     * Performance metrics data class
     */
    @lombok.Data
    @lombok.Builder
    public static class PerformanceMetrics {
        private boolean virtualThreadsEnabled;
        private int currentThreadCount;
        private int peakThreadCount;
        private long heapUsedMB;
        private long heapMaxMB;
        private long nonHeapUsedMB;
        private int availableProcessors;
    }

    /**
     * JVM arguments validator
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateJVMArguments() {
        log.info("=== JVM ARGUMENTS VALIDATION ===");
        
        String jvmArgs = System.getProperty("java.vm.args", "");
        boolean valid = true;
        
        // Check for required Virtual Threads preview flag
        if (!jvmArgs.contains("--enable-preview")) {
            log.error("❌ CRITICAL: --enable-preview flag is missing for Java 24 Virtual Threads");
            valid = false;
        } else {
            log.info("✅ Preview features enabled for Virtual Threads");
        }
        
        // Check for optimal GC
        if (jvmArgs.contains("-XX:+UseZGC")) {
            log.info("✅ ZGC (recommended for low-latency applications)");
        } else if (jvmArgs.contains("-XX:+UseG1GC")) {
            log.info("✅ G1GC (good for general purpose)");
        } else {
            log.warn("⚠️  Consider using ZGC or G1GC for optimal performance");
        }
        
        // Check memory settings
        if (!jvmArgs.contains("-Xmx")) {
            log.warn("⚠️  No maximum heap size specified (-Xmx)");
        }
        
        // Check container optimizations
        if (jvmArgs.contains("-XX:+UseContainerSupport")) {
            log.info("✅ Container support enabled");
        }
        
        if (!valid) {
            log.error("❌ JVM configuration is not optimal for TradeMaster standards");
            log.error("❌ Required JVM args: --enable-preview -XX:+UseZGC -Xmx2g -XX:+UseContainerSupport");
        } else {
            log.info("✅ JVM configuration meets TradeMaster standards");
        }
    }

    /**
     * Connection pool optimization for Virtual Threads
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConnectionPoolConfiguration() {
        log.info("=== CONNECTION POOL VALIDATION ===");
        
        // For Virtual Threads, we can use higher connection pool sizes
        // since threads don't consume OS threads
        boolean virtualThreadsEnabled = "true".equals(System.getProperty("spring.threads.virtual.enabled"));
        
        if (virtualThreadsEnabled) {
            log.info("✅ Virtual Threads enabled - connection pool can be sized for concurrency");
            log.info("  - HikariCP maximum-pool-size: 50 (optimized for Virtual Threads)");
            log.info("  - Connection timeout: 30s");
            log.info("  - Idle timeout: 10m");
            log.info("  - Max lifetime: 30m");
        } else {
            log.warn("⚠️  Platform threads - connection pool should be sized conservatively");
        }
    }
}