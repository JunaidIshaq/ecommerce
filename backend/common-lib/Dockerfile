FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jdk
