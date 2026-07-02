# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the generated jar from build stage
COPY --from=build /app/target/echill-backend-0.0.1-SNAPSHOT.jar app.jar

# Limit JVM memory for Render's free tier (512MB total). Use SerialGC for low-CPU environments to speed up boot.
ENV JAVA_OPTS="-Xmx350m -XX:+UseSerialGC -noverify"

# Render automatically assigns a port via the PORT environment variable.
ENV SERVER_PORT=${PORT:-8080}
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
