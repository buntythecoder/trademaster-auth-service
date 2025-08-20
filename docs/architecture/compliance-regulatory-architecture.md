# Compliance & Regulatory Architecture

## SEBI Compliance Engine

**Regulatory Rule Engine:**
```java
@Service
public class ComplianceEngine {
    
    public ComplianceResult validateTrade(Trade trade, User user) {
        List<Rule> applicableRules = ruleRepository
            .findByAssetType(trade.getAssetType());
        
        for (Rule rule : applicableRules) {
            ValidationResult result = rule.validate(trade, user);
            if (!result.isValid()) {
                return ComplianceResult.violation(rule, result.getMessage());
            }
        }
        
        return ComplianceResult.compliant();
    }
}
```

**Automated Reporting:**
- Daily trading volume reports
- Position limit monitoring
- Suspicious activity detection
- Audit trail generation for regulatory reviews

## Data Governance

**Data Classification:**
```java
public enum DataClassification {
    PUBLIC,           // Market data, public information
    INTERNAL,         // Internal analytics, aggregated data
    CONFIDENTIAL,     // User trading data, behavioral patterns
    RESTRICTED        // PII, financial information, auth data
}
```

**Data Retention Policies:**
- Trade data: 7 years (regulatory requirement)
- User behavior: 2 years (anonymized after 1 year)
- System logs: 1 year
- Audit trails: 10 years (immutable storage)
