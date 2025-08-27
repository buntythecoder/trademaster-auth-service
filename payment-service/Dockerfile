# Use OpenJDK 24 with virtual threads support
FROM openjdk:24-jre-slim

# Set working directory
WORKDIR /app

# Install required packages for production
RUN apt-get update && apt-get install -y \
    curl \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# Copy the JAR file
COPY build/libs/payment-service-1.0.0.jar app.jar

# Create non-root user for security
RUN groupadd -r paymentservice && useradd -r -g paymentservice paymentservice
RUN chown -R paymentservice:paymentservice /app
USER paymentservice

# Expose the port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8085/api/v1/payments/health || exit 1

# JVM configuration for production
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -Dspring.threads.virtual.enabled=true"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS --enable-preview -jar app.jar"]