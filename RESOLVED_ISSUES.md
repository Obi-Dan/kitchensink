# Resolved Issues

## WildFly Log Permission Issue

### Problem Description
The Kitchensink application container was crashing during startup due to permission issues with the WildFly log directory. The application failed to start because the WildFly server could not write to its log files.

### Error Message
```
Caused by: java.io.FileNotFoundException: /opt/jboss/wildfly/standalone/log/server.log (Permission denied)
```

### Root Cause Analysis
1. The WildFly Docker image runs as the non-root `jboss` user (uid 1000)
2. When the Docker volume for logs was mounted in the container, it was owned by root
3. The `jboss` user did not have sufficient permissions to write to the log directory
4. The server startup process requires write access to the log directory to function

This is a common issue in containerized environments where file permissions between the host and container don't align properly, especially when working with mounted volumes.

### Solution
The solution was to explicitly set the correct permissions for the log directory in the Dockerfile:

```dockerfile
# Create log directory and set proper permissions
USER root
RUN mkdir -p /opt/jboss/wildfly/standalone/log && \
    chown -R jboss:0 /opt/jboss/wildfly/standalone/log && \
    chmod -R 775 /opt/jboss/wildfly/standalone/log
USER jboss
```

This ensures:
1. The log directory exists inside the container
2. The directory is owned by the `jboss` user 
3. It has the correct permissions (775) to allow writing
4. The container process reverts to the `jboss` user before running the application

### Verification
After implementing the solution:
1. WildFly successfully starts up 
2. Logs are properly written to the mounted volume
3. The application is accessible at http://localhost:8080/kitchensink
4. The administrative console is accessible at http://localhost:9990

### Lessons Learned
When working with containerized applications:
1. Always consider file permissions when mounting volumes
2. Be aware of which user the application runs as inside the container
3. For applications that need to write to directories, ensure the running user has appropriate permissions
4. Directory permissions might need to be set explicitly in the Dockerfile or docker-compose configuration 