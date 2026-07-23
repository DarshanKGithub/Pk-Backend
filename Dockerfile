# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=60.0", \
  "-XX:MaxDirectMemorySize=32m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Xss512k", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
