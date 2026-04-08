FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy eureka-server sources from subfolder
COPY eureka-server/pom.xml ./pom.xml
COPY eureka-server/src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

