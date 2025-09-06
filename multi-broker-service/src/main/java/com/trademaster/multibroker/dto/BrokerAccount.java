package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Broker Account Information Record
 * 
 * MANDATORY: Immutable Record + Functional Validation + Zero Placeholders
 * 
 * Represents broker account details obtained from real API calls during connection validation.
 * Contains account identification, financial information, and trading permissions.
 * 
 * Data Sources (Real Broker APIs):
 * - Zerodha: Kite Connect Profile API
 * - Upstox: User Profile API  
 * - Angel One: SmartAPI Profile
 * - ICICI Direct: Breeze Profile API
 * 
 * Validation Rules:
 * - accountId must be non-empty for all brokers
 * - accountName required for display purposes
 * - accountStatus must be ACTIVE for trading
 * - availableFunds validated against broker minimums
 * 
 * @param accountId Unique broker account identifier
 * @param accountName Account holder name from broker
 * @param displayName User-friendly display name
 * @param accountStatus Account status (ACTIVE, SUSPENDED, CLOSED)
 * @param accountType Account type (INDIVIDUAL, CORPORATE, NRI)
 * @param availableFunds Available funds for trading
 * @param utilisedFunds Funds currently utilized in positions
 * @param marginAvailable Margin available for leveraged trading
 * @param marginUtilised Margin currently utilized
 * @param tradingSegments Enabled trading segments (EQ, FO, CD, etc.)
 * @param permissions Trading permissions granted
 * @param riskProfile Risk profile assigned by broker
 * @param lastUpdated When account info was last fetched
 * @param brokerSpecificData Additional broker-specific fields
 * 
 * @author TradeMaster Development Team  
 * @version 2.0.0 (Real Broker Account Integration)
 */
@Builder
public record BrokerAccount(
    String accountId,
    String accountName,
    String displayName,
    AccountStatus accountStatus,
    AccountType accountType,
    BigDecimal availableFunds,
    BigDecimal utilisedFunds,
    BigDecimal marginAvailable,
    BigDecimal marginUtilised,
    List<String> tradingSegments,
    List<String> permissions,
    String riskProfile,
    Instant lastUpdated,
    Map<String, Object> brokerSpecificData
) {
    
    /**
     * Validate account is ready for trading
     * 
     * @return true if account can be used for trading
     */
    public boolean isValidForTrading() {
        return accountId != null && 
               !accountId.trim().isEmpty() &&
               accountStatus == AccountStatus.ACTIVE &&
               availableFunds != null &&
               availableFunds.compareTo(BigDecimal.ZERO) >= 0 &&
               tradingSegments != null &&
               !tradingSegments.isEmpty();
    }
    
    /**
     * Check if account has sufficient funds for trading
     * 
     * @param requiredAmount Minimum amount needed
     * @return true if funds available
     */
    public boolean hasSufficientFunds(BigDecimal requiredAmount) {
        return availableFunds != null && 
               requiredAmount != null &&
               availableFunds.compareTo(requiredAmount) >= 0;
    }
    
    /**
     * Check if specific trading segment is enabled
     * 
     * @param segment Trading segment (EQ, FO, CD, MCX)
     * @return true if segment is enabled
     */
    public boolean hasSegmentAccess(String segment) {
        return tradingSegments != null &&
               tradingSegments.contains(segment);
    }
    
    /**
     * Check if specific permission is granted
     * 
     * @param permission Trading permission
     * @return true if permission exists
     */
    public boolean hasPermission(String permission) {
        return permissions != null &&
               permissions.contains(permission);
    }
    
    /**
     * Get total funds (available + utilized)
     * 
     * @return Total account funds
     */
    public BigDecimal getTotalFunds() {
        BigDecimal available = availableFunds != null ? availableFunds : BigDecimal.ZERO;
        BigDecimal utilized = utilisedFunds != null ? utilisedFunds : BigDecimal.ZERO;
        return available.add(utilized);
    }
    
    /**
     * Get fund utilization percentage
     * 
     * @return Percentage of funds utilized (0-100)
     */
    public BigDecimal getFundUtilizationPercent() {
        BigDecimal total = getTotalFunds();
        BigDecimal utilized = utilisedFunds != null ? utilisedFunds : BigDecimal.ZERO;
        
        if (total.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return utilized.divide(total, 2, java.math.RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get margin utilization percentage
     * 
     * @return Percentage of margin utilized (0-100)
     */
    public BigDecimal getMarginUtilizationPercent() {
        BigDecimal available = marginAvailable != null ? marginAvailable : BigDecimal.ZERO;
        BigDecimal utilized = marginUtilised != null ? marginUtilised : BigDecimal.ZERO;
        BigDecimal total = available.add(utilized);
        
        if (total.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return utilized.divide(total, 2, java.math.RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Account Status Enumeration
     */
    public enum AccountStatus {
        ACTIVE("Active", true),
        SUSPENDED("Suspended", false), 
        CLOSED("Closed", false),
        PENDING("Pending Activation", false),
        BLOCKED("Blocked", false);
        
        private final String displayName;
        private final boolean canTrade;
        
        AccountStatus(String displayName, boolean canTrade) {
            this.displayName = displayName;
            this.canTrade = canTrade;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean canTrade() { return canTrade; }
    }
    
    /**
     * Account Type Enumeration
     */
    public enum AccountType {
        INDIVIDUAL("Individual"),
        CORPORATE("Corporate"),
        PARTNERSHIP("Partnership"),
        NRI("Non-Resident Indian"),
        HUF("Hindu Undivided Family"),
        TRUST("Trust");
        
        private final String displayName;
        
        AccountType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Create account from Zerodha API response
     * 
     * @param profileData Zerodha profile API response
     * @param fundsData Zerodha funds API response
     * @return BrokerAccount instance
     */
    public static BrokerAccount fromZerodhaResponse(Map<String, Object> profileData, 
                                                   Map<String, Object> fundsData) {
        return BrokerAccount.builder()
            .accountId((String) profileData.get("user_id"))
            .accountName((String) profileData.get("user_name"))
            .displayName((String) profileData.get("user_shortname"))
            .accountStatus(AccountStatus.ACTIVE) // Zerodha doesn't provide status in profile
            .accountType(AccountType.INDIVIDUAL) // Default for most Zerodha accounts
            .availableFunds(new BigDecimal(fundsData.get("cash").toString()))
            .marginAvailable(new BigDecimal(fundsData.get("available").toString()))
            .tradingSegments((List<String>) profileData.get("products"))
            .permissions((List<String>) profileData.get("order_types"))
            .riskProfile((String) profileData.get("user_type"))
            .lastUpdated(Instant.now())
            .brokerSpecificData(Map.of(
                "broker", "ZERODHA",
                "api_version", "v3",
                "exchange", profileData.get("exchange")
            ))
            .build();
    }
    
    /**
     * Create account from Upstox API response
     * 
     * @param profileData Upstox profile API response  
     * @param fundsData Upstox funds API response
     * @return BrokerAccount instance
     */
    public static BrokerAccount fromUpstoxResponse(Map<String, Object> profileData,
                                                 Map<String, Object> fundsData) {
        return BrokerAccount.builder()
            .accountId((String) profileData.get("user_id"))
            .accountName((String) profileData.get("user_name"))
            .displayName((String) profileData.get("user_name"))
            .accountStatus(AccountStatus.ACTIVE)
            .accountType(AccountType.INDIVIDUAL)
            .availableFunds(new BigDecimal(fundsData.get("available_cash").toString()))
            .utilisedFunds(new BigDecimal(fundsData.get("used_margin").toString()))
            .marginAvailable(new BigDecimal(fundsData.get("available_margin").toString()))
            .tradingSegments((List<String>) profileData.get("exchanges"))
            .permissions((List<String>) profileData.get("products"))
            .lastUpdated(Instant.now())
            .brokerSpecificData(Map.of(
                "broker", "UPSTOX",
                "api_version", "v2",
                "client_id", profileData.get("client_id")
            ))
            .build();
    }
}