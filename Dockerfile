# ==========================================
# Stage 1: Build the application using Maven
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy dependency descriptors first to leverage Docker layer caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy application source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal JRE 17 Runtime Image
# ==========================================
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Create a non-root system user for security
RUN groupadd --system library && useradd --system --gid library library

# Copy the built artifact from the builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

RUN chown -R library:library /app
USER library

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

