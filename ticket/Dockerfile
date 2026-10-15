# ==========================================
# Multi-Stage Dockerfile for Spring WebFlux Microservice
# ==========================================

# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and configuration files first for layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build bootJar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Extract layers for Spring Boot layered JAR optimization
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Phnom_Penh /etc/localtime && \
    echo "Asia/Phnom_Penh" > /etc/timezone

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy extracted layers from builder
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

# Expose microservice HTTP port
EXPOSE 8000

# Container healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8000/actuator/health || exit 1

# Launch Spring Boot with container-aware memory settings
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Phnom_Penh", \
    "org.springframework.boot.loader.launch.JarLauncher"]
