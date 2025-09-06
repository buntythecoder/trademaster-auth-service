# Portfolio Service Compilation Fix Plan

## 1. Circuit Breaker RetryConfig API Fix (CRITICAL)
- Fix `exponentialBackoffMultiplier(2.0)` method call
- Use proper Resilience4j RetryConfig builder API

## 2. Controller UUID vs Long Type Issues (CRITICAL)
- PortfolioController: All methods expect Long but receive UUID
- Need to align controller parameter types with service interface
- Fix createPortfolio missing userId parameter

## 3. Service Interface vs Implementation Mismatches (CRITICAL)
- Missing methods in PortfolioService interface
- Add getPortfoliosForUser method
- Add initiateRebalancing method
- Fix deletePortfolio signature mismatch

## 4. Missing Service Methods in Analytics/Risk Services (CRITICAL)
- PortfolioAnalyticsService missing methods
- PortfolioRiskService missing methods
- PnLCalculationService return type mismatches

## 5. Missing Repository Query Methods (HIGH)
- Repository methods being called that don't exist
- Add missing finder methods

## 6. Missing Metrics Fields (HIGH)
- portfolioMetrics variable references missing
- Need to inject metrics service properly

## 7. Domain vs Service Class Conflicts (MEDIUM)
- Same class names in different packages causing import conflicts
- Move domain classes or rename to avoid conflicts

## 8. TaxLotInfo boolean comparison fix (MEDIUM)
- Fix boolean comparison with null issue

## 9. Missing DTO Methods (MEDIUM)
- CreatePortfolioRequest missing getName() method
- Add missing properties to DTOs