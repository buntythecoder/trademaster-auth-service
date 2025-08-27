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