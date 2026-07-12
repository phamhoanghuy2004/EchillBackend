# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# 🚀 Tối ưu Build: Tải trước dependencies để cache layer, các lần build sau sẽ cực nhanh
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the generated jar from build stage
COPY --from=build /app/target/echill-backend-0.0.1-SNAPSHOT.jar app.jar

# 🚀 Tối ưu Run (VPS): 
# - Dùng MaxRAMPercentage=75.0 để Java tự động nhận diện RAM của Container thay vì fix cứng 350MB.
# - Bỏ UseSerialGC và noverify vì VPS thường có dư CPU để chạy G1GC (mặc định của Java 21) cho hiệu suất cao hơn.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError"

ENV SERVER_PORT=${PORT:-8080}
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
