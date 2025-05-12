# Running Kitchensink in Docker with WildFly

This guide explains how to containerize and run the Kitchensink application using Docker and WildFly.

## Prerequisites

- Docker installed on your system
- Docker Compose installed on your system
- Make (optional, for using the Makefile)

## Using the Makefile (Recommended)

A Makefile is provided for easier management of the application:

```bash
# Build the Docker image
make build

# Run the application
make run

# View application logs
make logs

# Stop the application
make stop

# Clean up all resources
make clean

# Show all available commands
make help
```

## Manual Building and Running

If you prefer not to use the Makefile, you can use Docker Compose directly:

1. Build and start the containers:

```bash
docker-compose up -d
```

2. The application will be available at:
   - Application URL: http://localhost:8080/kitchensink
   - WildFly Management Console: http://localhost:9990 (login with admin/Admin#70365)

3. To shut down the application:

```bash
docker-compose down
```

## Configuration Details

- The application uses a bridged network with the subnet 172.28.0.0/16
- Container logs are persisted using a Docker volume
- WildFly is configured with a default administrative user
- Health checks are configured to verify the application is running properly

## Troubleshooting

- If the container fails to start, check the logs:

```bash
docker-compose logs jboss-eap
# or
make logs
```

- To access the container's shell:

```bash
docker exec -it kitchensink-app /bin/bash
```

- To check deployed applications in WildFly:

```bash
docker exec -it kitchensink-app /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="deployment-info"
``` 