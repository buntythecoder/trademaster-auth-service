# TradeMaster Spring Cloud Config Server - Multi-stage Docker build
# Stage 1: Build stage with Java 24 and Gradle
FROM openjdk:24-jdk-slim as builder

# Set working directory
WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime stage with Java 24
FROM openjdk:24-jdk-slim

# Metadata
LABEL org.opencontainers.image.title="TradeMaster Config Server"
LABEL org.opencontainers.image.description="Spring Cloud Config Server for TradeMaster Platform"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="TradeMaster Development Team"
LABEL org.opencontainers.image.source="https://github.com/trademaster/platform"

# Create non-root user for security
RUN groupadd -r trademaster && useradd -r -g trademaster -s /bin/false trademaster

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create application directories
RUN mkdir -p /app /var/log/trademaster /opt/trademaster/config \
    && chown -R trademaster:trademaster /app /var/log/trademaster /opt/trademaster

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder --chown=trademaster:trademaster /app/build/libs/*.jar app.jar

# Copy configuration files (optional)
COPY --chown=trademaster:trademaster src/main/resources/application.yml /opt/trademaster/config/

# Switch to non-root user
USER trademaster

# JVM configuration for Java 24
ENV JAVA_OPTS="\
    --enable-preview \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-development} \
    -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus \
    -Dlogging.file.name=/var/log/trademaster/config-server.log"

# Spring Boot configuration
ENV SPRING_PROFILES_ACTIVE=development
ENV SERVER_PORT=8888
ENV EUREKA_URI=http://eureka-server:8761/eureka/

# Expose port
EXPOSE 8888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8888/actuator/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Default command
CMD []