# Eureka Server for TradeMaster Service Discovery
FROM openjdk:24-ea-jdk-slim

# Create app directory
WORKDIR /app

# Copy the application jar from Gradle build directory
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8761

# Run the application with Java 24 preview features
ENTRYPOINT ["java", "--enable-preview", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", "-jar", "app.jar"]