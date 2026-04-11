# Build stage
FROM gradle:9.4.1-jdk21-alpine AS builder
WORKDIR /app

# Copy module-specific settings and db module first (dependency)
COPY db/ /app/db/
COPY core/ /app/core/

# Build core module (skipping tests)
RUN gradle -p /app/core bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/core/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
