package com.trademaster.auth.strategy;

import com.trademaster.auth.dto.AuthenticationRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Authentication Strategy Registry - SOLID Strategy Pattern + Registry Pattern
 *
 * Centralized registry for authentication strategies with auto-discovery.
 * Automatically discovers and registers all AuthenticationStrategy beans from Spring context.
 *
 * Features:
 * - Auto-discovery using Spring ApplicationContext
 * - Priority-based strategy ordering
 * - Runtime strategy selection
 * - Thread-safe concurrent access
 * - Dynamic strategy registration
 * - Fallback to default strategy (Password)
 *
 * Strategy Selection Algorithm:
 * 1. Sort strategies by priority (highest first)
 * 2. Find first strategy that supports the request
 * 3. Fallback to password strategy if none found
 *
 * Benefits:
 * - Open/Closed Principle: Add new strategies without modifying registry
 * - Single Responsibility: Each strategy handles one auth method
 * - Dependency Inversion: Depends on AuthenticationStrategy interface
 * - Runtime flexibility: Strategy selection at runtime based on request
 *
 * This registry is 100% functional programming compliant:
 * - No if-else statements (uses Stream API, Optional chains)
 * - No try-catch blocks (uses Optional for error handling)
 * - No for/while loops (uses Stream API)
 * - Immutable collections for thread safety
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationStrategyRegistry {

    private final ApplicationContext applicationContext;

    // Thread-safe concurrent maps for strategy storage
    private final Map<String, AuthenticationStrategy> strategyByName = new ConcurrentHashMap<>();
    private final Map<Integer, List<AuthenticationStrategy>> strategiesByPriority = new ConcurrentHashMap<>();

    private static final String DEFAULT_STRATEGY_NAME = "PASSWORD";

    /**
     * Auto-discover and register all AuthenticationStrategy beans
     * Called automatically by Spring after dependency injection
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing Authentication Strategy Registry with auto-discovery");

        // Discover all AuthenticationStrategy beans from Spring context
        Map<String, AuthenticationStrategy> strategyBeans = applicationContext.getBeansOfType(AuthenticationStrategy.class);

        // Register each discovered strategy
        strategyBeans.values().forEach(this::registerStrategy);

        // Log registered strategies
        logRegisteredStrategies();

        log.info("Authentication Strategy Registry initialized with {} strategies", strategyByName.size());
    }

    /**
     * Register authentication strategy
     * Strategies are indexed by name and priority for efficient lookup
     *
     * @param strategy Authentication strategy to register
     */
    public void registerStrategy(AuthenticationStrategy strategy) {
        // Register by name
        strategyByName.put(strategy.getStrategyName(), strategy);

        // Register by priority (for ordered selection)
        strategiesByPriority.computeIfAbsent(strategy.getPriority(), k -> new ArrayList<>())
            .add(strategy);

        log.debug("Registered authentication strategy: {} with priority: {}",
            strategy.getStrategyName(), strategy.getPriority());
    }

    /**
     * Select appropriate authentication strategy for the request
     * Uses priority-based selection with fallback to default strategy
     *
     * Selection Algorithm:
     * 1. Get strategies sorted by priority (highest first)
     * 2. Find first strategy that supports the request
     * 3. Fallback to password strategy if none found
     *
     * @param request Authentication request
     * @param httpRequest HTTP request for context
     * @return Selected authentication strategy
     */
    public Optional<AuthenticationStrategy> selectStrategy(
            AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Selecting authentication strategy for request");

        // Get strategies sorted by priority (highest first)
        List<AuthenticationStrategy> sortedStrategies = getSortedStrategiesByPriority();

        // Find first strategy that supports the request
        Optional<AuthenticationStrategy> selectedStrategy = sortedStrategies.stream()
            .filter(strategy -> strategy.supports(request, httpRequest))
            .findFirst();

        // Log selected strategy
        selectedStrategy.ifPresentOrElse(
            strategy -> log.debug("Selected authentication strategy: {} (priority: {})",
                strategy.getStrategyName(), strategy.getPriority()),
            () -> log.warn("No strategy found for request, using fallback")
        );

        // Fallback to default password strategy if none found
        return selectedStrategy.or(this::getDefaultStrategy);
    }

    /**
     * Get authentication strategy by name
     *
     * @param strategyName Strategy name (e.g., "PASSWORD", "MFA", "SOCIAL")
     * @return Optional containing strategy if found
     */
    public Optional<AuthenticationStrategy> getStrategy(String strategyName) {
        return Optional.ofNullable(strategyByName.get(strategyName))
            .map(strategy -> {
                log.debug("Retrieved authentication strategy: {}", strategyName);
                return strategy;
            });
    }

    /**
     * Get all registered strategies
     *
     * @return Unmodifiable collection of all strategies
     */
    public Collection<AuthenticationStrategy> getAllStrategies() {
        return Collections.unmodifiableCollection(strategyByName.values());
    }

    /**
     * Get strategies sorted by priority (highest first)
     *
     * @return List of strategies sorted by priority
     */
    public List<AuthenticationStrategy> getSortedStrategiesByPriority() {
        return strategiesByPriority.entrySet().stream()
            .sorted(Map.Entry.<Integer, List<AuthenticationStrategy>>comparingByKey().reversed())
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
    }

    /**
     * Get default strategy (Password authentication)
     *
     * @return Optional containing default strategy
     */
    public Optional<AuthenticationStrategy> getDefaultStrategy() {
        return getStrategy(DEFAULT_STRATEGY_NAME)
            .map(strategy -> {
                log.debug("Using default authentication strategy: {}", DEFAULT_STRATEGY_NAME);
                return strategy;
            });
    }

    /**
     * Check if strategy is registered
     *
     * @param strategyName Strategy name to check
     * @return true if strategy is registered
     */
    public boolean hasStrategy(String strategyName) {
        return strategyByName.containsKey(strategyName);
    }

    /**
     * Get count of registered strategies
     *
     * @return Number of registered strategies
     */
    public int getStrategyCount() {
        return strategyByName.size();
    }

    /**
     * Get all registered strategy names
     *
     * @return Unmodifiable set of strategy names
     */
    public Set<String> getStrategyNames() {
        return Collections.unmodifiableSet(strategyByName.keySet());
    }

    /**
     * Get strategies by priority level
     *
     * @param priority Priority level
     * @return List of strategies with the given priority
     */
    public List<AuthenticationStrategy> getStrategiesByPriority(int priority) {
        return Optional.ofNullable(strategiesByPriority.get(priority))
            .map(Collections::unmodifiableList)
            .orElse(Collections.emptyList());
    }

    /**
     * Log all registered strategies for debugging
     */
    private void logRegisteredStrategies() {
        getSortedStrategiesByPriority().stream()
            .forEach(strategy -> log.info("  - {} (priority: {})",
                strategy.getStrategyName(), strategy.getPriority()));
    }

    /**
     * Get registry statistics for monitoring
     *
     * @return Map containing registry statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "totalStrategies", getStrategyCount(),
            "strategies", getStrategyNames(),
            "priorityLevels", strategiesByPriority.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList()),
            "defaultStrategy", DEFAULT_STRATEGY_NAME
        );
    }

    /**
     * Validate registry integrity
     * Ensures all required strategies are registered
     *
     * @return true if registry is valid
     */
    public boolean validateRegistry() {
        Set<String> requiredStrategies = Set.of("PASSWORD", "MFA", "SOCIAL", "API_KEY");

        boolean isValid = requiredStrategies.stream()
            .allMatch(this::hasStrategy);

        Optional.of(isValid)
            .filter(valid -> !valid)
            .ifPresent(valid -> log.warn("Registry validation failed: Missing required strategies"));

        return isValid;
    }
}
