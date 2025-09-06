package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PnL Validation Result for data integrity and accuracy verification
 * 
 * @param portfolioId Portfolio identifier
 * @param validationDate Date of validation
 * @param validationStatus Overall validation status
 * @param totalPnLCalculated Calculated total PnL
 * @param totalPnLReconciled Reconciled total PnL from external sources
 * @param discrepancy Discrepancy between calculated and reconciled
 * @param discrepancyPercent Discrepancy as percentage
 * @param validationRules Validation rules applied
 * @param failedRules Rules that failed validation
 * @param positionValidations Individual position validations
 * @param dataQualityScore Data quality score (0-100)
 * @param recommendedActions Recommended corrective actions
 * @param validatedBy Validation performed by (system/user)
 */
public record PnLValidationResult(
    Long portfolioId,
    Instant validationDate,
    ValidationStatus validationStatus,
    BigDecimal totalPnLCalculated,
    BigDecimal totalPnLReconciled,
    BigDecimal discrepancy,
    BigDecimal discrepancyPercent,
    List<ValidationRule> validationRules,
    List<ValidationRule> failedRules,
    List<PositionValidation> positionValidations,
    Integer dataQualityScore,
    List<String> recommendedActions,
    String validatedBy
) {
    
    public enum ValidationStatus {
        PASSED, FAILED, WARNING, PENDING
    }
    
    public record ValidationRule(
        String ruleName,
        String ruleDescription,
        ValidationStatus status,
        String errorMessage,
        BigDecimal threshold,
        BigDecimal actualValue
    ) {}
    
    public record PositionValidation(
        String symbol,
        ValidationStatus status,
        BigDecimal calculatedPnL,
        BigDecimal reconciledPnL,
        BigDecimal discrepancy,
        List<String> issues
    ) {}
}