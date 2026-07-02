# ==========================================================
# Stage 1: Build the application (Maven)
# ==========================================================
# This stage compiles the Spring Boot application and produces
# an executable JAR file.
#
# We use a full Maven image with JDK 21 to ensure compatibility
# with the project's Java version.
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory inside the container
WORKDIR /app

# Copy Maven configuration first (leverages Docker cache)
COPY pom.xml .

# Copy application source code
COPY src ./src

# Build the application and skip tests for faster deployment
RUN mvn clean package -DskipTests


# ==========================================================
# Stage 2: Runtime environment (lightweight)
# ==========================================================
# This stage creates a minimal image containing only the
# compiled application and a lightweight JRE.
#
# This significantly reduces image size and improves startup time.
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Default port used by Spring Boot (can be overridden by Render)
ENV PORT=8080

# Expose application port
EXPOSE 8080

# Start the application
# - Uses Render's $PORT environment variable if provided
# - Falls back to 8080 locally
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]