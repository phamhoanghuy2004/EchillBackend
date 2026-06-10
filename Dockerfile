# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests to speed up the build
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user with UID 1000 as required by Hugging Face Spaces
RUN useradd -m -u 1000 user
RUN chown -R user:user /app
USER user

# Hugging Face Spaces expose port 7860 by default
# Spring Boot will automatically use SERVER_PORT environment variable
ENV SERVER_PORT=7860
EXPOSE 7860

# Copy the built jar file from the build stage
COPY --from=build --chown=user:user /app/target/echill-backend-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
