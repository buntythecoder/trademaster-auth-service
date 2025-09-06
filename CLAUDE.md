# TradeMaster Project Rules for Claude

## Project Context
TradeMaster is a comprehensive trading platform built with Java 24+ Virtual Threads, Spring Boot 3.5+, and AgentOS framework. Focus on financial data integrity, real-time performance, and enterprise-grade security.

## MANDATORY JAVA DEVELOPMENT RULES ⚠️
**THESE 27 RULES ARE ABSOLUTE - NO EXCEPTIONS**

### 1. Java 24 + Virtual Threads Architecture (CRITICAL)
**MANDATORY TECHNOLOGY STACK**:
- ✅ Java 24 with `--enable-preview` flag and Virtual Threads ONLY
- ✅ Spring Boot 3.5.3 + Spring MVC (❌ NO WebFlux/Reactive)
- ✅ JPA/Hibernate with HikariCP (❌ NO R2DBC) 
- ✅ OkHttp or Apache HttpClient5 (❌ NO WebClient)
- ✅ `spring.threads.virtual.enabled=true` MANDATORY
- ✅ CompletableFuture for async operations with Virtual Threads
- ❌ FORBIDDEN: Spring WebFlux, R2DBC, WebClient, Mono/Flux, reactor-test

### 2. SOLID Principles Enforcement (CRITICAL)
**MANDATORY IMPLEMENTATION**:
- ✅ **Single Responsibility**: Each class has ONE reason to change, max 5 methods per class
- ✅ **Open/Closed**: Use strategy patterns, functional interfaces for extension
- ✅ **Liskov Substitution**: Sealed classes, proper inheritance hierarchies
- ✅ **Interface Segregation**: Small, focused interfaces with specific purposes
- ✅ **Dependency Inversion**: Constructor injection, depend on abstractions
- ❌ FORBIDDEN: God classes, utility classes with >3 methods, static dependencies

### 3. Functional Programming First (CRITICAL)
**MANDATORY PATTERNS**:
- ✅ **No if-else**: Use pattern matching, Optional, Strategy pattern, Map lookups
- ✅ **No loops**: Use Stream API, recursive functions, functional composition
- ✅ **Immutable data**: Records, sealed classes, no mutable fields
- ✅ **Function composition**: Higher-order functions, method references
- ✅ **Monadic patterns**: Optional, CompletableFuture, Result types
- ❌ FORBIDDEN: if-else statements, for/while loops, mutable data structures

### 4. Advanced Design Patterns (CRITICAL)
**MANDATORY IMPLEMENTATION**:
- ✅ **Factory Pattern**: Functional factories with enum-based implementations
- ✅ **Builder Pattern**: Records with fluent builder APIs
- ✅ **Strategy Pattern**: Function-based strategies, no if-else conditionals
- ✅ **Command Pattern**: Functional command objects with CompletableFuture
- ✅ **Observer Pattern**: Event publishers with functional observers
- ✅ **Adapter Pattern**: Functional adapters with method composition
- ❌ FORBIDDEN: God patterns, complex inheritance hierarchies

### 5. Cognitive Complexity Control (CRITICAL)
**MANDATORY LIMITS**:
- ✅ **Method Complexity**: Max cognitive complexity of 7 per method
- ✅ **Class Complexity**: Max 15 total complexity per class
- ✅ **Cyclomatic Complexity**: Max 5 branches per method
- ✅ **Nesting Depth**: Max 3 levels of nesting
- ✅ **Method Length**: Max 15 lines per method
- ✅ **Class Size**: Max 200 lines per class, max 10 methods per class
- ❌ FORBIDDEN: God methods, complex nested structures, deep conditionals

### 6. Zero Trust Security Policy (CRITICAL)
**MANDATORY TIERED SECURITY PATTERNS**:
- ✅ **External Access**: SecurityFacade + SecurityMediator for all REST APIs and external calls
- ✅ **Internal Access**: Simple constructor injection for service-to-service communication
- ✅ **Default Deny**: All external access denied by default, explicit grants only
- ✅ **Least Privilege**: Use Builder/Factory patterns for controlled object creation
- ✅ **Security Boundary**: Clear separation between external (full security) and internal (lightweight)
- ✅ **Mediator Coordination**: SecurityMediator coordinates authentication, authorization, risk assessment
- ✅ **Audit Trail**: Log ALL external access attempts with correlation IDs
- ✅ **Input Validation**: Functional validation chains for all external inputs
- ❌ FORBIDDEN: Public fields, bypassing security facade, over-engineering internal calls

### 7. Zero Placeholders/TODOs Policy (CRITICAL)
**ABSOLUTELY FORBIDDEN**:
- ❌ TODO comments or TODO markers
- ❌ Placeholder comments ("for production", "implement later")
- ❌ Demo code or simplified versions  
- ❌ "In a real implementation" comments
- ❌ Version downgrades of any dependencies
- ❌ Temporary workarounds or hacks
- ❌ Comments suggesting future implementation

### 8. Zero Warnings Policy (CRITICAL)
**FIX ALL WARNINGS**:
- ❌ Use lambda expressions instead of anonymous classes
- ❌ Use method references where applicable (String::valueOf instead of x -> String.valueOf(x))
- ❌ Remove unused methods OR implement missing functionality
- ❌ Replace deprecated code with modern alternatives
- ❌ Remove unused imports and variables
- ❌ Fix all compiler warnings before committing

### 9. Immutability & Records Usage (CRITICAL)
**MANDATORY PATTERNS**:
- ✅ Records for ALL data holders (DTOs, value objects, entities)
- ✅ Sealed classes for type hierarchies and pattern matching
- ✅ Immutable collections (List.of(), Set.of(), Map.of())
- ✅ Builder pattern for complex record construction
- ✅ Validation in record compact constructors
- ❌ FORBIDDEN: Mutable data classes, setters on data objects, mutable fields

### 10. Lombok Standards (CRITICAL)
**MANDATORY PATTERNS**:
- ✅ `@Slf4j` for all logging (never System.out/err)
- ✅ `@RequiredArgsConstructor` for dependency injection
- ✅ `@Data` for simple DTOs only (when records not applicable)
- ✅ `@Getter/@Setter` for fine-grained control ONLY when needed
- ✅ Custom getters for AtomicInteger/AtomicLong (Lombok can't handle these)
- ❌ FORBIDDEN: Manual getters/setters when Lombok can generate them

### 11. Error Handling Patterns (CRITICAL)
**MANDATORY IMPLEMENTATION**:
- ✅ **Result Types**: Use Either<Error, Success> pattern for all operations
- ✅ **Railway Programming**: Chain operations with flatMap/map
- ✅ **No try-catch**: Wrap in functional constructs (tryExecute, safely)
- ✅ **Optional Usage**: Never return null, use Optional or Result types
- ✅ **Validation Chains**: Functional validation with error accumulation
- ❌ FORBIDDEN: try-catch in business logic, null returns, unchecked exceptions

### 12. Virtual Threads & Concurrency (CRITICAL)
**MANDATORY IMPLEMENTATION**:
- ✅ **Virtual Thread Factory**: Use Executors.newVirtualThreadPerTaskExecutor()
- ✅ **Structured Concurrency**: Use StructuredTaskScope for coordinated tasks
- ✅ **Lock-Free Patterns**: Use AtomicReference, ConcurrentHashMap
- ✅ **Async Operations**: CompletableFuture with virtual thread executors
- ✅ **Channel Patterns**: BlockingQueue for async communication
- ❌ FORBIDDEN: Platform threads for I/O, traditional thread pools, blocking locks

### 13. Stream API Mastery (CRITICAL)
**MANDATORY USAGE**:
- ✅ **Replace all loops**: Use streams for ALL collection processing
- ✅ **Parallel Processing**: Use parallelStream() with virtual threads
- ✅ **Custom Collectors**: Create domain-specific collectors
- ✅ **Lazy Evaluation**: Use stream suppliers for expensive operations
- ✅ **Functional Pipeline**: map, filter, flatMap, reduce operations
- ❌ FORBIDDEN: for/while loops, imperative collection processing

### 14. Pattern Matching Excellence (CRITICAL)
**MANDATORY USAGE**:
- ✅ **Switch Expressions**: Use pattern matching for all conditionals
- ✅ **Sealed Classes**: Use with pattern matching for type safety
- ✅ **Guard Conditions**: Use when clauses for complex conditions
- ✅ **Record Patterns**: Destructure records in switch expressions
- ✅ **Type Patterns**: Replace instanceof checks with pattern matching
- ❌ FORBIDDEN: if-else chains, instanceof without pattern matching

### 15. Structured Logging & Monitoring (CRITICAL)
**MANDATORY REQUIREMENTS**:
- ✅ Use `@Slf4j` and structured logging with placeholders
- ✅ Include correlation IDs in ALL log entries
- ✅ Prometheus metrics for ALL business operations
- ✅ Health checks for ALL services and dependencies
- ✅ Performance monitoring with response time tracking
- ✅ Security metrics for authentication and authorization
- ❌ FORBIDDEN: System.out/err, string concatenation in logs, unstructured logging

### 16. Dynamic Configuration (CRITICAL)
**ALL CONFIGURATION MUST BE EXTERNALIZED**:
- ✅ Use `@Value("${property.name}")` for all configurable values
- ✅ `@ConfigurationProperties` for complex configuration groups
- ✅ Environment-specific profiles (dev, test, prod)
- ✅ Default values for all properties: `@Value("${timeout:5000}")`
- ❌ FORBIDDEN: Hardcoded values, magic numbers, configuration in code

### 17. Constants & Magic Numbers (CRITICAL)
**REPLACE ALL MAGIC NUMBERS/STRINGS**:
- ✅ Create public static final constants for all fixed values
- ✅ Group related constants in dedicated classes or enums
- ✅ Use meaningful constant names: `MAX_RETRY_ATTEMPTS` not `FIVE`
- ✅ Document complex constants with comments
- ❌ FORBIDDEN: Magic numbers, magic strings, unexplained literals

### 18. Method & Class Naming (CRITICAL)
**MANDATORY CONVENTIONS**:
- ✅ **Classes**: PascalCase with single responsibility names
- ✅ **Methods**: camelCase with action verbs (calculate, validate, process)
- ✅ **Predicates**: isValid, hasValue, canProcess
- ✅ **Functions**: transform, convert, map, filter
- ✅ **Constants**: UPPER_SNAKE_CASE with descriptive names
- ❌ FORBIDDEN: Generic names, abbreviations, unclear method names

### 19. Access Control & Encapsulation (CRITICAL)
**ZERO TRUST ACCESS CONTROL**:
- ✅ **Default Private**: All fields and methods private by default
- ✅ **Explicit Public**: Only expose what's absolutely necessary
- ✅ **Builder Access**: Use Builder pattern for controlled object construction
- ✅ **Factory Access**: Use Factory pattern for complex object creation
- ✅ **Facade Pattern**: Single entry point for external access
- ❌ FORBIDDEN: Public fields, package-private without justification, broad access

### 20. Testing Standards (CRITICAL)
**MANDATORY TESTING PATTERNS**:
- ✅ **Unit Tests**: >80% coverage for business logic with functional test builders
- ✅ **Integration Tests**: >70% coverage using TestContainers
- ✅ **Property Testing**: Use functional property-based testing
- ✅ **Virtual Thread Testing**: Test concurrency with virtual threads
- ✅ **Pattern Testing**: Validate all design patterns work correctly
- ❌ FORBIDDEN: Mocking complex behavior, testing implementation details

### 21. Code Organization (CRITICAL)
**MANDATORY STRUCTURE**:
- ✅ **Feature-Based Packages**: Group by business capability
- ✅ **Clean Architecture**: domain, application, infrastructure, presentation
- ✅ **Single Responsibility**: One concern per package/class
- ✅ **Dependency Direction**: Inward dependencies only
- ❌ FORBIDDEN: Technical packages (util, common), circular dependencies

### 22. Performance Standards (CRITICAL)
**MANDATORY PERFORMANCE TARGETS**:
- ✅ **API Response**: <200ms for standard operations
- ✅ **Order Processing**: <50ms with virtual threads
- ✅ **Database Queries**: Optimized with proper indexing
- ✅ **Memory Usage**: Efficient with immutable data structures
- ✅ **Concurrent Users**: 10,000+ supported with virtual threads
- ❌ FORBIDDEN: Performance regressions, inefficient algorithms

### 23. Security Implementation (CRITICAL)
**MANDATORY SECURITY PATTERNS**:
- ✅ **JWT Authentication**: Stateless with proper validation
- ✅ **Role-Based Access**: Method-level security with SpEL
- ✅ **Input Sanitization**: Validate all inputs with functional chains
- ✅ **Audit Logging**: Log all security-relevant operations
- ✅ **Secure Defaults**: Fail-safe security configurations
- ❌ FORBIDDEN: Security through obscurity, weak authentication

### 24. Zero Compilation Errors (CRITICAL)
**ABSOLUTE REQUIREMENT**:
- ✅ All classes must compile without any errors
- ✅ All dependencies must be properly resolved
- ✅ All imports must be valid and used
- ✅ All method signatures must be correct
- ✅ Run `./gradlew build` successfully before any commit
- ❌ FORBIDDEN: Compilation warnings, build failures, missing dependencies

### 25. Circuit Breaker Implementation (CRITICAL)
**MANDATORY RESILIENCE PATTERNS**:
- ✅ **External API Calls**: Circuit breaker for ALL external service calls (broker APIs, market data)
- ✅ **Database Operations**: Circuit breaker for critical database operations with high latency risk
- ✅ **Message Queue**: Circuit breaker for message publishing/consuming operations
- ✅ **File I/O Operations**: Circuit breaker for file system operations that may fail
- ✅ **Network Operations**: Circuit breaker for all network-dependent operations
- ✅ **Functional Implementation**: Use functional circuit breaker with CompletableFuture
- ✅ **Resilience4j Integration**: Spring Boot integration with proper configuration
- ✅ **Fallback Strategies**: Meaningful fallback responses, not just empty responses
- ✅ **Metrics & Monitoring**: Circuit breaker state changes logged with correlation IDs
- ❌ FORBIDDEN: Direct external calls without circuit breaker, blocking circuit breakers

### 26. Configuration Synchronization Audit (CRITICAL)
**MANDATORY YAML/PROPERTIES VALIDATION**:
- ✅ **Deprecated Keys**: Remove ALL deprecated configuration keys from YAML/properties
- ✅ **Code Sync**: All `@Value` and `@ConfigurationProperties` must have corresponding config entries
- ✅ **Default Values**: All configuration properties must have reasonable defaults in code
- ✅ **Environment Profiles**: Verify dev/test/prod profiles have consistent property structures
- ✅ **Spring Boot Migration**: Update to latest Spring Boot 3.5+ configuration conventions
- ✅ **Property Validation**: Use `@Validated` on `@ConfigurationProperties` classes
- ✅ **Type Safety**: Ensure configuration types match code expectations (Duration, DataSize, etc.)
- ✅ **Documentation**: Document all custom properties with descriptions and examples
- ✅ **Security**: No sensitive data in properties files (use environment variables or Vault)
- ❌ FORBIDDEN: Deprecated properties, unused configuration, hardcoded sensitive values

### 27. Standards Compliance Audit (CRITICAL)
**FINAL VERIFICATION REQUIRED**:
- ✅ **Design Patterns**: Audit against `standards/advanced-design-patterns.md`
- ✅ **Functional Programming**: Audit against `standards/functional-programming-guide.md`
- ✅ **Tech Stack**: Verify compliance with `standards/tech-stack.md`
- ✅ **Coding Standards**: Check `standards/trademaster-coding-standards.md`
- ✅ **Code Style**: Follow `standards/code-style.md` and `standards/best-practices.md`
- ❌ FORBIDDEN: Skipping standards compliance, partial implementation

## Financial Domain Rules

### Data Integrity & Precision
- **Money Calculations**: Use BigDecimal for ALL financial calculations
- **Audit Trail**: Log ALL financial operations with correlation IDs
- **Security**: Never log sensitive data (API keys, tokens, PII)
- **Compliance**: Maintain audit logs for regulatory compliance
- **Validation**: Functional validation chains for all financial data

### AgentOS Integration
- **MCP Protocol**: Implement proper Multi-Agent Communication Protocol
- **Capability Registry**: Track agent health and performance metrics
- **Registration**: Proper lifecycle management with state tracking
- **Health Checks**: Real-time health monitoring with degradation alerts

## Architecture Patterns

### Service Structure
```
service/
├── agentos/          # AgentOS framework integration
├── config/           # Spring configuration classes  
├── controller/       # REST API controllers
├── dto/             # Data transfer objects (Records only)
├── entity/          # JPA entities
├── repository/      # Data access layer
├── service/         # Business logic (functional)
└── websocket/       # WebSocket handlers
```

### Security Access Control Pattern (Zero Trust - Tiered Approach)

#### **Level 1: External Access** (Full Security Stack)
```java
// MANDATORY: SecurityFacade + SecurityMediator for external access
@Component
public class SecurityFacade {
    private final SecurityMediator mediator;  // Delegates to mediator
    
    public <T> Result<T, SecurityError> secureAccess(
            SecurityContext context,
            Function<Void, T> operation) {
        return mediator.mediateAccess(context, operation);
    }
}

@Component  
public class SecurityMediator {
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final AuditService auditService;
    private final RiskAssessmentService riskService;
    
    public <T> Result<T, SecurityError> mediateAccess(
            SecurityContext context,
            Function<Void, T> operation) {
        
        // Mediator coordinates all security components
        return authService.authenticate(context)
            .flatMap(authzService::authorize) 
            .flatMap(riskService::assessRisk)
            .flatMap(this::executeOperation)
            .map(result -> auditService.log(context, result));
    }
}
```

#### **Level 2: Internal Service-to-Service** (Lightweight Direct Access)
```java
// MANDATORY: Simple direct injection for internal services
@Service
public class TradingService {
    private final PortfolioService portfolioService;  // Direct injection
    private final RiskService riskService;
    private final OrderRepository orderRepository;
    
    public Result<Order, OrderError> placeOrder(OrderRequest request) {
        // Direct service calls - already inside security boundary
        return validateOrder(request)
            .flatMap(order -> riskService.checkRisk(order))  // Direct call
            .flatMap(order -> portfolioService.updatePosition(order))  // Direct call
            .flatMap(orderRepository::save)
            .map(this::auditOrder);
    }
}
```

#### **Security Boundary Rules:**
- **Outside Boundary** (REST APIs, External calls): SecurityFacade + SecurityMediator
- **Inside Boundary** (Service-to-service): Simple constructor injection + direct calls

### Cognitive Complexity Control Pattern
```java
// GOOD: Max 7 cognitive complexity
public Result<OrderResult, OrderError> processOrder(OrderRequest request) {
    return validateOrder(request)
        .flatMap(this::checkRiskLimits)
        .flatMap(this::executeOrder)
        .map(this::auditOrder);
}

// Each method has max 5 lines, complexity 1-2
private Result<OrderRequest, ValidationError> validateOrder(OrderRequest request) {
    return Optional.of(request)
        .filter(this::isValidSymbol)
        .map(Result::success)
        .orElse(Result.failure(ValidationError.INVALID_SYMBOL));
}
```

## MANDATORY PRE-COMMIT CHECKLIST ✅
**EVERY SINGLE CODE CHANGE MUST PASS ALL 27 ITEMS**:

### Java 24 & Virtual Threads Compliance
- [ ] ✅ Java 24 with `--enable-preview` enabled
- [ ] ✅ Virtual threads enabled (`spring.threads.virtual.enabled=true`)
- [ ] ✅ No WebFlux/Reactive dependencies
- [ ] ✅ CompletableFuture with virtual thread executors
- [ ] ✅ Structured concurrency for complex operations

### Design Patterns & Architecture
- [ ] ✅ At least 2 design patterns implemented per class
- [ ] ✅ SOLID principles followed (especially SRP, DIP)
- [ ] ✅ Functional programming patterns used (no if-else, no loops)
- [ ] ✅ Pattern matching used for conditionals
- [ ] ✅ Immutable data structures (Records, sealed classes)

### Cognitive Complexity Control
- [ ] ✅ Method complexity ≤ 7 per method
- [ ] ✅ Class complexity ≤ 15 per class
- [ ] ✅ Max 15 lines per method
- [ ] ✅ Max 10 methods per class
- [ ] ✅ Max 3 levels of nesting

### Zero Trust Security (Tiered Approach)
- [ ] ✅ External access uses SecurityFacade + SecurityMediator pattern
- [ ] ✅ Internal service-to-service uses simple constructor injection
- [ ] ✅ Clear security boundary separation (external vs internal)
- [ ] ✅ Builder/Factory patterns for controlled object creation
- [ ] ✅ Input validation with functional chains for external inputs
- [ ] ✅ Audit logging for all external access attempts
- [ ] ✅ No over-engineering of internal service communication

### Code Quality (Zero Tolerance)
- [ ] ❌ Zero TODO comments or placeholders
- [ ] ❌ Zero compilation warnings
- [ ] ❌ Zero hardcoded values (all externalized)
- [ ] ❌ Zero magic numbers (all constants)
- [ ] ❌ Zero mutable data structures

### Functional Programming Excellence
- [ ] ✅ No if-else statements (pattern matching, Optional, strategies)
- [ ] ✅ No loops (Stream API, functional composition)
- [ ] ✅ Result/Either types for error handling
- [ ] ✅ Function composition over method chaining
- [ ] ✅ Higher-order functions and monadic patterns

### Virtual Threads & Concurrency
- [ ] ✅ Virtual thread factory for async operations
- [ ] ✅ Structured concurrency for coordinated tasks
- [ ] ✅ Lock-free patterns (AtomicReference, ConcurrentHashMap)
- [ ] ✅ No platform threads for I/O operations

### Configuration & Monitoring
- [ ] ✅ All config externalized with `@Value` or `@ConfigurationProperties`
- [ ] ✅ Structured logging with correlation IDs
- [ ] ✅ Prometheus metrics for business operations
- [ ] ✅ Health checks implemented

### Testing & Documentation
- [ ] ✅ Unit tests >80% coverage with functional builders
- [ ] ✅ Integration tests with TestContainers
- [ ] ✅ Virtual thread concurrency testing
- [ ] ✅ API documentation with OpenAPI

### Circuit Breaker & Resilience
- [ ] ✅ Circuit breakers implemented for all external API calls
- [ ] ✅ Circuit breakers for database and message queue operations
- [ ] ✅ Resilience4j properly configured with Spring Boot
- [ ] ✅ Functional circuit breaker patterns with CompletableFuture
- [ ] ✅ Meaningful fallback strategies implemented
- [ ] ✅ Circuit breaker metrics and monitoring configured

### Configuration Synchronization
- [ ] ✅ All deprecated YAML/properties keys removed
- [ ] ✅ Code annotations sync with configuration files
- [ ] ✅ All properties have reasonable defaults
- [ ] ✅ Environment profiles have consistent structures
- [ ] ✅ Spring Boot 3.5+ configuration conventions followed
- [ ] ✅ Property validation with `@Validated` implemented
- [ ] ✅ No sensitive data in configuration files

### Final Standards Audit
- [ ] ✅ `standards/advanced-design-patterns.md` compliance verified
- [ ] ✅ `standards/functional-programming-guide.md` compliance verified
- [ ] ✅ `standards/tech-stack.md` compliance verified
- [ ] ✅ `standards/trademaster-coding-standards.md` compliance verified
- [ ] ✅ `standards/code-style.md` and `standards/best-practices.md` compliance verified

### Build & Deployment
- [ ] ✅ `./gradlew build` passes without errors or warnings
- [ ] ✅ All tests pass
- [ ] ✅ No dependency conflicts
- [ ] ✅ Performance benchmarks meet targets

## Enforcement Policy
**ABSOLUTE REQUIREMENTS - NO EXCEPTIONS**:
1. All 27 mandatory rules must be followed without deviation
2. Pre-commit checklist must be 100% complete
3. Code review must verify all standards compliance
4. Automated builds must pass all quality gates
5. Any violation requires immediate fix before merge
6. Cognitive complexity violations are blocking issues
7. Zero trust violations are security incidents

## Quick Reference Commands
```bash
# Check compilation and warnings
./gradlew build --warning-mode all

# Run all tests including integration
./gradlew test integrationTest

# Check dependencies
./gradlew dependencyInsight

# Performance benchmarks
./gradlew jmh

# Security scan
./gradlew dependencyCheckAnalyze

# Validate configuration
./gradlew bootRun --args='--spring.config.validate=true'

# Circuit breaker health check
curl -s http://localhost:8080/actuator/circuitbreakers
```

**Remember: These rules exist to maintain the highest code quality for a financial trading platform. Every violation risks system reliability, security, and regulatory compliance. The cognitive complexity and zero trust requirements are non-negotiable for enterprise-grade software.**