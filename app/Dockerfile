FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY checkstyle_rules.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Environment variable for Spring Boot profile (e.g., prod, dev)
# ENV SPRING_PROFILES_ACTIVE=prod

CMD ["java", "-jar", "app.jar"] 