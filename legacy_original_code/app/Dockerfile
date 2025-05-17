FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app
COPY . .
RUN mvn clean package

FROM quay.io/wildfly/wildfly:28.0.1.Final-jdk11

# Copy the built war file to the deployments directory
COPY --from=builder /app/target/kitchensink.war /opt/jboss/wildfly/standalone/deployments/

# Expose web and management ports
EXPOSE 8080
EXPOSE 9990

# Add admin user for management console access
RUN /opt/jboss/wildfly/bin/add-user.sh admin Admin#70365 --silent

# Create log directory and set proper permissions
USER root
RUN mkdir -p /opt/jboss/wildfly/standalone/log && \
    chown -R jboss:0 /opt/jboss/wildfly/standalone/log && \
    chmod -R 775 /opt/jboss/wildfly/standalone/log
USER jboss

# Run with the standalone-full.xml configuration to ensure all required EE features are available
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"] 