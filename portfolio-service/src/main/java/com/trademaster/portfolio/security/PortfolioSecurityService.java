package com.trademaster.portfolio.security;

import com.trademaster.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
     * Check if user can access the specified portfolio
     */
    public boolean canAccessPortfolio(UUID portfolioId, String username) {
        try {
            boolean hasAccess = portfolioRepository.existsByIdAndOwnerUsernameOrSharedWith(
                portfolioId, username, username);
            
            if (!hasAccess) {
                log.warn("Access denied: User {} attempted to access portfolio {}", 
                        username, portfolioId);
            }
            
            return hasAccess;
        } catch (Exception e) {
            log.error("Error checking portfolio access for user {} and portfolio {}: {}", 
                     username, portfolioId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if user can modify the specified portfolio
     */
    public boolean canModifyPortfolio(UUID portfolioId, String username) {
        try {
            boolean canModify = portfolioRepository.existsByIdAndOwnerUsernameOrModifyPermission(
                portfolioId, username, username);
            
            if (!canModify) {
                log.warn("Modification denied: User {} attempted to modify portfolio {}", 
                        username, portfolioId);
            }
            
            return canModify;
        } catch (Exception e) {
            log.error("Error checking portfolio modification rights for user {} and portfolio {}: {}", 
                     username, portfolioId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if user can delete the specified portfolio
     */
    public boolean canDeletePortfolio(UUID portfolioId, String username) {
        try {
            boolean canDelete = portfolioRepository.existsByIdAndOwnerUsername(portfolioId, username);
            
            if (!canDelete) {
                log.warn("Deletion denied: User {} attempted to delete portfolio {}", 
                        username, portfolioId);
            }
            
            return canDelete;
        } catch (Exception e) {
            log.error("Error checking portfolio deletion rights for user {} and portfolio {}: {}", 
                     username, portfolioId, e.getMessage());
            return false;
        }
    }
}