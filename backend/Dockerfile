# Use an official Maven image with OpenJDK 17 to build the application
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and other necessary files for dependency resolution
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN mvn clean package

# Use an official OpenJDK 17 runtime image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Install SQLite3 for the database
RUN apt-get update && apt-get install -y sqlite3

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy the SQLite database file
COPY src/main/resources/database.db /app/src/main/resources/database.db

# Expose the necessary ports
EXPOSE 8080 9090

# Run the application
CMD ["java", "-jar", "/app/app.jar"]
