# Authentication Service Performance Benchmarks

This package contains JMH (Java Microbenchmark Harness) performance benchmarks for the TradeMaster Authentication Service.

## Overview

The benchmarks validate critical authentication paths and Virtual Thread performance to ensure production-ready performance targets.

## Benchmark Classes

### 1. AuthenticationBenchmark
**Purpose**: Core authentication operations performance validation
**Target Performance**:
- Password hashing: <50ms
- Password validation: <10ms
- JWT token generation: <20ms
- JWT token validation: <10ms
- Complete authentication: <100ms
- Virtual Thread authentication: <150ms
- Session validation: <5ms
- Token refresh: <30ms

### 2. VirtualThreadPerformanceBenchmark
**Purpose**: Virtual Thread performance comparison and validation
**Features**:
- Virtual Thread vs Platform Thread I/O performance
- Thread creation overhead comparison
- Concurrent authentication simulation
- Structured concurrency performance
- Memory usage comparison

### 3. CriticalPathBenchmark
**Purpose**: Critical path performance for financial trading system
**Target Performance**:
- Login validation: <20ms
- Token generation: <10ms
- Session validation: <5ms
- Authorization check: <3ms
- MFA validation: <15ms
- Complete authentication flow: <50ms
- Async authentication flow: <30ms
- Financial calculations: <2ms

## Running Benchmarks

### Run All Benchmarks
```bash
./gradlew jmh
```

### Run Specific Benchmark Class
```bash
./gradlew jmh -Pjmh.include=".*AuthenticationBenchmark.*"
```

### Run Specific Benchmark Method
```bash
./gradlew jmh -Pjmh.include=".*benchmarkPasswordHashing.*"
```

### Custom JMH Parameters
```bash
./gradlew jmh -Pjmh.include=".*VirtualThread.*" -Pjmh.fork=1 -Pjmh.wi=2 -Pjmh.i=3
```

## Benchmark Configuration

### JMH Settings
- **Forks**: 2 (separate JVM processes for accuracy)
- **Warmup Iterations**: 3 (prepare JIT compiler)
- **Measurement Iterations**: 5 (actual performance measurement)
- **Time Unit**: Milliseconds (ms)
- **Benchmark Mode**: Average time per operation

### Performance Targets

#### Authentication Operations
| Operation | Target Time | Critical Path |
|-----------|-------------|---------------|
| Password Hashing | <50ms | Login |
| Password Validation | <10ms | Login |
| JWT Generation | <20ms | Login/Token Refresh |
| JWT Validation | <10ms | Every Request |
| Session Validation | <5ms | Every Request |
| Complete Auth Flow | <100ms | Login |

#### Virtual Thread Performance
| Scenario | Target | Comparison |
|----------|--------|------------|
| I/O Bound Tasks | 60%+ improvement | vs Platform Threads |
| Thread Creation | 90%+ faster | vs Platform Threads |
| Memory Usage | 50%+ less | vs Platform Threads |
| Concurrent Auth | 1000+ req/sec | High throughput |

#### Financial System Requirements
| Operation | Target | Importance |
|-----------|--------|------------|
| Authorization Check | <3ms | Critical |
| Financial Calculations | <2ms | Critical |
| MFA Validation | <15ms | Security |
| Async Auth Flow | <30ms | User Experience |

## Performance Analysis

### Interpreting Results
1. **Score**: Average time per operation (lower is better)
2. **Error**: Confidence interval (smaller is better)
3. **Units**: Milliseconds per operation
4. **Samples**: Number of measurement iterations

### Performance Monitoring
- Results saved to: `build/reports/jmh/results.json`
- Use JMH Visualizer for graphical analysis
- Set up CI/CD performance regression alerts

### Troubleshooting Poor Performance

#### High Authentication Latency
- Check database connection pooling
- Verify password hashing configuration
- Monitor JVM garbage collection

#### Virtual Thread Issues
- Ensure `spring.threads.virtual.enabled=true`
- Check for blocking operations
- Verify Virtual Thread executor usage

#### Memory Issues
- Monitor heap usage during benchmarks
- Check for object creation in hot paths
- Verify caching configuration

## Integration with CI/CD

### Performance Gates
```bash
# Add to CI pipeline
./gradlew jmh
# Parse results and fail if performance degrades >10%
```

### Monitoring Integration
- Export JMH results to monitoring systems
- Set up alerts for performance regressions
- Track performance trends over time

## Best Practices

### Benchmark Design
1. **Warm-up JVM**: Adequate warmup iterations
2. **Isolate Operations**: Benchmark single operations
3. **Use Blackhole**: Prevent JIT optimizations
4. **Realistic Data**: Use representative test data

### Performance Optimization
1. **Measure First**: Always benchmark before optimizing
2. **Profile Hot Paths**: Focus on critical code paths
3. **Virtual Threads**: Use for I/O bound operations
4. **Caching**: Cache expensive operations

## Production Performance Validation

### Pre-deployment Checks
- All benchmarks pass performance targets
- No performance regressions >5%
- Virtual Thread performance validated
- Financial calculation accuracy verified

### Monitoring in Production
- Real-time authentication latency monitoring
- Virtual Thread performance metrics
- Financial operation performance tracking
- User experience impact measurement

## Dependencies

### JMH Framework
```gradle
testImplementation 'org.openjdk.jmh:jmh-core:1.37'
testAnnotationProcessor 'org.openjdk.jmh:jmh-generator-annprocess:1.37'
```

### Java 24 Virtual Threads
```gradle
compileOptions {
    jvmArgs += ['--enable-preview']
}
```

## Documentation References

- [JMH Documentation](https://openjdk.java.net/projects/code-tools/jmh/)
- [Java 24 Virtual Threads](https://openjdk.java.net/jeps/444)
- [Spring Boot Performance](https://spring.io/guides/gs/testing-web/)
- [TradeMaster Performance Standards](../../../../../../../standards/performance-requirements.md)

## Contact

For questions about benchmarks or performance issues:
- Performance Team: performance@trademaster.com
- Architecture Team: architecture@trademaster.com
- DevOps Team: devops@trademaster.com