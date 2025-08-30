Claude Code Production Standards Rule
Core Rule Definition
Rule Name: PRODUCTION_CODE_STANDARDS
Priority: CRITICAL
Prohibited Practices
1. No Placeholders or Temporary Code

❌ Never use // TODO, // FIXME, // PLACEHOLDER
❌ No comments like "Add implementation here" or "Replace with actual logic"
❌ No dummy/mock data or temporary hardcoded values
❌ No simplified versions or partial implementations
❌ No "Coming soon" or "To be implemented" sections

2. Version Standards - No Downgrades

❌ Never suggest downgrading to older versions
❌ No instructions to use legacy versions "for compatibility"
✅ MANDATORY VERSIONS:

Java: 24 (always use latest features)
Spring Boot: 3.5.3 (exact version required)
Lombok: Latest compatible with Java 24 and Spring Boot 3.5.3
All dependencies: Latest stable versions compatible with above


❌ No suggestions to use Java 17, 11, or Spring Boot 2.x for "stability"

3. Architecture Standards

❌ No multiple classes in single file (except exceptional designs)
❌ No inline class definitions unless architecturally justified
❌ No utility classes mixed with business logic

Required Practices
1. Standards Directory Compliance
MUST follow all patterns defined in standards/ directory
	-advanced-design-patterns
	-functional-programming-guide
	-trademaster-coding-standards
	-tech-stack
	-code-style
	-best-practices
2. File Organization

✅ One class per file (mandatory)
✅ Proper package structure following standards
✅ Consistent naming conventions
✅ Exception: Only for exceptional architectural patterns (Builder inner classes, etc.)

3. Constants Management

✅ Extract all magic numbers to Constants classes
✅ Use proper constant naming (UPPER_SNAKE_CASE)
✅ Group related constants logically
✅ Document constant purposes

4. Complete Implementation

✅ Fully functional code with all methods implemented
✅ Proper error handling and validation
✅ Complete business logic implementation
✅ Production-ready logging and monitoring

5. Lombok & Records Usage (MANDATORY)

✅ @Slf4j for ALL logging (never System.out/err)
✅ @RequiredArgsConstructor for dependency injection
✅ @Data for simple DTOs and entities  
✅ @Getter/@Setter for fine-grained control
✅ Records for immutable data holders (DTOs, value objects)
✅ Custom getters for AtomicInteger/AtomicLong (Lombok can't handle these)
❌ Manual getters/setters when Lombok can generate them

6. Dynamic Configuration (CRITICAL)

✅ Use @Value("${property.name:defaultValue}") for all configurable values
✅ @ConfigurationProperties for complex configuration groups
✅ Environment-specific profiles (dev, test, prod)
❌ Hardcoded values anywhere in code
❌ Magic numbers without constants

7. Zero Warnings Policy (CRITICAL)

❌ Use lambda expressions instead of anonymous classes
❌ Use method references where applicable (String::valueOf vs x -> String.valueOf(x))
❌ Remove unused methods OR implement missing functionality
❌ Replace deprecated code with modern alternatives
❌ Remove unused imports and variables
❌ Fix ALL compiler warnings before committing

8. Structured Logging & Monitoring (MANDATORY)

✅ Use @Slf4j and structured logging: log.info("User {} logged in", userId)
✅ Include correlation IDs in all log entries
✅ Use proper log levels (ERROR, WARN, INFO, DEBUG, TRACE)
✅ Prometheus metrics for all business operations
✅ Health checks for all services and dependencies
❌ System.out.println() or System.err.println()
❌ String concatenation in log messages

Pre-Commit Checklist (MANDATORY)
Every code change MUST pass ALL these checks:

Standards Compliance
- [ ] Checked against /standards/advanced-design-patterns.md
- [ ] Checked against /standards/functional-programming-guide.md
- [ ] Checked against /standards/tech-stack.md
- [ ] Checked against /standards/trademaster-coding-standards.md
- [ ] Checked against /standards/code-style.md

Code Quality (Zero Tolerance)
- [ ] Zero TODO comments or placeholders
- [ ] Zero "for production" or demo code
- [ ] Zero hardcoded values (all externalized)
- [ ] Zero magic numbers (all constants)
- [ ] Zero compilation warnings
- [ ] Zero compilation errors
- [ ] Zero unused methods/imports/variables
- [ ] Zero System.out/err usage

Build Verification
- [ ] ./gradlew build passes without errors
- [ ] All tests pass
- [ ] No dependency conflicts

ABSOLUTE ENFORCEMENT: Any violation stops work immediately until fixed.