.PHONY: build run stop logs clean test test-coverage test-report help

# Default target
help:
	@echo "Kitchensink Application Makefile"
	@echo ""
	@echo "Usage:"
	@echo "  make build          - Build the Docker image"
	@echo "  make run            - Run the application (builds if not built yet)"
	@echo "  make stop           - Stop the running application"
	@echo "  make logs           - Show application logs"
	@echo "  make clean          - Remove containers, volumes, and images"
	@echo "  make test           - Run unit tests"
	@echo "  make test-coverage  - Run unit tests with code coverage"
	@echo "  make test-report    - Open the coverage report in a browser"
	@echo "  make help           - Show this help message"
	@echo ""

# Build the Docker image
build:
	@echo "Building Kitchensink Docker image..."
	docker-compose build

# Run the application
run:
	@echo "Starting Kitchensink application..."
	docker-compose up -d
	@echo "Application will be available at http://localhost:8080/kitchensink"
	@echo "WildFly Management Console available at http://localhost:9990"
	@echo "Management Console credentials: admin / Admin#70365"

# Stop the application
stop:
	@echo "Stopping Kitchensink application..."
	docker-compose down

# Show application logs
logs:
	@echo "Showing Kitchensink application logs..."
	docker-compose logs -f wildfly

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources..."
	docker-compose down -v --rmi local
	@echo "Cleanup complete."

# Run unit tests
test:
	@echo "Validating code style..."
	mvn checkstyle:check
	@echo "Running unit tests..."
	mvn dependency:resolve clean test

# Run unit tests with code coverage
test-coverage:
	@echo "Validating code style..."
	mvn checkstyle:check
	@echo "Running unit tests with JaCoCo code coverage..."
	mvn dependency:resolve clean test jacoco:report

# Open the coverage report in a browser (OS dependent)
test-report:
	@echo "Opening coverage report..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open target/site/jacoco/index.html; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open target/site/jacoco/index.html; \
	else \
		echo "Please open target/site/jacoco/index.html in your browser"; \
	fi 