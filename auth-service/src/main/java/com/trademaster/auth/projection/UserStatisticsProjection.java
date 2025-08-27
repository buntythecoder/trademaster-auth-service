package com.trademaster.auth.projection;

/**
 * Projection interface for user statistics query results
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface UserStatisticsProjection {
    Long getTotalUsers();
    Long getActiveUsers();
    Long getLockedUsers();
    Long getSuspendedUsers();
    Long getVerifiedUsers();
    Long getRecentLogins();
}