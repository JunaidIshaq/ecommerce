FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy shared common-lib (because it's inside the context ".")
COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml install -DskipTests

# Copy search-service sources from subfolder
COPY search-service/pom.xml ./pom.xml
COPY search-service/src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

