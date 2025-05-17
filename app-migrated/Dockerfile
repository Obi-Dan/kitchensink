# Stage 1: Build the Quarkus application
# FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS builder
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /project

# Copy only the pom.xml to leverage Docker cache for dependencies
COPY pom.xml .

# Clean and Download dependencies. This layer is rebuilt only if pom.xml changes.
RUN mvn clean dependency:go-offline -B

# Copy the rest of the application source code
COPY src ./src

# Clean and Build the Uber-JAR. 
# Use -DskipTests to avoid running tests during Docker image build if they are run elsewhere.
RUN mvn clean package -Dquarkus.package.type=uber-jar -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /work/

# Copy the Uber-JAR from the builder stage
COPY --from=builder /project/target/*-runner.jar ./application.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# No need for add-user.sh or specific WildFly configurations

ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "application.jar"] 