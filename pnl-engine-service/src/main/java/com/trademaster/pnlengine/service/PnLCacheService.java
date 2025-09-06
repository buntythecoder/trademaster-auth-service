package com.trademaster.pnlengine.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * P&L Cache Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance caching service for P&L calculations providing
 * sub-50ms response times with intelligent cache invalidation and
 * distributed caching capabilities.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PnLCacheService {
    
    <T> CompletableFuture<Optional<T>> get(String key, Class<T> type);
    
    <T> CompletableFuture<Void> put(String key, T value, long ttlSeconds);
    
    CompletableFuture<Void> invalidate(String key);
    
    CompletableFuture<Void> invalidateUserCache(String userId);
}