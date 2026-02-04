# Build Stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and source
COPY pom.xml .
COPY src src

# Build
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 9090

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
