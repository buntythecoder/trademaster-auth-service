package com.trademaster.auth.pattern;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Virtual Thread Factory Pattern Implementation
 * 
 * Provides consistent virtual thread management across the authentication service
 * following TradeMaster Advanced Design Patterns standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum VirtualThreadFactory {
    INSTANCE;

    /**
     * -- GETTER --
     *  Get the underlying executor service for advanced use cases
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * Execute a runnable task asynchronously on virtual threads
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }
    
    /**
     * Execute a supplier task asynchronously on virtual threads
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * Create a virtual thread factory with custom naming
     */
    public static ThreadFactory createFactory(String namePrefix) {
        return Thread.ofVirtual()
            .name(namePrefix, 0)
            .factory();
    }
    
    /**
     * Create a virtual thread executor with custom naming
     */
    public static ExecutorService createExecutor(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(createFactory(namePrefix));
    }
    
    /**
     * Shutdown the executor (typically called during application shutdown)
     */
    public void shutdown() {
        executor.shutdown();
    }
}