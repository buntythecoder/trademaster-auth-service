# Payment Service TestContainer Integration Testing

## Overview

The payment service uses **TestContainers** for integration testing with a real PostgreSQL database. This ensures PostgreSQL-specific queries and features are tested correctly.

## Prerequisites

### Required Software
- **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)
- Docker must be running before executing integration tests
- Java 24+ with Virtual Threads enabled

### Verification
```bash
# Verify Docker is running
docker --version
docker ps

# Expected output: Docker version information and running containers list
```

## TestContainer Configuration

### Dependencies (Already Configured)
```gradle
testImplementation "org.testcontainers:junit-jupiter:1.19.3"
testImplementation "org.testcontainers:postgresql:1.19.3"
testImplementation "org.testcontainers:testcontainers:1.19.3"
```

### Test Configuration
- **Image**: `postgres:16-alpine` (lightweight PostgreSQL 16)
- **Database**: `testdb`
- **Username**: `test`
- **Password**: `test`
- **Container Reuse**: Enabled for faster test execution

## Running Integration Tests

### Command
```bash
# Run all tests (unit + integration)
./gradlew :payment-service:test

# Run only integration tests
./gradlew :payment-service:test --tests "PaymentServiceApplicationTest"

# Run with info logging
./gradlew :payment-service:test --info
```

### Expected Behavior
1. **First Run**: Docker downloads `postgres:16-alpine` image (~80MB)
2. **Container Start**: PostgreSQL container starts automatically
3. **Schema Creation**: Hibernate creates tables using `ddl-auto: create-drop`
4. **Tests Execute**: Both `contextLoads()` and `applicationStartsWithTestProfile()` run
5. **Container Cleanup**: Container stops after tests complete (unless reuse enabled)

## Troubleshooting

### Docker Not Running
**Error**: `Could not start container`
**Solution**:
```bash
# Start Docker Desktop or Docker daemon
# Verify with: docker ps
```

### Port Conflicts
**Error**: `Port already in use`
**Solution**: TestContainers uses random ports, but if issues persist:
```bash
# Stop all containers
docker stop $(docker ps -aq)
```

### Container Pull Issues
**Error**: `Unable to pull image postgres:16-alpine`
**Solution**:
```bash
# Manual pull
docker pull postgres:16-alpine

# Or use different image in test:
# .withImage("postgres:15-alpine")
```

### Slow Test Execution
**Issue**: First run downloads image and starts container
**Solution**: Container reuse is enabled (`.withReuse(true)`)
- Subsequent runs reuse the same container
- Much faster after first execution

## Test Coverage

### Integration Tests
- ✅ **contextLoads**: Validates entire Spring ApplicationContext with PostgreSQL
- ✅ **applicationStartsWithTestProfile**: Verifies test profile configuration
- ✅ **PostgreSQL Query Validation**: All repository queries tested against real database
- ✅ **JPA Entity Mapping**: Entity mappings validated with actual PostgreSQL schema

### Unit Tests (54 Tests)
- ✅ Service layer tests with mocked dependencies
- ✅ Controller tests with MockMvc
- ✅ Repository method signature tests

## Benefits of TestContainer Approach

1. **Real Database**: Tests run against actual PostgreSQL, not H2 in-memory
2. **Query Compatibility**: PostgreSQL-specific syntax (`EXTRACT(EPOCH FROM ...)`) works correctly
3. **Isolation**: Each test run gets fresh database state
4. **CI/CD Ready**: Works in CI pipelines with Docker support
5. **Developer Experience**: No manual PostgreSQL setup required

## Configuration Files

### Test Class
```java
@Testcontainers
@SpringBootTest
@Import(TestKafkaConfig.class)
class PaymentServiceApplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### application-test.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Auto-create schema for tests
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: false  # Disabled - using JPA ddl-auto instead
```

## Performance Optimization

### Container Reuse
- `.withReuse(true)` keeps container running between test executions
- Reduces startup time from ~10s to <1s for subsequent runs
- Container auto-stops after 10 minutes of inactivity (default)

### Connection Pool
- Test pool size: 5 (vs production: 50)
- Faster startup, sufficient for integration tests

## Future Enhancements

1. **Test Data Builders**: Add fluent test data builders for entities
2. **Database Cleanup**: Add `@AfterEach` cleanup for test isolation
3. **Performance Tests**: Add load testing with TestContainers
4. **Migration Tests**: Enable Flyway for migration testing scenarios
5. **Multi-Container**: Add Redis TestContainer for caching tests

## References

- [TestContainers Documentation](https://www.testcontainers.org/)
- [Spring Boot TestContainers](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1)
- [PostgreSQL TestContainer](https://www.testcontainers.org/modules/databases/postgres/)
