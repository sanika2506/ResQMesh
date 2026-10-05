# ==============================================================================
# Stage 1: Build Environment
# Uses a full Maven + JDK 17 image to compile and package the project.
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source files and web assets
COPY src ./src

# Compile application into JAR and copy compile-time dependencies to target/dependency
RUN mvn clean package -DskipTests
RUN mvn dependency:copy-dependencies -DincludeScope=compile

# ==============================================================================
# Stage 2: Production Runtime Environment
# Uses a lean JRE (Java Runtime Environment) without Maven or build tools.
# ==============================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Configure default port
ENV PORT=8080

# Copy application JAR and libraries from builder stage
COPY --from=builder /app/target/resqmesh-simulator-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /app/target/dependency/ ./lib/

# Expose web server port
EXPOSE 8080

# Start ResQMesh Web Server
CMD ["java", "-cp", "app.jar:lib/*", "com.resqmesh.web.ResQMeshWebServer"]
