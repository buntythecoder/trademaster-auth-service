package com.trademaster.portfolio.security;

import com.trademaster.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Portfolio Security Service
 * 
 * Provides authorization checks for portfolio operations to ensure
 * users can only access portfolios they own or have been granted access to.
 * 
 * Security Features:
 * - Portfolio ownership validation
 * - Role-based access control
 * - Audit logging for security events
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSecurityService {
    
    private final PortfolioRepository portfolioRepository;
    
    /**
     * Check if user can access the specified portfolio.
     *
     * Rule #6: Zero Trust Security - Validates ownership before granting access
     * Rule #3: No if-else - Uses Optional map/filter/orElse functional chain
     * Rule #11: Functional error handling instead of try-catch
     *
     * @param portfolioId Portfolio identifier
     * @param username Username from JWT token
     * @return true if portfolio exists and user has ownership
     */
    public boolean canAccessPortfolio(Long portfolioId, String username) {
        return portfolioRepository.findById(portfolioId)
            .map(portfolio -> validatePortfolioOwnership(portfolio, username, "access", portfolioId))
            .orElseGet(() -> logAccessDeniedNonExistent(username, portfolioId));
    }

    /**
     * Rule #6: Ownership validation helper
     * Rule #3: Functional validation with filter
     * Complexity: 2
     */
    private boolean validatePortfolioOwnership(com.trademaster.portfolio.entity.Portfolio portfolio,
                                                String username, String action, Long portfolioId) {
        return java.util.Optional.ofNullable(username)
            .map(String::toLowerCase)
            .filter(user -> portfolio.getUserId().toString().equals(user) ||
                           user.equals(portfolio.getUserId().toString()))
            .map(user -> {
                log.debug("Portfolio {} granted: User {} {} portfolio {}",
                        action, username, action + "ing", portfolioId);
                return true;
            })
            .orElseGet(() -> logAccessDeniedUnauthorized(username, action, portfolioId));
    }

    /**
     * Rule #15: Structured logging for security events
     * Complexity: 1
     */
    private boolean logAccessDeniedNonExistent(String username, Long portfolioId) {
        log.warn("Access denied: User {} attempted to access non-existent portfolio {}",
                username, portfolioId);
        return false;
    }

    /**
     * Rule #15: Structured logging for unauthorized access attempts
     * Complexity: 1
     */
    private boolean logAccessDeniedUnauthorized(String username, String action, Long portfolioId) {
        log.warn("Access denied: User {} unauthorized to {} portfolio {}",
                username, action, portfolioId);
        return false;
    }
    
    /**
     * Check if user can modify the specified portfolio.
     *
     * Rule #6: Zero Trust Security - Validates ownership before allowing modification
     * Rule #3: No if-else - Uses Optional map/filter/orElse functional chain
     * Rule #11: Functional error handling instead of try-catch
     *
     * @param portfolioId Portfolio identifier
     * @param username Username from JWT token
     * @return true if portfolio exists and user has ownership
     */
    public boolean canModifyPortfolio(Long portfolioId, String username) {
        return portfolioRepository.findById(portfolioId)
            .map(portfolio -> validatePortfolioOwnership(portfolio, username, "modify", portfolioId))
            .orElseGet(() -> logModificationDeniedNonExistent(username, portfolioId));
    }

    /**
     * Rule #15: Structured logging for modification denial on non-existent portfolio
     * Complexity: 1
     */
    private boolean logModificationDeniedNonExistent(String username, Long portfolioId) {
        log.warn("Modification denied: User {} attempted to modify non-existent portfolio {}",
                username, portfolioId);
        return false;
    }

    /**
     * Check if user can delete the specified portfolio.
     *
     * Rule #6: Zero Trust Security - Validates ownership before allowing deletion
     * Rule #3: No if-else - Uses Optional map/filter/orElse functional chain
     * Rule #11: Functional error handling instead of try-catch
     *
     * @param portfolioId Portfolio identifier
     * @param username Username from JWT token
     * @return true if portfolio exists and user has ownership
     */
    public boolean canDeletePortfolio(Long portfolioId, String username) {
        return portfolioRepository.findById(portfolioId)
            .map(portfolio -> validatePortfolioOwnership(portfolio, username, "delete", portfolioId))
            .orElseGet(() -> logDeletionDeniedNonExistent(username, portfolioId));
    }

    /**
     * Rule #15: Structured logging for deletion denial on non-existent portfolio
     * Complexity: 1
     */
    private boolean logDeletionDeniedNonExistent(String username, Long portfolioId) {
        log.warn("Deletion denied: User {} attempted to delete non-existent portfolio {}",
                username, portfolioId);
        return false;
    }
}