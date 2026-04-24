# Stage 1: Build the application using a Maven image
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies (this layer will be cached)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image using a lightweight JRE
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the fat JAR from the builder stage
# The jar name format usually matches <artifactId>-<version>.jar from your pom.xml
COPY --from=builder /app/target/PPR_Uebung2-1.0.0.jar app.jar

# Expose any ports your application uses (e.g., 8080 if it's a web server)
# EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]