package com.trademaster.brokerauth.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Data Classification Service
 * 
 * Comprehensive data classification and sensitivity analysis for:
 * - PCI-DSS: Cardholder Data Environment (CDE) classification
 * - GDPR: Personal Data and Special Categories identification
 * - CCPA: Personal Information and Sensitive PI classification
 * - SOC2: Data confidentiality and integrity requirements
 * 
 * Features:
 * - Automated data type detection and classification
 * - Multi-framework compliance mapping
 * - Sensitive data pattern recognition
 * - Data retention policy assignment
 * - Cross-border transfer restrictions
 * - Data minimization recommendations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataClassificationService {

    private final boolean pciDssEnabled;
    private final boolean gdprEnabled;
    private final boolean ccpaEnabled;
    
    // Classification Levels
    public enum ClassificationLevel {
        PUBLIC("PUBLIC", 0, "No restrictions"),
        INTERNAL("INTERNAL", 1, "Internal use only"),
        CONFIDENTIAL("CONFIDENTIAL", 2, "Restricted access required"),
        RESTRICTED("RESTRICTED", 3, "Highly restricted, requires approval"),
        TOP_SECRET("TOP_SECRET", 4, "Maximum security controls required");
        
        private final String label;
        private final int level;
        private final String description;
        
        ClassificationLevel(String label, int level, String description) {
            this.label = label;
            this.level = level;
            this.description = description;
        }
        
        public String getLabel() { return label; }
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
    
    // Data Categories for Compliance
    public enum DataCategory {
        // PCI-DSS Categories
        CARDHOLDER_DATA("CARDHOLDER_DATA", "PCI-DSS", "Credit card numbers, expiry dates"),
        SENSITIVE_AUTHENTICATION_DATA("SENSITIVE_AUTH_DATA", "PCI-DSS", "CVV, PIN, magnetic stripe data"),
        
        // GDPR Categories
        PERSONAL_DATA("PERSONAL_DATA", "GDPR", "Any data relating to identified person"),
        SPECIAL_CATEGORY_DATA("SPECIAL_CATEGORY_DATA", "GDPR", "Racial, ethnic, political, religious data"),
        BIOMETRIC_DATA("BIOMETRIC_DATA", "GDPR", "Fingerprints, facial recognition data"),
        GENETIC_DATA("GENETIC_DATA", "GDPR", "Inherited or acquired genetic characteristics"),
        HEALTH_DATA("HEALTH_DATA", "GDPR", "Physical or mental health information"),
        
        // CCPA Categories
        PERSONAL_INFORMATION("PERSONAL_INFORMATION", "CCPA", "Information identifying consumer"),
        SENSITIVE_PERSONAL_INFORMATION("SENSITIVE_PI", "CCPA", "SSN, financial account, geolocation"),
        
        // Financial and Trading Data
        FINANCIAL_DATA("FINANCIAL_DATA", "SOC2", "Bank accounts, trading positions"),
        TRADING_DATA("TRADING_DATA", "SOC2", "Orders, positions, portfolio data"),
        AUTHENTICATION_DATA("AUTHENTICATION_DATA", "SOC2", "Passwords, tokens, API keys"),
        
        // General Categories
        SYSTEM_DATA("SYSTEM_DATA", "SOC2", "System logs, configuration data"),
        PUBLIC_DATA("PUBLIC_DATA", "NONE", "Publicly available information");
        
        private final String code;
        private final String framework;
        private final String description;
        
        DataCategory(String code, String framework, String description) {
            this.code = code;
            this.framework = framework;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getFramework() { return framework; }
        public String getDescription() { return description; }
    }
    
    // Data Detection Patterns
    private final Map<DataCategory, List<Pattern>> detectionPatterns = new HashMap<>();
    private final Map<DataCategory, List<String>> keywordPatterns = new HashMap<>();
    private final Map<String, ClassificationLevel> fieldClassifications = new HashMap<>();
    private final Map<DataCategory, String> retentionPolicies = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        log.info("🔍 Initializing Data Classification Service...");
        
        initializeDetectionPatterns();
        initializeKeywordPatterns();
        initializeFieldClassifications();
        initializeRetentionPolicies();
        
        log.info("✅ Data Classification Service initialized");
        log.info("📊 Active Frameworks: PCI-DSS={}, GDPR={}, CCPA={}", 
                pciDssEnabled, gdprEnabled, ccpaEnabled);
    }
    
    /**
     * Classify data field based on content and context
     */
    public DataClassification classifyField(String fieldName, String fieldValue, Map<String, Object> context) {
        log.debug("Classifying field: {} with value length: {}", fieldName, 
                fieldValue != null ? fieldValue.length() : 0);
        
        DataClassification classification = DataClassification.builder()
            .fieldName(fieldName)
            .fieldValue(maskSensitiveData(fieldValue))
            .timestamp(java.time.ZonedDateTime.now())
            .build();
        
        // Step 1: Pattern-based detection
        Set<DataCategory> detectedCategories = detectDataCategories(fieldName, fieldValue);
        
        // Step 2: Context-based enhancement
        detectedCategories = enhanceWithContext(detectedCategories, context);
        
        // Step 3: Determine classification level
        ClassificationLevel level = determineClassificationLevel(detectedCategories);
        
        // Step 4: Apply compliance requirements
        Set<String> applicableFrameworks = getApplicableFrameworks(detectedCategories);
        
        // Step 5: Determine handling requirements
        DataHandlingRequirements requirements = determineHandlingRequirements(detectedCategories, level);
        
        classification.setCategories(detectedCategories);
        classification.setClassificationLevel(level);
        classification.setApplicableFrameworks(applicableFrameworks);
        classification.setHandlingRequirements(requirements);
        classification.setRetentionPeriod(determineRetentionPeriod(detectedCategories));
        classification.setEncryptionRequired(requiresEncryption(level, detectedCategories));
        classification.setAccessControlRequired(requiresAccessControl(level, detectedCategories));
        classification.setCrossBorderRestrictions(getCrossBorderRestrictions(detectedCategories));
        
        log.debug("Classification completed: field={}, level={}, categories={}, frameworks={}", 
                fieldName, level, detectedCategories.size(), applicableFrameworks.size());
        
        return classification;
    }
    
    /**
     * Classify data object with multiple fields
     */
    public ObjectClassification classifyObject(String objectName, Map<String, Object> data, Map<String, Object> metadata) {
        log.debug("Classifying object: {} with {} fields", objectName, data.size());
        
        ObjectClassification objectClassification = ObjectClassification.builder()
            .objectName(objectName)
            .timestamp(java.time.ZonedDateTime.now())
            .fieldClassifications(new ArrayList<>())
            .build();
        
        // Classify each field
        Set<DataCategory> allCategories = new HashSet<>();
        ClassificationLevel highestLevel = ClassificationLevel.PUBLIC;
        Set<String> allFrameworks = new HashSet<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldValue = entry.getValue() != null ? entry.getValue().toString() : null;
            DataClassification fieldClassification = classifyField(entry.getKey(), fieldValue, metadata);
            
            objectClassification.getFieldClassifications().add(fieldClassification);
            allCategories.addAll(fieldClassification.getCategories());
            allFrameworks.addAll(fieldClassification.getApplicableFrameworks());
            
            if (fieldClassification.getClassificationLevel().getLevel() > highestLevel.getLevel()) {
                highestLevel = fieldClassification.getClassificationLevel();
            }
        }
        
        // Set object-level classification
        objectClassification.setOverallClassificationLevel(highestLevel);
        objectClassification.setDetectedCategories(allCategories);
        objectClassification.setApplicableFrameworks(allFrameworks);
        objectClassification.setRequiresSpecialHandling(highestLevel.getLevel() >= ClassificationLevel.CONFIDENTIAL.getLevel());
        objectClassification.setComplianceRisk(calculateComplianceRisk(allCategories, highestLevel));
        
        log.info("Object classification completed: object={}, level={}, categories={}, risk={}", 
                objectName, highestLevel, allCategories.size(), objectClassification.getComplianceRisk());
        
        return objectClassification;
    }
    
    /**
     * Get data minimization recommendations
     */
    public DataMinimizationRecommendation getDataMinimizationRecommendation(ObjectClassification classification) {
        DataMinimizationRecommendation recommendation = DataMinimizationRecommendation.builder()
            .objectName(classification.getObjectName())
            .timestamp(java.time.ZonedDateTime.now())
            .recommendations(new ArrayList<>())
            .build();
        
        for (DataClassification field : classification.getFieldClassifications()) {
            if (field.getClassificationLevel().getLevel() >= ClassificationLevel.CONFIDENTIAL.getLevel()) {
                // Check if field is necessary
                String necessity = assessFieldNecessity(field);
                if ("UNNECESSARY".equals(necessity)) {
                    recommendation.getRecommendations().add(
                        "Remove field '" + field.getFieldName() + "' - not required for business purpose"
                    );
                } else if ("REDUCIBLE".equals(necessity)) {
                    recommendation.getRecommendations().add(
                        "Reduce precision of field '" + field.getFieldName() + "' to minimum required"
                    );
                }
            }
        }
        
        // Suggest anonymization for high-risk data
        if (classification.getComplianceRisk().ordinal() >= ComplianceRisk.HIGH.ordinal()) {
            recommendation.getRecommendations().add(
                "Consider anonymization or pseudonymization for high-risk data categories"
            );
        }
        
        return recommendation;
    }
    
    /**
     * Validate cross-border data transfer
     */
    public CrossBorderTransferValidation validateCrossBorderTransfer(
            ObjectClassification classification, String sourceCountry, String targetCountry) {
        
        CrossBorderTransferValidation validation = CrossBorderTransferValidation.builder()
            .sourceCountry(sourceCountry)
            .targetCountry(targetCountry)
            .timestamp(java.time.ZonedDateTime.now())
            .allowed(true)
            .restrictions(new ArrayList<>())
            .requirements(new ArrayList<>())
            .build();
        
        // Check GDPR restrictions
        if (gdprEnabled && isEuCountry(sourceCountry) && !hasAdequacyDecision(targetCountry)) {
            validation.setAllowed(false);
            validation.getRestrictions().add("GDPR: No adequacy decision for target country");
            validation.getRequirements().add("Standard Contractual Clauses or Binding Corporate Rules required");
        }
        
        // Check data categories for specific restrictions
        for (DataCategory category : classification.getDetectedCategories()) {
            if (category == DataCategory.SPECIAL_CATEGORY_DATA) {
                validation.getRequirements().add("Additional safeguards required for special category data");
            }
            if (category == DataCategory.CARDHOLDER_DATA) {
                validation.getRequirements().add("PCI-DSS Level 1 service provider required in target country");
            }
        }
        
        return validation;
    }
    
    // Private helper methods
    private void initializeDetectionPatterns() {
        // Credit Card Patterns
        detectionPatterns.put(DataCategory.CARDHOLDER_DATA, Arrays.asList(
            Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3[0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b"),
            Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
            Pattern.compile("\\b\\d{4}[\\s-]?\\d{6}[\\s-]?\\d{5}\\b") // AMEX
        ));
        
        // SSN Pattern
        detectionPatterns.put(DataCategory.SENSITIVE_PERSONAL_INFORMATION, Arrays.asList(
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            Pattern.compile("\\b\\d{9}\\b")
        ));
        
        // Email Pattern
        detectionPatterns.put(DataCategory.PERSONAL_DATA, Arrays.asList(
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
        ));
        
        // Phone Pattern
        detectionPatterns.put(DataCategory.PERSONAL_INFORMATION, Arrays.asList(
            Pattern.compile("\\b\\+?1?[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
            Pattern.compile("\\b\\d{10}\\b")
        ));
    }
    
    private void initializeKeywordPatterns() {
        keywordPatterns.put(DataCategory.CARDHOLDER_DATA, 
            Arrays.asList("card", "credit", "debit", "visa", "mastercard", "amex", "pan", "primary_account_number"));
        
        keywordPatterns.put(DataCategory.SENSITIVE_AUTHENTICATION_DATA,
            Arrays.asList("cvv", "cvc", "pin", "security_code", "card_verification", "magnetic_stripe"));
        
        keywordPatterns.put(DataCategory.PERSONAL_DATA,
            Arrays.asList("email", "phone", "address", "name", "dob", "birth", "personal", "contact"));
        
        keywordPatterns.put(DataCategory.FINANCIAL_DATA,
            Arrays.asList("account", "balance", "transaction", "payment", "bank", "routing", "iban"));
        
        keywordPatterns.put(DataCategory.TRADING_DATA,
            Arrays.asList("position", "order", "trade", "portfolio", "symbol", "quantity", "price", "broker"));
        
        keywordPatterns.put(DataCategory.AUTHENTICATION_DATA,
            Arrays.asList("password", "token", "key", "secret", "credential", "auth", "session", "cookie"));
        
        keywordPatterns.put(DataCategory.BIOMETRIC_DATA,
            Arrays.asList("fingerprint", "facial", "retina", "biometric", "face_id", "touch_id"));
        
        keywordPatterns.put(DataCategory.HEALTH_DATA,
            Arrays.asList("health", "medical", "diagnosis", "treatment", "prescription", "doctor"));
    }
    
    private void initializeFieldClassifications() {
        // Pre-defined field classifications
        fieldClassifications.put("password", ClassificationLevel.RESTRICTED);
        fieldClassifications.put("ssn", ClassificationLevel.RESTRICTED);
        fieldClassifications.put("credit_card", ClassificationLevel.RESTRICTED);
        fieldClassifications.put("api_key", ClassificationLevel.CONFIDENTIAL);
        fieldClassifications.put("email", ClassificationLevel.CONFIDENTIAL);
        fieldClassifications.put("phone", ClassificationLevel.CONFIDENTIAL);
        fieldClassifications.put("address", ClassificationLevel.CONFIDENTIAL);
        fieldClassifications.put("name", ClassificationLevel.CONFIDENTIAL);
        fieldClassifications.put("user_id", ClassificationLevel.INTERNAL);
        fieldClassifications.put("session_id", ClassificationLevel.INTERNAL);
        fieldClassifications.put("public_key", ClassificationLevel.PUBLIC);
    }
    
    private void initializeRetentionPolicies() {
        retentionPolicies.put(DataCategory.CARDHOLDER_DATA, "1_YEAR");
        retentionPolicies.put(DataCategory.SENSITIVE_AUTHENTICATION_DATA, "IMMEDIATE_DELETE");
        retentionPolicies.put(DataCategory.PERSONAL_DATA, "3_YEARS");
        retentionPolicies.put(DataCategory.FINANCIAL_DATA, "7_YEARS");
        retentionPolicies.put(DataCategory.TRADING_DATA, "7_YEARS");
        retentionPolicies.put(DataCategory.AUTHENTICATION_DATA, "90_DAYS");
        retentionPolicies.put(DataCategory.SYSTEM_DATA, "1_YEAR");
    }
    
    private Set<DataCategory> detectDataCategories(String fieldName, String fieldValue) {
        Set<DataCategory> categories = new HashSet<>();
        
        String lowerFieldName = fieldName.toLowerCase();
        String lowerFieldValue = fieldValue != null ? fieldValue.toLowerCase() : "";
        
        // Keyword-based detection
        for (Map.Entry<DataCategory, List<String>> entry : keywordPatterns.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerFieldName.contains(keyword) || lowerFieldValue.contains(keyword)) {
                    categories.add(entry.getKey());
                    break;
                }
            }
        }
        
        // Pattern-based detection
        if (fieldValue != null) {
            for (Map.Entry<DataCategory, List<Pattern>> entry : detectionPatterns.entrySet()) {
                for (Pattern pattern : entry.getValue()) {
                    if (pattern.matcher(fieldValue).find()) {
                        categories.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        
        return categories;
    }
    
    private Set<DataCategory> enhanceWithContext(Set<DataCategory> categories, Map<String, Object> context) {
        if (context == null) return categories;
        
        // Business context enhancement
        String businessContext = (String) context.get("business_context");
        if ("trading".equals(businessContext)) {
            if (categories.contains(DataCategory.FINANCIAL_DATA)) {
                categories.add(DataCategory.TRADING_DATA);
            }
        }
        
        // User type context
        String userType = (String) context.get("user_type");
        if ("broker".equals(userType) && categories.contains(DataCategory.PERSONAL_DATA)) {
            categories.add(DataCategory.FINANCIAL_DATA);
        }
        
        return categories;
    }
    
    private ClassificationLevel determineClassificationLevel(Set<DataCategory> categories) {
        if (categories.isEmpty()) {
            return ClassificationLevel.PUBLIC;
        }
        
        // Highest level category determines classification
        if (categories.contains(DataCategory.SENSITIVE_AUTHENTICATION_DATA) ||
            categories.contains(DataCategory.CARDHOLDER_DATA)) {
            return ClassificationLevel.RESTRICTED;
        }
        
        if (categories.contains(DataCategory.PERSONAL_DATA) ||
            categories.contains(DataCategory.FINANCIAL_DATA) ||
            categories.contains(DataCategory.BIOMETRIC_DATA)) {
            return ClassificationLevel.CONFIDENTIAL;
        }
        
        if (categories.contains(DataCategory.SYSTEM_DATA) ||
            categories.contains(DataCategory.AUTHENTICATION_DATA)) {
            return ClassificationLevel.INTERNAL;
        }
        
        return ClassificationLevel.PUBLIC;
    }
    
    private Set<String> getApplicableFrameworks(Set<DataCategory> categories) {
        Set<String> frameworks = new HashSet<>();
        
        for (DataCategory category : categories) {
            String framework = category.getFramework();
            if (!"NONE".equals(framework)) {
                frameworks.add(framework);
            }
        }
        
        return frameworks;
    }
    
    private DataHandlingRequirements determineHandlingRequirements(Set<DataCategory> categories, ClassificationLevel level) {
        return DataHandlingRequirements.builder()
            .encryptionRequired(level.getLevel() >= ClassificationLevel.CONFIDENTIAL.getLevel())
            .accessLoggingRequired(level.getLevel() >= ClassificationLevel.INTERNAL.getLevel())
            .approvalRequired(level.getLevel() >= ClassificationLevel.RESTRICTED.getLevel())
            .auditTrailRequired(true)
            .dataMinimizationRequired(categories.stream().anyMatch(c -> 
                c == DataCategory.PERSONAL_DATA || c == DataCategory.CARDHOLDER_DATA))
            .consentRequired(gdprEnabled && categories.contains(DataCategory.PERSONAL_DATA))
            .build();
    }
    
    private String determineRetentionPeriod(Set<DataCategory> categories) {
        if (categories.isEmpty()) return "INDEFINITE";
        
        // Return the shortest retention period for compliance
        return categories.stream()
            .map(category -> retentionPolicies.getOrDefault(category, "7_YEARS"))
            .min(Comparator.comparing(this::getRetentionDays))
            .orElse("7_YEARS");
    }
    
    private int getRetentionDays(String period) {
        switch (period) {
            case "IMMEDIATE_DELETE": return 0;
            case "90_DAYS": return 90;
            case "1_YEAR": return 365;
            case "3_YEARS": return 1095;
            case "7_YEARS": return 2555;
            default: return Integer.MAX_VALUE;
        }
    }
    
    private boolean requiresEncryption(ClassificationLevel level, Set<DataCategory> categories) {
        return level.getLevel() >= ClassificationLevel.CONFIDENTIAL.getLevel() ||
               categories.contains(DataCategory.CARDHOLDER_DATA) ||
               categories.contains(DataCategory.SENSITIVE_AUTHENTICATION_DATA);
    }
    
    private boolean requiresAccessControl(ClassificationLevel level, Set<DataCategory> categories) {
        return level.getLevel() >= ClassificationLevel.INTERNAL.getLevel();
    }
    
    private Map<String, String> getCrossBorderRestrictions(Set<DataCategory> categories) {
        Map<String, String> restrictions = new HashMap<>();
        
        if (categories.contains(DataCategory.PERSONAL_DATA)) {
            restrictions.put("GDPR", "Adequacy decision or appropriate safeguards required");
        }
        
        if (categories.contains(DataCategory.CARDHOLDER_DATA)) {
            restrictions.put("PCI_DSS", "Level 1 service provider required");
        }
        
        if (categories.contains(DataCategory.FINANCIAL_DATA)) {
            restrictions.put("FINANCIAL", "Financial services license required in target jurisdiction");
        }
        
        return restrictions;
    }
    
    private ComplianceRisk calculateComplianceRisk(Set<DataCategory> categories, ClassificationLevel level) {
        int riskScore = level.getLevel() * 2;
        
        // Add category-specific risk
        if (categories.contains(DataCategory.CARDHOLDER_DATA)) riskScore += 3;
        if (categories.contains(DataCategory.SENSITIVE_AUTHENTICATION_DATA)) riskScore += 4;
        if (categories.contains(DataCategory.SPECIAL_CATEGORY_DATA)) riskScore += 3;
        if (categories.contains(DataCategory.BIOMETRIC_DATA)) riskScore += 3;
        
        if (riskScore >= 10) return ComplianceRisk.CRITICAL;
        if (riskScore >= 7) return ComplianceRisk.HIGH;
        if (riskScore >= 4) return ComplianceRisk.MEDIUM;
        return ComplianceRisk.LOW;
    }
    
    private String assessFieldNecessity(DataClassification field) {
        // Business logic to assess if field is necessary
        // This would typically integrate with business requirements
        return "NECESSARY"; // Simplified for example
    }
    
    private String maskSensitiveData(String value) {
        if (value == null || value.length() <= 4) return "*****";
        return value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2);
    }
    
    private boolean isEuCountry(String country) {
        Set<String> euCountries = Set.of("DE", "FR", "IT", "ES", "NL", "BE", "PL", "RO", "EL", "CZ", 
            "PT", "HU", "SE", "AT", "BG", "DK", "FI", "SK", "IE", "HR", "LT", "SI", "LV", "EE", 
            "CY", "LU", "MT");
        return euCountries.contains(country);
    }
    
    private boolean hasAdequacyDecision(String country) {
        Set<String> adequateCountries = Set.of("US", "CA", "CH", "IL", "JE", "GG", "IM", "FO", "AD", "AR", "UY", "NZ", "JP", "KR");
        return adequateCountries.contains(country);
    }
    
    // Enums and Data Classes
    public enum ComplianceRisk {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DataClassification {
        private String fieldName;
        private String fieldValue;
        private java.time.ZonedDateTime timestamp;
        private Set<DataCategory> categories;
        private ClassificationLevel classificationLevel;
        private Set<String> applicableFrameworks;
        private DataHandlingRequirements handlingRequirements;
        private String retentionPeriod;
        private boolean encryptionRequired;
        private boolean accessControlRequired;
        private Map<String, String> crossBorderRestrictions;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ObjectClassification {
        private String objectName;
        private java.time.ZonedDateTime timestamp;
        private List<DataClassification> fieldClassifications;
        private ClassificationLevel overallClassificationLevel;
        private Set<DataCategory> detectedCategories;
        private Set<String> applicableFrameworks;
        private boolean requiresSpecialHandling;
        private ComplianceRisk complianceRisk;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DataHandlingRequirements {
        private boolean encryptionRequired;
        private boolean accessLoggingRequired;
        private boolean approvalRequired;
        private boolean auditTrailRequired;
        private boolean dataMinimizationRequired;
        private boolean consentRequired;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DataMinimizationRecommendation {
        private String objectName;
        private java.time.ZonedDateTime timestamp;
        private List<String> recommendations;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CrossBorderTransferValidation {
        private String sourceCountry;
        private String targetCountry;
        private java.time.ZonedDateTime timestamp;
        private boolean allowed;
        private List<String> restrictions;
        private List<String> requirements;
    }
}