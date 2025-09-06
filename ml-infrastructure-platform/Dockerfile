# ML Infrastructure Platform Dockerfile
# Multi-stage build for optimized production image

# Stage 1: Build stage with Java 24 and Gradle
FROM openjdk:24-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build application
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime stage with Java 24
FROM openjdk:24-jdk-slim AS runtime

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    netcat-traditional \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r mlplatform && useradd -r -g mlplatform mlplatform

# Set working directory
WORKDIR /app

# Copy JAR file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p /var/log/ml-platform && chown -R mlplatform:mlplatform /var/log/ml-platform

# Create data directories
RUN mkdir -p /app/data /app/models /app/artifacts && \
    chown -R mlplatform:mlplatform /app

# Switch to non-root user
USER mlplatform

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for Java 24 Virtual Threads
ENV JAVA_OPTS="\
    --enable-preview \
    -Dspring.threads.virtual.enabled=true \
    -XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4 \
    -XX:+EnableJVMCI \
    -XX:+UseJVMCICompiler \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production"

# Environment variables
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=production
ENV SPRING_THREADS_VIRTUAL_ENABLED=true
ENV LOGGING_FILE_NAME=/var/log/ml-platform/application.log

# Labels
LABEL maintainer="TradeMaster ML Team <ml-team@trademaster.com>" \
      version="1.0.0" \
      description="Production-Grade ML Infrastructure Platform" \
      org.opencontainers.image.title="ML Infrastructure Platform" \
      org.opencontainers.image.description="Enterprise MLOps platform with experiment tracking, feature store, and model serving" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="TradeMaster" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.source="https://github.com/trademaster/ml-infrastructure-platform"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Default command
CMD []

---

# Development Dockerfile
FROM openjdk:24-jdk-slim AS development

# Install development tools
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    netcat-traditional \
    procps \
    vim \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy application files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Expose ports (application + debug)
EXPOSE 8080 5005

# Development JVM options with debugging
ENV JAVA_OPTS_DEV="\
    --enable-preview \
    -Dspring.threads.virtual.enabled=true \
    -Xdebug \
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005 \
    -XX:+UseG1GC \
    -Dspring.profiles.active=dev"

# Environment variables for development
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_THREADS_VIRTUAL_ENABLED=true

# Entry point for development
ENTRYPOINT ["sh", "-c", "./gradlew bootRun --args='--spring.profiles.active=dev'"]

---

# Testing Dockerfile
FROM openjdk:24-jdk-slim AS testing

# Install testing tools
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    netcat-traditional \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy application files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Testing JVM options
ENV JAVA_OPTS_TEST="\
    --enable-preview \
    -Dspring.threads.virtual.enabled=true \
    -XX:+UseG1GC \
    -Dspring.profiles.active=test"

# Environment variables for testing
ENV SPRING_PROFILES_ACTIVE=test
ENV SPRING_THREADS_VIRTUAL_ENABLED=true

# Entry point for testing
ENTRYPOINT ["sh", "-c", "./gradlew test integrationTest --no-daemon"]

# Default command
CMD []