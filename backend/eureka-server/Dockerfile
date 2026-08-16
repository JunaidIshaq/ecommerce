FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom and install it
COPY pom.xml .
COPY toolchains.xml /root/.m2/toolchains.xml
RUN mvn install -N -DskipTests

# Copy eureka-server sources from subfolder
COPY eureka-server/pom.xml ./pom.xml
COPY eureka-server/src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/eureka-server-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

