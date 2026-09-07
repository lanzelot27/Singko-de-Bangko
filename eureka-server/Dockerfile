# -------------------------------------------------------
# Stage 1: Build — compile and package the fat JAR
# -------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# -------------------------------------------------------
# Stage 2: Runtime — lean image, only the JAR
# -------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=build /app/target/eureka-server-1.0.0.jar app.jar

# Eureka Server runs on port 8761
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
