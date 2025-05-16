# Use an official Playwright for Java image as a parent image
FROM mcr.microsoft.com/playwright/java:v1.43.0-jammy

# Set the working directory in the container
WORKDIR /usr/src/ui-tests

# Copy the Maven project files for the UI tests
# Copy pom.xml first to leverage Docker layer caching for dependencies
COPY ./ui-acceptance-tests/pom.xml ./pom.xml

# Download Maven dependencies
# Using a specific Maven version if needed, but default from image should be fine
# RUN mvn dependency:go-offline -B
# The base Playwright image might not have mvn directly, let's check its setup or install maven.
# Playwright Java images usually come with Java. We might need to install Maven.
USER root
RUN apt-get update && apt-get install -y maven

# Now that pom.xml is copied and Maven is available, download dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the UI acceptance test project source code
COPY ./ui-acceptance-tests/src ./src

# Default command to execute when the container starts (can be overridden)
# This will run all tests using Maven
CMD ["mvn", "test", "-Dapp.url=http://app:8080/rest/app/ui"] 