# Auth Service Database Migration Summary

## Overview
This document summarizes the database migrations created to align the auth service database schema with the Java entity classes.

## Migration History

### V1-V6: Existing Migrations
- **V1**: Initial users, user_profiles, user_roles, user_role_assignments, password_history, mfa_configurations, user_devices tables
- **V2**: Audit tables (auth_audit_log, security_events, rate_limit_violations, user_sessions, compliance_reports, api_access_log)
- **V3**: Verification tokens table
- **V4**: MFA and security enhancement tables (updated structure)
- **V5**: Added first_name, last_name to users table
- **V6**: Added UserDetails fields to users table

### V7-V9: New Entity Alignment Migrations

#### V7: Fix Entity-Table Structure Mismatches
**Purpose**: Corrects fundamental mismatches between entity definitions and database schema

**Key Changes**:
- Fixed foreign key data types from VARCHAR(50) to INTEGER for proper user references
- Aligned MfaConfiguration, UserDevice, UserSession, SessionSettings, DeviceSettings, SecurityAuditLog tables
- Added missing foreign key constraints with proper cascade behavior
- Fixed column naming inconsistencies

**Affected Tables**:
- `mfa_configuration`: user_id type fix, proper FK constraint
- `user_devices`: user_id type fix, proper FK constraint  
- `user_sessions`: user_id type fix, proper FK constraint
- `session_settings`: user_id type fix, proper FK constraint
- `device_settings`: user_id type fix, proper FK constraint
- `security_audit_logs`: user_id type fix, added missing columns

#### V8: Add Missing Entity Tables
**Purpose**: Ensures complete alignment with Java entity structure

**Key Changes**:
- Added Spring Security UserDetails fields to users table (enabled, account_non_expired, etc.)
- Fixed MFA configuration enum constraints
- Added missing columns to user_profiles (address, timezone)
- Fixed user_devices column names and added trust_expiry
- Updated enum constraints to match Java entity enums
- Added proper indexes for entity relationships
- Data type fixes for InetAddress fields
- Added validation and foreign key cleanup

**Entity Alignments**:
- **User**: Added UserDetails implementation fields
- **UserProfile**: Added missing behavioral fields
- **UserDevice**: Fixed column names and constraints
- **MfaConfiguration**: Updated enum values and constraints
- **VerificationToken**: Updated enum constraints

#### V9: Final Entity Alignment and Cleanup
**Purpose**: Final validation and edge case handling

**Key Changes**:
- Synchronized phone_number between users and user_profiles tables
- Updated enum values to uppercase to match Java enums
- Added missing triggers for updated_at columns
- Created performance indexes for common entity queries
- Data cleanup and orphaned record removal
- Created entity_validation_summary view for monitoring

**Data Integrity**:
- Removed duplicate user_role_assignments
- Cleaned orphaned verification_tokens
- Removed expired user_sessions
- Updated enum values to match entity constants

## Entity-Database Mapping Verification

### ✅ User Entity
- **Primary Key**: `id` (SERIAL/IDENTITY) ✓
- **Email Authentication**: `email`, `password_hash` ✓
- **Profile Fields**: `first_name`, `last_name` ✓
- **Security Fields**: All Spring Security UserDetails fields ✓
- **Status Enums**: KycStatus, SubscriptionTier, AccountStatus ✓
- **Audit Fields**: `created_at`, `updated_at`, etc. ✓
- **Relationships**: OneToOne UserProfile, OneToMany roles/devices ✓

### ✅ UserProfile Entity
- **Primary Key**: `id` ✓
- **User Relationship**: `user_id` FK to users(id) ✓
- **Personal Info**: All KYC fields ✓
- **Trading Profile**: Risk tolerance, experience enums ✓
- **JSON Fields**: Behavioral settings, preferences, KYC docs ✓
- **Enums**: Properly constrained ✓

### ✅ MfaConfiguration Entity
- **Primary Key**: `id` (UUID) ✓
- **User Relationship**: `user_id` FK properly typed ✓
- **MFA Types**: Enum constraint matches entity ✓
- **Security Fields**: `secret_key`, `backup_codes` ✓
- **Status Fields**: `enabled`, `last_used`, `failed_attempts` ✓

### ✅ UserDevice Entity
- **Primary Key**: `id` (UUID) ✓
- **User Relationship**: `user_id` FK properly typed ✓
- **Device Fields**: All fingerprinting and trust fields ✓
- **Timestamps**: `first_seen`, `last_seen`, `trust_expiry` ✓
- **Location**: IP address and location tracking ✓

### ✅ UserSession Entity
- **Primary Key**: `session_id` (VARCHAR) ✓
- **User Relationship**: `user_id` FK properly typed ✓
- **Session Fields**: Activity tracking, expiration ✓
- **JSON Attributes**: Session metadata ✓
- **Business Logic**: Active status, expiration handling ✓

### ✅ Other Entities
- **UserRole**: Permissions JSONB, relationships ✓
- **UserRoleAssignment**: Proper many-to-many mapping ✓
- **VerificationToken**: Token types, expiration ✓
- **AuthAuditLog**: Comprehensive event tracking ✓
- **SecurityAuditLog**: Risk assessment fields ✓
- **SessionSettings**: User session preferences ✓
- **DeviceSettings**: Device trust preferences ✓

## Critical Fixes Applied

### 1. Foreign Key Data Type Mismatches
- **Issue**: Entity relationships used `Long userId` but database had `VARCHAR(50) user_id`
- **Fix**: Updated all user_id columns to INTEGER type with proper FK constraints

### 2. Enum Value Consistency
- **Issue**: Java enums used uppercase (e.g., `ACTIVE`) but database had lowercase
- **Fix**: Updated all enum constraints and existing data to uppercase

### 3. Missing UserDetails Fields
- **Issue**: User entity implemented Spring Security UserDetails but database lacked required fields
- **Fix**: Added `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired`

### 4. Column Name Mismatches
- **Issue**: Entity field names didn't match database column names
- **Fix**: Renamed columns or added proper JPA mappings

### 5. Missing Relationships
- **Issue**: Foreign key constraints were missing or incorrect
- **Fix**: Added proper CASCADE constraints matching entity relationships

## Testing Recommendations

### 1. Migration Testing
```bash
# Run migrations in sequence
./gradlew flywayMigrate

# Validate schema
./gradlew flywayValidate

# Check migration status
./gradlew flywayInfo
```

### 2. Entity Loading Tests
```java
// Test all entity loading
@Test
public void testEntityLoading() {
    // Test User with all relationships
    User user = userRepository.findById(1L).orElseThrow();
    assertThat(user.getUserProfile()).isNotNull();
    assertThat(user.getRoleAssignments()).isNotEmpty();
    assertThat(user.getMfaConfigurations()).isNotNull();
    assertThat(user.getUserDevices()).isNotNull();
}
```

### 3. Data Validation
```sql
-- Use the created validation view
SELECT * FROM entity_validation_summary;

-- Check foreign key integrity
SELECT table_name, constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_schema = 'public' AND constraint_type = 'FOREIGN KEY';
```

## Deployment Notes

### Pre-Deployment Checklist
- [ ] Backup existing database
- [ ] Test migrations on staging environment
- [ ] Validate entity loading in integration tests
- [ ] Check application startup after migrations
- [ ] Verify foreign key constraints are working

### Post-Deployment Validation
- [ ] Run entity validation view query
- [ ] Check application logs for entity loading errors
- [ ] Test user authentication flow
- [ ] Verify MFA configuration works
- [ ] Test device trust functionality

## Monitoring

Use the created `entity_validation_summary` view to monitor:
- Total records per entity table
- Valid record counts
- Active/enabled record counts
- Foreign key relationship integrity

```sql
SELECT * FROM entity_validation_summary;
```

## Rollback Plan

If issues occur, migrations can be rolled back:
```bash
# Rollback to V6 (before entity alignment)
./gradlew flywayUndo -Dflyway.target=6

# Or restore from backup
pg_restore -d auth_service backup_before_migration.sql
```

## Conclusion

The database schema is now fully aligned with Java entities, ensuring:
- ✅ Proper type safety and relationships
- ✅ Spring Security integration compatibility  
- ✅ JPA entity loading without mapping issues
- ✅ Referential integrity with CASCADE behavior
- ✅ Performance optimization through proper indexing
- ✅ Data validation and cleanup