# ==============================================================================
# Multi-Stage Dockerfile for Equi-Extin Spring Boot Backend
# Target Platform: AWS App Runner / Amazon ECS / Docker
# Base Image: Eclipse Temurin OpenJDK 21 (LTS)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application with Maven
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# 1. Copy Maven Wrapper and POM file first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# 2. Ensure executable permissions on Maven Wrapper
RUN chmod +x mvnw

# 3. Download dependencies in a separate layer to speed up subsequent builds
RUN ./mvnw dependency:go-offline -B

# 4. Copy source code and build the final production JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Production Runtime with Minimal JRE
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# 1. Security best practice: Create a dedicated non-root system user and group
RUN addgroup --system --gid 1001 springgroup && \
    adduser --system --uid 1001 --ingroup springgroup springuser

# 2. Copy the compiled JAR artifact from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# 3. Change ownership to the non-root user
RUN chown -R springuser:springgroup /app

# 4. Switch to non-root user
USER springuser:springgroup

# 5. Default port for Spring Boot (Can be overridden dynamically by AWS App Runner via $PORT)
ENV PORT=8090
EXPOSE 8090 8080

# 6. Container-optimized JVM entrypoint
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
