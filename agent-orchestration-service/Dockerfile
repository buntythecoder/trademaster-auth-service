# TradeMaster Agent OS - Orchestration Service Dockerfile
# Multi-stage build for Java 24 with Virtual Threads

# Stage 1: Build stage
FROM eclipse-temurin:24-jdk-alpine AS builder

LABEL stage=builder
LABEL maintainer="TradeMaster DevOps <devops@trademaster.com>"
LABEL description="Agent OS Orchestration Service Build Stage"

# Install dependencies for building
RUN apk add --no-cache curl

# Set working directory
WORKDIR /build

# Copy Gradle wrapper and build files first (for better caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make Gradle wrapper executable
RUN chmod +x ./gradlew

# Download dependencies (this layer will be cached if build.gradle doesn't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew clean bootJar --no-daemon && \
    mkdir -p build/dependency && \
    (cd build/dependency; jar -xf ../libs/*.jar)

# Stage 2: Runtime stage
FROM eclipse-temurin:24-jre-alpine AS runtime

LABEL maintainer="TradeMaster DevOps <devops@trademaster.com>"
LABEL description="Agent OS Orchestration Service Runtime"
LABEL version="1.0.0"
LABEL component="agent-orchestration"

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    jq \
    tzdata \
    dumb-init && \
    rm -rf /var/cache/apk/*

# Create application user and group
RUN addgroup -g 1000 agentos && \
    adduser -u 1000 -G agentos -s /bin/sh -D agentos

# Set working directory
WORKDIR /app

# Copy application files from builder stage
COPY --from=builder /build/build/dependency/BOOT-INF/lib lib
COPY --from=builder /build/build/dependency/META-INF META-INF
COPY --from=builder /build/build/dependency/BOOT-INF/classes .

# Create logs directory
RUN mkdir -p logs && chown -R agentos:agentos /app

# Switch to non-root user
USER agentos

# Expose application port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=15s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/agent-os/actuator/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication --enable-preview" \
    SPRING_PROFILES_ACTIVE=docker \
    TZ=UTC

# Entry point with dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Start the application with Virtual Threads enabled
CMD ["sh", "-c", "java $JAVA_OPTS -cp /app org.springframework.boot.loader.launch.JarLauncher"]

# Metadata
LABEL org.opencontainers.image.title="TradeMaster Agent OS Orchestration Service"
LABEL org.opencontainers.image.description="AI Agent orchestration and workflow management service"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.created="2024-01-01T00:00:00Z"
LABEL org.opencontainers.image.source="https://github.com/trademaster/agent-os"
LABEL org.opencontainers.image.documentation="https://docs.trademaster.com/agent-os/"