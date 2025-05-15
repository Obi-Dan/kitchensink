.PHONY: build run stop logs clean test test-coverage test-report acceptance-test help test-all

# Default target
help:
	@echo "Kitchensink Application Makefile"
	@echo ""
	@echo "Usage:"
	@echo "  make build           - Build the Docker image for the application"
	@echo "  make run             - Run the application (builds if not built yet)"
	@echo "  make stop            - Stop the running application"
	@echo "  make logs            - Show application logs"
	@echo "  make clean           - Remove containers, volumes, and images for the application"
	@echo "  make test            - Run application unit tests (includes style check)"
	@echo "  make test-coverage   - Run application unit tests with code coverage (includes style check)"
	@echo "  make test-report     - Open the application's coverage report in a browser"
	@echo "  make acceptance-test - Start app, run acceptance tests, then stop app"
	@echo "  make test-all        - Run all tests (unit, coverage, and acceptance tests)"
	@echo "  make help            - Show this help message"
	@echo ""

# Build the Docker image
build:
	@echo "Building Kitchensink Docker image (Dockerfile in app/, docker-compose.yml in root)..."
	docker-compose build

# Run the application
run:
	@echo "Starting Kitchensink application (Dockerfile in app/, docker-compose.yml in root)..."
	docker-compose up -d
	@echo "Application will be available at http://localhost:8080/kitchensink"
	@echo "WildFly Management Console available at http://localhost:9990"
	@echo "Management Console credentials: admin / Admin#70365"

# Stop the application
stop:
	@echo "Stopping Kitchensink application (docker-compose.yml in root)..."
	docker-compose down

# Show application logs
logs:
	@echo "Showing Kitchensink application logs (docker-compose.yml in root)..."
	docker-compose logs -f wildfly

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources (docker-compose.yml in root)..."
	docker-compose down -v --rmi local
	@echo "Cleanup complete."

# Run unit tests for the main application
test:
	@echo "Running application tests in app/ directory..."
	(cd app && \
		echo "Validating application code style..." && \
		mvn checkstyle:check && \
		echo "Running application unit tests..." && \
		mvn dependency:resolve clean test)

# Run unit tests with code coverage for the main application
test-coverage:
	@echo "Running application tests with coverage in app/ directory..."
	(cd app && \
		echo "Validating application code style..." && \
		mvn checkstyle:check && \
		echo "Running application unit tests with JaCoCo code coverage..." && \
		mvn dependency:resolve clean test jacoco:report)

# Open the coverage report in a browser (OS dependent)
# Note: This now expects the report to be in app/target/
test-report:
	@echo "Opening coverage report from app/target/site/jacoco/index.html..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open app/target/site/jacoco/index.html; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open app/target/site/jacoco/index.html; \
	else \
		echo "Please open app/target/site/jacoco/index.html in your browser"; \
	fi

# Run acceptance tests
acceptance-test:
	@echo "Starting application for acceptance tests (docker-compose.yml in root)..."
	docker-compose up -d
	@echo "Waiting for application to start..."
	@timeout_seconds=120; \
	start_time=$$(date +%s); \
	while ! curl -s -f http://localhost:8080/kitchensink/rest/members > /dev/null; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Application failed to start within $$timeout_seconds seconds."; \
			docker-compose logs wildfly && docker-compose down; \
			exit 1; \
		fi; \
		echo "Still waiting for app (http://localhost:8080/kitchensink/rest/members)... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "Application started!"
	@echo "Running acceptance tests from acceptance-tests/ directory..."
	cd acceptance-tests && mvn test
	@echo "Stopping application after acceptance tests (docker-compose.yml in root)..."
	docker-compose down
	@echo "Acceptance tests finished."

# Run all tests
test-all: test-coverage acceptance-test
	@echo "All tests (unit, coverage, acceptance) completed." 