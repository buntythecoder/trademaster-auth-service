# COMPLETE TRADING SERVICE SCHEMA AUDIT

## Entity vs Database Analysis

### EXISTING TABLES IN DATABASE:
- flyway_schema_history (system)
- order_fills ✅
- orders ✅ 
- portfolio_history ✅
- portfolios ✅
- positions ✅ (fixed)
- risk_limits ✅ (partially fixed)
- trades ✅
- trading_audit_log ✅

### ENTITY ANALYSIS:

1. **Order.java** → `orders` table ✅ EXISTS
2. **OrderFill.java** → `order_fills` table ✅ EXISTS  
3. **PortfolioHistory.java** → `portfolio_history` table ✅ EXISTS
4. **Position.java** → `positions` table ✅ EXISTS (FIXED)
5. **RiskLimit.java** → `risk_limits` table ✅ EXISTS (NEEDS COLUMN AUDIT)
6. **SimpleOrder.java** → `simple_orders` table ❌ MISSING
7. **SimplifiedPosition.java** → `portfolios` table ✅ EXISTS (NEEDS AUDIT)
8. **SimplifiedRiskLimit.java** → `risk_limits` table ✅ EXISTS (NEEDS AUDIT)
9. **Trade.java** → `trades` table ✅ EXISTS (NEEDS AUDIT)
10. **TradingAuditLog.java** → `trading_audit_log` table ✅ EXISTS (NEEDS AUDIT)

## CRITICAL MISSING:
1. `simple_orders` table - MUST CREATE

## NEXT STEPS:
1. Create simple_orders table
2. Audit columns in each existing table
3. Add missing columns, indexes, constraints
4. Create comprehensive migration script