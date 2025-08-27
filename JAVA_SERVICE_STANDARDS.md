# TradeMaster Java Service Standards

**Version**: 1.0.0  
**Date**: 2025-01-26  
**Status**: PRODUCTION STANDARD  

## Overview

This document defines the standardized configuration and compilation standards for all TradeMaster Java microservices. These standards ensure consistent build behavior, Java 24 compatibility, and Spring Boot 3.5.3 integration across the entire platform.

## ✅ Verified Services

All the following services have been verified to compile successfully with these standards:

- ✅ **agent-orchestration-service** - Agent OS core orchestration
- ✅ **auth-service** - Authentication and user management
- ✅ **notification-service** - Notification and messaging
- ✅ **market-data-service** - Market data ingestion and processing
- ✅ **trading-service** - Trading execution and management
- ✅ **portfolio-service** - Portfolio tracking and analysis
- ✅ **payment-service** - Payment processing and gateway integration
- ✅ **subscription-service** - Subscription and billing management
- ✅ **user-profile-service** - User profile and preferences
- ✅ **broker-auth-service** - Broker authentication and compliance

## Core Technology Stack

### Mandatory Versions
- **Java**: 24 (with Preview Features ENABLED)
- **Spring Boot**: 3.5.3
- **Gradle**: 8.14.2+
- **Lombok**: Latest compatible version

### Key Dependencies
- Spring Boot Starters (Web, Data JPA, Security, etc.)
- PostgreSQL driver
- Redis support
- Micrometer for metrics
- Structured logging (Logstash)

## 🔧 Standardized Gradle Configuration

### 1. Java Configuration
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
```

**❌ NEVER USE** (deprecated patterns):
```gradle
java {
    sourceCompatibility = '24'    // REMOVE
    targetCompatibility = '24'    // REMOVE
}
```

### 2. Preview Features Configuration
```gradle
// MANDATORY: Java 24 Preview Features per TradeMaster Standards
tasks.named('compileJava') {
    options.compilerArgs += ['--enable-preview']
}

tasks.named('compileTestJava') {
    options.compilerArgs += ['--enable-preview']
}

tasks.named('test') {
    jvmArgs += ['--enable-preview']
    useJUnitPlatform()
}

tasks.withType(JavaExec).configureEach {
    jvmArgs += ['--enable-preview']
}
```

### 3. Spring Boot Runtime Configuration
```gradle
// Boot run configuration
bootRun {
    jvmArgs = [
        "-Dspring.threads.virtual.enabled=true",
        "--enable-preview"
    ]
}

// Spring Boot configuration
springBoot {
    mainClass = 'com.trademaster.[service].[ServiceName]Application'
    buildInfo()
}
```

**❌ NEVER USE** (causes compilation errors):
```gradle
applicationDefaultJvmArgs = [...]  // NOT COMPATIBLE WITH SPRING BOOT PLUGIN
```

### 4. Lombok Configuration
Ensure Lombok is properly configured with annotation processing:

```gradle
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

## 🚨 Critical API Compatibility Fixes

### Micrometer Timer API (Spring Boot 3.5.3)
```java
// ❌ INCORRECT (Timer.tag() doesn't exist)
timer.stop(Timer.Sample.start(meterRegistry)).tag("result", "success");

// ✅ CORRECT
Timer.Sample sample = Timer.Sample.start(meterRegistry);
sample.stop(Timer.builder("operation.time")
    .tag("result", "success")
    .register(meterRegistry));
```

### JWT API (JJWT 0.12.5+)
```java
// ❌ INCORRECT (parserBuilder() doesn't exist)
Jwts.parserBuilder().verifyWith(secretKey).build()

// ✅ CORRECT
Jwts.parser().verifyWith(secretKey).build()
```

### Micrometer Gauge API
```java
// ❌ INCORRECT (old API)
Gauge.builder("metric.name")
    .register(meterRegistry, this, obj -> obj.getValue());

// ✅ CORRECT (new API)
Gauge.builder("metric.name", this, obj -> (double) obj.getValue())
    .register(meterRegistry);
```

### Redis API Compatibility
Remove deprecated Redis method calls and use current API patterns.

### Spring Vault API
```java
// ✅ CORRECT return types
public <T> VaultResponseSupport<T> read(String path, Class<T> responseType)
public VaultResponse write(String path, Object body)
```

## 📁 Project Structure Standards

### Service Naming Convention
- Service directories: `[domain]-service` (e.g., `auth-service`)
- Main class: `[Domain]ServiceApplication` (e.g., `AuthServiceApplication`)
- Package structure: `com.trademaster.[domain].*`

### Configuration Files
- `application.yml` - Main configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

## 🔄 Build and Testing Standards

### Test Configuration
```gradle
tasks.named('test') {
    jvmArgs += ['--enable-preview']
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

### Docker and Containerization
All services should be containerizable with consistent Docker configurations.

### Database Migrations
Use Flyway for database schema management:
```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

## 🚀 Performance and Optimization

### Virtual Threads (Java 24)
Enable Virtual Threads for all services:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### JVM Optimization
Standard JVM flags for all services:
```gradle
jvmArgs = [
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=200",
    "--enable-preview",
    "-Dspring.threads.virtual.enabled=true"
]
```

## 📊 Monitoring and Observability

### Required Dependencies
```gradle
// Monitoring & Metrics - MANDATORY
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.micrometer:micrometer-tracing-bridge-brave'

// Structured Logging - MANDATORY
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

### Health Checks
Implement Spring Boot Actuator health endpoints for all services.

## 🔐 Security Standards

### Authentication and Authorization
- JWT tokens with proper validation
- Rate limiting implementation
- Security headers configuration

### Data Protection
- Encryption at rest and in transit
- Proper secret management (Vault integration)
- Audit logging for compliance

## 📋 Compliance and Quality

### Code Quality
- Lombok for reducing boilerplate
- Proper exception handling
- Structured logging with correlation IDs

### Testing Requirements
- Unit tests with JUnit 5
- Integration tests with Testcontainers
- Minimum 80% code coverage

## 🔄 Migration Guide

### Upgrading Existing Services
1. Update `build.gradle` with standardized configuration
2. Remove deprecated `sourceCompatibility`/`targetCompatibility`
3. Add preview features configuration
4. Update API calls for compatibility
5. Test compilation and runtime behavior

### Verification Steps
1. `gradle compileJava` - Must succeed without errors
2. `gradle test` - All tests must pass
3. `gradle bootRun` - Application must start successfully
4. Health check endpoints must be accessible

## 🛠️ Troubleshooting

### Common Issues and Solutions

**Issue**: `applicationDefaultJvmArgs` error
**Solution**: Replace with `bootRun { jvmArgs = [...] }`

**Issue**: Lombok annotation processing failures
**Solution**: Ensure proper `annotationProcessor` configuration

**Issue**: Preview features not enabled
**Solution**: Add `--enable-preview` to all compilation tasks

**Issue**: Timer.tag() method not found
**Solution**: Use Timer.builder() pattern instead

## 📝 Maintenance

### Regular Updates
- Monitor Spring Boot releases for compatibility
- Update dependency versions quarterly
- Review and update JVM optimization flags
- Maintain API compatibility documentation

### Documentation
- Keep this document updated with any configuration changes
- Document any new patterns or API compatibility issues
- Maintain service-specific configuration notes

---

**Last Updated**: 2025-01-26  
**Next Review**: 2025-04-26  
**Owner**: TradeMaster Development Team  
**Contact**: development@trademaster.com