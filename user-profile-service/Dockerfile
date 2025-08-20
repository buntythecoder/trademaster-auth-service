# TradeMaster User Profile Service Dockerfile
# Multi-stage build for optimal image size and security

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency resolution
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 trademaster && \
    adduser -D -s /bin/sh -u 1001 -G trademaster trademaster

# Install necessary packages
RUN apk add --no-cache curl wget

# Create required directories
RUN mkdir -p /app/logs /app/uploads && \
    chown -R trademaster:trademaster /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership of the JAR file
RUN chown trademaster:trademaster app.jar

# Switch to non-root user
USER trademaster

# Expose port
EXPOSE 8082

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=docker

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/api/v1/profiles/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]