# ==========================================
# Multi-Stage Dockerfile for Spring WebFlux Microservice
# ==========================================

# Stage 1: Build JAR
FROM eclipse-temurin:25-jdk-alpine AS builder
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
RUN java -Djarmode=tools -jar build/libs/*.jar extract --launcher --layers dependencies,spring-boot-loader,snapshot-dependencies,application --destination extracted

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Phnom_Penh /etc/localtime && \
    echo "Asia/Phnom_Penh" > /etc/timezone

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy extracted layers from builder
COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./

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
