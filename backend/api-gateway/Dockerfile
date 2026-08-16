FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy shared common-lib (because it's inside the context ".")
COPY pom.xml .
COPY toolchains.xml /root/.m2/toolchains.xml
RUN mvn install -N -DskipTests
COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml install -DskipTests

# Copy product-service sources from subfolder
COPY api-gateway/pom.xml ./pom.xml
COPY api-gateway/src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/api-gateway-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
