# Use Eclipse Temurin JDK 21 as base image for building
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build speed)
RUN ./gradlew build -x test --no-daemon

# Use Eclipse Temurin JRE 21 for runtime (smaller image)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -g 1001 -S trademaster && \
    adduser -u 1001 -S trademaster -G trademaster

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R trademaster:trademaster /app

# Switch to non-root user
USER trademaster

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/auth/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for maintainability
LABEL maintainer="TradeMaster Development Team <dev@trademaster.com>"
LABEL version="1.0.0"
LABEL description="TradeMaster Authentication Service"
LABEL org.opencontainers.image.source="https://github.com/trademaster/auth-service"