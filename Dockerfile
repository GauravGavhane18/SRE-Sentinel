# Stage 1: Build the Maven application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the production SRE runner container
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Install Docker CLI (docker.io) and process utilities (procps)
RUN apt-get update && apt-get install -y \
    docker.io \
    procps \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy configuration and setup runtime paths
COPY config/sentinel-config.json ./config/sentinel-config.json
COPY --from=build /app/target/sre-sentinel-1.0-SNAPSHOT.jar ./sentinel.jar

# Create logs directory for SRE persistent incidents logging
RUN mkdir -p logs

# Set environment parameters for proper ANSI color support in console dashboard
ENV TERM=xterm-256color

# Run Sentinel daemon
ENTRYPOINT ["java", "-jar", "sentinel.jar"]
