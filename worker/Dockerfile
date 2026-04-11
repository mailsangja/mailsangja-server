# Build stage
FROM gradle:9.4.1-jdk21-alpine AS builder
WORKDIR /app

# Copy root settings and db module first (dependency)
COPY settings.gradle /app/
COPY db/ /app/db/
COPY worker/ /app/worker/

# Build worker module (skipping tests)
RUN gradle :worker:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/worker/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
