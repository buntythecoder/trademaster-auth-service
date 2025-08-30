# TradeMaster Project Rules for Claude

## Project Context
TradeMaster is a comprehensive trading platform built with Java 24+ Virtual Threads, Spring Boot 3.5+, and AgentOS framework. Focus on financial data integrity, real-time performance, and enterprise-grade security.

## MANDATORY JAVA DEVELOPMENT RULES ⚠️
**THESE 10 RULES ARE ABSOLUTE - NO EXCEPTIONS**

### 1. Standards Directory Compliance (CRITICAL)
**MUST** check against ALL files in `/standards/` directory:
- `advanced-design-patterns.md` - Follow all design patterns and architectural guidelines
- `functional-programming-guide.md` - Apply functional programming principles
- `tech-stack.md` - Use only approved technologies and versions
- `trademaster-coding-standards.md` - Follow all coding conventions
- `code-style.md` - Apply consistent code formatting and style
- Any other standards files - ALL must be followed without deviation

### 2. Zero Placeholders/TODOs Policy (CRITICAL)
**ABSOLUTELY FORBIDDEN**:
- ❌ TODO comments or TODO markers
- ❌ Placeholder comments ("for production", "implement later")
- ❌ Demo code or simplified versions  
- ❌ "In a real implementation" comments
- ❌ Version downgrades of any dependencies
- ❌ Temporary workarounds or hacks
- ❌ Comments suggesting future implementation

### 3. Zero Warnings Policy (CRITICAL)
**FIX ALL WARNINGS**:
- ❌ Use lambda expressions instead of anonymous classes
- ❌ Use method references where applicable (String::valueOf instead of x -> String.valueOf(x))
- ❌ Remove unused methods OR implement missing functionality
- ❌ Replace deprecated code with modern alternatives
- ❌ Remove unused imports and variables
- ❌ Fix all compiler warnings before committing

### 4. Lombok & Records Usage (CRITICAL)
**MANDATORY PATTERNS**:
- ✅ `@Slf4j` for all logging (never System.out/err)
- ✅ `@RequiredArgsConstructor` for dependency injection
- ✅ `@Data` for simple DTOs and entities
- ✅ `@Getter/@Setter` for fine-grained control
- ✅ Records for immutable data holders (DTOs, value objects)
- ✅ Custom getters for AtomicInteger/AtomicLong (Lombok can't handle these)
- ❌ Manual getters/setters when Lombok can generate them

### 5. Structured Logging & Monitoring (CRITICAL)
**MANDATORY REQUIREMENTS**:
- ✅ Use `@Slf4j` and structured logging with placeholders: `log.info("User {} logged in with role {}", userId, role)`
- ✅ Include correlation IDs in all log entries
- ✅ Use proper log levels (ERROR, WARN, INFO, DEBUG, TRACE)
- ✅ Prometheus metrics for all business operations
- ✅ Health checks for all services and dependencies
- ✅ Performance monitoring with response time tracking
- ❌ System.out.println() or System.err.println()
- ❌ String concatenation in log messages

### 6. Dynamic Configuration (CRITICAL)
**ALL CONFIGURATION MUST BE EXTERNALIZED**:
- ✅ Use `@Value("${property.name}")` for all configurable values
- ✅ `@ConfigurationProperties` for complex configuration groups
- ✅ Environment-specific profiles (dev, test, prod)
- ✅ Default values for all properties: `@Value("${timeout:5000}")`
- ❌ Hardcoded values anywhere in code
- ❌ Magic numbers without constants

### 7. Constants Policy (CRITICAL)
**REPLACE ALL MAGIC NUMBERS/STRINGS**:
- ✅ Create public static final constants for all fixed values
- ✅ Group related constants in dedicated classes
- ✅ Use meaningful constant names: `MAX_RETRY_ATTEMPTS` not `FIVE`
- ✅ Document complex constants with comments
- ❌ Magic numbers: `thread.sleep(5000)` → use `HEALTH_CHECK_INTERVAL_MS`
- ❌ Magic strings: `"ACTIVE"` → use `Status.ACTIVE`

### 8. Zero Compilation Errors (CRITICAL)
**ABSOLUTE REQUIREMENT**:
- ✅ All classes must compile without any errors
- ✅ All dependencies must be properly resolved
- ✅ All imports must be valid and used
- ✅ All method signatures must be correct
- ✅ Run `./gradlew build` successfully before any commit

### 9. Advanced Design Patterns Audit (CRITICAL)
**MUST AUDIT FINAL CODE AGAINST**:
- ✅ Apply ALL patterns from `standards/advanced-design-patterns.md`
- ✅ Verify proper implementation of architectural patterns
- ✅ Ensure design principles are correctly followed
- ✅ Validate pattern usage matches project requirements
- ✅ Check for anti-patterns and correct them
- ❌ Ignore or skip pattern compliance validation

### 10. Functional Programming Audit (CRITICAL)
**MUST AUDIT FINAL CODE AGAINST**:
- ✅ Apply ALL guidelines from `standards/functional-programming-guide.md`
- ✅ Verify immutability principles are followed
- ✅ Ensure functional programming patterns are used
- ✅ Validate stream operations and functional interfaces
- ✅ Check for proper error handling in functional style
- ❌ Use imperative style where functional is required

## Java & Framework Rules

### Financial Domain Rules
- **Data Integrity**: All financial calculations must be precise (BigDecimal for money)
- **Audit Trail**: Log all financial operations with correlation IDs
- **Security**: Never log sensitive data (API keys, tokens, PII)
- **Compliance**: Maintain audit logs for regulatory compliance

### AgentOS Integration
- **MCP Protocol**: Implement proper Multi-Agent Communication Protocol
- **Capability Registry**: Track agent health and performance metrics
- **Registration**: Proper lifecycle management with state tracking
- **Health Checks**: Real-time health monitoring with degradation alerts

### Performance Standards
- **Real-time Data**: <5ms processing latency
- **API Response**: <200ms for standard operations
- **Database**: Optimized queries with connection pooling
- **Caching**: Redis with appropriate TTL strategies
- **Memory**: Efficient resource usage with virtual threads

## Architecture Patterns

### Service Structure
```
service/
├── agentos/          # AgentOS framework integration
├── config/           # Spring configuration classes
├── controller/       # REST API controllers
├── dto/             # Data transfer objects
├── entity/          # JPA entities
├── repository/      # Data access layer
├── service/         # Business logic
└── websocket/       # WebSocket handlers
```

### Naming Conventions
- **Classes**: PascalCase with descriptive names
- **Methods**: camelCase with action verbs
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase with domain separation

### Documentation Requirements
- **Class**: Purpose, key features, performance targets, author, version
- **Methods**: Brief description of functionality and parameters
- **APIs**: OpenAPI/Swagger documentation
- **Configuration**: Explain all settings and their impacts

## Testing Standards
- **Unit Tests**: >80% coverage for business logic
- **Integration Tests**: >70% coverage for service interactions  
- **Performance Tests**: Validate response time requirements
- **Security Tests**: Validate authentication and authorization

## Security Requirements
- **Authentication**: JWT with proper validation
- **Authorization**: Role-based access control
- **Data Protection**: Encrypt sensitive data at rest and in transit
- **Logging**: No sensitive information in logs
- **Dependencies**: Regular security vulnerability scanning

## Deployment Standards
- **Docker**: Containerized deployments with multi-stage builds
- **Health Checks**: Comprehensive health endpoints
- **Monitoring**: Prometheus metrics with Grafana dashboards
- **Logging**: Structured JSON logging for ELK stack
- **Configuration**: Externalized configuration with profiles

## MANDATORY PRE-COMMIT CHECKLIST ✅
**EVERY SINGLE CODE CHANGE MUST PASS ALL ITEMS**:

### Standards Compliance
- [ ] ✅ Checked against `/standards/advanced-design-patterns.md`
- [ ] ✅ Checked against `/standards/functional-programming-guide.md`  
- [ ] ✅ Checked against `/standards/tech-stack.md`
- [ ] ✅ Checked against `/standards/trademaster-coding-standards.md`
- [ ] ✅ Checked against `/standards/code-style.md`
- [ ] ✅ All other standards files reviewed and applied

### Code Quality (Zero Tolerance)
- [ ] ❌ Zero TODO comments or placeholders
- [ ] ❌ Zero "for production" or demo code
- [ ] ❌ Zero hardcoded values (all externalized to config)
- [ ] ❌ Zero magic numbers (all replaced with constants)
- [ ] ❌ Zero compilation warnings
- [ ] ❌ Zero compilation errors
- [ ] ❌ Zero unused methods/imports/variables
- [ ] ❌ Zero deprecated code usage
- [ ] ❌ Zero System.out/err usage

### Lombok & Records 
- [ ] ✅ `@Slf4j` used for all logging
- [ ] ✅ `@RequiredArgsConstructor` for dependency injection
- [ ] ✅ `@Data/@Getter/@Setter` used instead of manual getters/setters
- [ ] ✅ Records used for immutable data structures
- [ ] ✅ Custom getters for AtomicInteger/AtomicLong

### Configuration & Constants
- [ ] ✅ All config values use `@Value("${property:default}")`
- [ ] ✅ Complex config uses `@ConfigurationProperties`
- [ ] ✅ All magic numbers extracted to constants
- [ ] ✅ Constants grouped in dedicated classes
- [ ] ✅ Meaningful constant names used

### Logging & Monitoring
- [ ] ✅ Structured logging with placeholders
- [ ] ✅ Correlation IDs included
- [ ] ✅ Proper log levels used
- [ ] ✅ Prometheus metrics added for business operations
- [ ] ✅ Health checks implemented
- [ ] ✅ Performance monitoring in place

### Modern Java Usage
- [ ] ✅ Lambda expressions used instead of anonymous classes
- [ ] ✅ Method references used where applicable
- [ ] ✅ Java 24+ features utilized (Virtual Threads, sealed interfaces, records)
- [ ] ✅ Stream API used for collections processing

### Build & Compilation
- [ ] ✅ `./gradlew build` passes without errors
- [ ] ✅ All tests pass
- [ ] ✅ No dependency conflicts
- [ ] ✅ All imports valid and necessary

### Design Patterns & Functional Programming Audits
- [ ] ✅ Final code audited against `standards/advanced-design-patterns.md`
- [ ] ✅ All required design patterns properly implemented
- [ ] ✅ No anti-patterns detected or corrected
- [ ] ✅ Final code audited against `standards/functional-programming-guide.md`
- [ ] ✅ Immutability principles followed
- [ ] ✅ Functional programming patterns correctly used
- [ ] ✅ Stream operations and functional interfaces validated

## Enforcement Policy
**ABSOLUTE REQUIREMENTS - NO EXCEPTIONS**:
1. All 10 mandatory rules must be followed
2. Pre-commit checklist must be 100% complete
3. Code review must verify all standards compliance
4. Automated builds must pass all quality gates
5. Any violation requires immediate fix before merge

## Quick Reference Commands
```bash
# Check compilation
./gradlew build

# Run tests  
./gradlew test

# Check for unused dependencies
./gradlew dependencyInsight

# Code style check (if configured)
./gradlew checkstyleMain
```

**Remember: These rules exist to maintain the highest code quality for a financial trading platform. Every violation risks system reliability and regulatory compliance.**