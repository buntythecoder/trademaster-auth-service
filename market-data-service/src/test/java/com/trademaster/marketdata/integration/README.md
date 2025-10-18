# TestContainers Integration Testing Guide

## Overview

This directory contains integration tests using TestContainers for real infrastructure testing with PostgreSQL, Redis, and Kafka.

## Configuration

### Test Profiles

- **`test`** - H2 in-memory database for fast unit tests
- **`integration`** - TestContainers with real PostgreSQL, Redis, Kafka

### TestContainers Setup

TestContainers automatically provisions and manages Docker containers for:

1. **PostgreSQL 16** - Database operations
2. **Redis 7** - Cache operations
3. **Kafka 7.6** - Event streaming

## Usage

### Running Integration Tests

```bash
# Run all integration tests
./gradlew test --tests "*Integration*"

# Run specific integration test
./gradlew test --tests "TestContainersIntegrationTest"

# Run with TestContainers debug logging
./gradlew test --tests "*Integration*" --debug
```

### Writing Integration Tests

#### Basic Integration Test

```java
@SpringBootTest
@ActiveProfiles("integration")
@Import(TestContainersConfiguration.class)
class MyIntegrationTest {

    @Autowired
    private MarketDataRepository repository;

    @Test
    void testDatabaseOperations() {
        // Test with real PostgreSQL
        var data = repository.save(createTestData());
        assertThat(data.getId()).isNotNull();
    }
}
```

#### Redis Cache Testing

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

@Test
void testCacheOperations() {
    redisTemplate.opsForValue().set("key", "value", 60, TimeUnit.SECONDS);
    var cached = redisTemplate.opsForValue().get("key");
    assertThat(cached).isEqualTo("value");
}
```

#### Kafka Streaming Testing

```java
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;

@Test
void testKafkaPublishing() {
    kafkaTemplate.send("topic", "message");
    // Add consumer to verify message received
}
```

## Performance Considerations

### Container Reuse

TestContainers are configured with `.withReuse(true)` for performance:

```java
@Bean
PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(...)
        .withReuse(true);  // Reuse across test runs
}
```

### Resource Limits

Containers are configured with appropriate resource limits:

- **PostgreSQL**: 200 connections, 256MB shared buffers
- **Redis**: 256MB maxmemory with LRU eviction
- **Kafka**: KRaft mode (no ZooKeeper overhead)

## Test Organization

```
integration/
├── README.md (this file)
├── TestContainersIntegrationTest.java (example)
├── DatabaseIntegrationTest.java
├── CacheIntegrationTest.java
├── KafkaIntegrationTest.java
└── FullSystemIntegrationTest.java
```

## Requirements

### Docker

TestContainers requires Docker to be installed and running:

```bash
# Check Docker is running
docker ps

# Docker must be accessible without sudo (Linux)
docker run hello-world
```

### Environment Variables

Optional environment variables for TestContainers:

```bash
# Reuse containers across test runs
export TESTCONTAINERS_REUSE_ENABLE=true

# Docker daemon socket (if non-standard)
export DOCKER_HOST=unix:///var/run/docker.sock
```

## Troubleshooting

### Container Start Failures

```bash
# Check Docker daemon is running
docker info

# Check available disk space
docker system df

# Clean up stopped containers
docker system prune -a
```

### Port Conflicts

TestContainers automatically assigns random ports to avoid conflicts. Access via:

```java
String jdbcUrl = postgresContainer.getJdbcUrl();
String redisHost = redisContainer.getHost();
Integer redisPort = redisContainer.getMappedPort(6379);
```

### Slow Test Startup

First-time container image pulls can be slow. Subsequent runs are fast due to:
- Container reuse (`.withReuse(true)`)
- Docker image caching
- Spring Boot test context caching

## CI/CD Integration

### GitHub Actions

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Integration Tests
        run: ./gradlew test --tests "*Integration*"
```

### GitLab CI

```yaml
test:
  services:
    - docker:dind
  script:
    - ./gradlew test --tests "*Integration*"
```

## References

- [TestContainers Documentation](https://www.testcontainers.org/)
- [Spring Boot TestContainers](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers)
- [TradeMaster Testing Standards](../../../../../../../CLAUDE.md#testing-standards-critical)
