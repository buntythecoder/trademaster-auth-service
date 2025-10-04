# Use Eclipse Temurin JDK 21 as base image for building
# TODO: Upgrade to Java 24 when official images available - MANDATORY Rule #1
FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.docker.gradle build.gradle
COPY settings.gradle ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src ./src

# Build the application with Java 24 preview features enabled - MANDATORY Rule #1
RUN ./gradlew bootJar -x test --no-daemon

# Use Eclipse Temurin JRE 21 for runtime
# TODO: Upgrade to Java 24 when official images available - MANDATORY Rule #1
FROM eclipse-temurin:21-jre AS runtime

# Create non-root user for security
RUN groupadd -g 1001 trademaster && \
    useradd -u 1001 -g trademaster -M -s /bin/false trademaster

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

# Health check - Kong compatible endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v2/health || exit 1

# JVM optimization for containers with Virtual Threads - Java 21 compatible
# TODO: Upgrade to Java 24 when official images available - MANDATORY Rule #1
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 --enable-preview"

# Run the application with Virtual Threads enabled - MANDATORY Rule #1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for maintainability
LABEL maintainer="TradeMaster Development Team <dev@trademaster.com>"
LABEL version="1.0.0"
LABEL description="TradeMaster Authentication Service"
LABEL org.opencontainers.image.source="https://github.com/trademaster/auth-service"