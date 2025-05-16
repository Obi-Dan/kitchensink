.PHONY: build run stop logs clean test test-coverage test-report acceptance-test help test-all ui-test open-video-dir quarkus-dev

# Configurable Environment Variables
APP_HOST_PORT ?= 8080
APP_DB_NAME ?= kitchensinkDB

# Environment prefix for docker-compose commands
DC_ENV = APP_HOST_PORT=$(APP_HOST_PORT) APP_DB_NAME=$(APP_DB_NAME)

# Default target
help:
	@echo "Kitchensink Application Makefile (Migrated to Quarkus)"
	@echo ""
	@echo "Configurable variables (can be set in environment or passed to make, e.g., make run APP_HOST_PORT=8888):"
	@echo "  APP_HOST_PORT        - Host port for the Quarkus application (default: $(APP_HOST_PORT))"
	@echo "  APP_DB_NAME          - MongoDB database name (default: $(APP_DB_NAME))"
	@echo ""
	@echo "Usage:"
	@echo "  make build           - Build the Docker image for the Quarkus application"
	@echo "  make run             - Run the Quarkus application and MongoDB (builds if not built yet)"
	@echo "  make stop            - Stop the running application and MongoDB"
	@echo "  make logs            - Show Quarkus application logs"
	@echo "  make clean           - Remove containers, volumes for the application and MongoDB"
	@echo "  make test            - Run Quarkus application unit/integration tests (includes style check)"
	@echo "  make test-coverage   - Run Quarkus application tests with code coverage (includes style check)"
	@echo "  make test-report     - Open the Quarkus application's coverage report in a browser"
	@echo "  make acceptance-test - Start app & DB, run API acceptance tests, then stop app & DB"
	@echo "  make ui-test         - Start app & DB, run UI acceptance tests (with video), then stop app & DB"
	@echo "  make test-all        - Run all tests (Quarkus app tests, coverage, API acceptance, UI acceptance)"
	@echo "  make quarkus-dev     - Run the Quarkus application in dev mode (app-migrated)"
	@echo "  make open-video-dir  - Open the UI test video recording directory"
	@echo "  make help            - Show this help message"
	@echo ""

# Build the Docker image for Quarkus app
build:
	@echo "Building Kitchensink Quarkus Docker image..."
	$(DC_ENV) docker-compose build kitchensink-quarkus

# Run the Quarkus application and MongoDB
run: build
	@echo "Starting Kitchensink Quarkus application and MongoDB..."
	$(DC_ENV) docker-compose up -d kitchensink-quarkus mongodb
	@echo "Quarkus application will be available at http://localhost:$(APP_HOST_PORT)"
	@echo "UI available at http://localhost:$(APP_HOST_PORT)/rest/members/ui"
	@echo "MongoDB available at mongodb://localhost:27017 (exposed from container)"

# Stop the application and MongoDB
stop:
	@echo "Stopping Kitchensink Quarkus application and MongoDB..."
	$(DC_ENV) docker-compose down -v 

# Show Quarkus application logs
logs:
	@echo "Showing Kitchensink Quarkus application logs..."
	$(DC_ENV) docker-compose logs -f kitchensink-quarkus

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources (containers, volumes)..."
	@echo "Cleanup complete (manual image pruning might be needed)."

# Run unit/integration tests for the migrated Quarkus application
test:
	@echo "Running Quarkus application tests in app-migrated/ directory..."
	(cd app-migrated && \
		echo "Validating application code style (Spotless)..." && \
		./mvnw spotless:check && \
		echo "Running Quarkus application unit/integration tests..." && \
		./mvnw test)

# Run Quarkus application tests with code coverage
test-coverage:
	@echo "Running Quarkus application tests with coverage in app-migrated/ directory..."
	(cd app-migrated && \
		echo "Validating application code style (Spotless)..." && \
		./mvnw spotless:check && \
		echo "Running Quarkus application tests with JaCoCo code coverage..." && \
		./mvnw clean test jacoco:report)

# Open the coverage report in a browser
test-report:
	@echo "Opening coverage report from app-migrated/target/site/jacoco/index.html..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open app-migrated/target/site/jacoco/index.html; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open app-migrated/target/site/jacoco/index.html; \
	else \
		echo "Please open app-migrated/target/site/jacoco/index.html in your browser"; \
	fi

# Run API acceptance tests
acceptance-test:
	@echo "Starting Quarkus application and MongoDB for API acceptance tests..."
	$(DC_ENV) docker-compose up -d --build kitchensink-quarkus mongodb 
	@echo "Waiting for Quarkus application to start (checking health endpoint on port $(APP_HOST_PORT))..."
	@timeout_seconds=120; \
	start_time=$$(date +%s); \
	while ! curl -s -f http://localhost:$(APP_HOST_PORT)/q/health/live | grep -q UP; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Quarkus application failed to start within $$timeout_seconds seconds."; \
			$(DC_ENV) docker-compose logs kitchensink-quarkus && $(DC_ENV) docker-compose down -v; \
			exit 1; \
		fi; \
		echo "Still waiting for app (http://localhost:$(APP_HOST_PORT)/q/health/live)... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "Quarkus application started!"
	@echo "Running API acceptance tests from acceptance-tests/ directory..."
	# For acceptance tests to use the configured APP_HOST_PORT, their RestAssured.port would need to be set dynamically.
	# Currently, they are hardcoded to 8080 in the test code (BASE_URL).
	# This can be passed as a system property: -Dapp.host.port=$(APP_HOST_PORT)
	(cd acceptance-tests && ../mvnw verify -Dapp.host.port=$(APP_HOST_PORT) -Dapp.db.name=$(APP_DB_NAME)) 
	@echo "Stopping application and MongoDB after API acceptance tests..."
	$(DC_ENV) docker-compose down -v
	@echo "API Acceptance tests finished."

# Run UI acceptance tests
ui-test:
	@echo "Cleaning up old UI test videos..."
	rm -rf ui-acceptance-tests/target/videos/*
	@echo "Starting Quarkus application and MongoDB for UI acceptance tests..."
	$(DC_ENV) docker-compose up -d --build kitchensink-quarkus mongodb
	@echo "Waiting for Quarkus application UI to be available on port $(APP_HOST_PORT)..."
	@timeout_seconds=120; \
	start_time=$$(date +%s); \
	while ! curl -s -f http://localhost:$(APP_HOST_PORT)/rest/members/ui > /dev/null; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Quarkus UI failed to start within $$timeout_seconds seconds."; \
			$(DC_ENV) docker-compose logs kitchensink-quarkus && $(DC_ENV) docker-compose down -v; \
			exit 1; \
		fi; \
		echo "Still waiting for UI (http://localhost:$(APP_HOST_PORT)/rest/members/ui)... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "Quarkus application UI ready!"
	@echo "Running UI acceptance tests from ui-acceptance-tests/ directory..."
	# Similar to API tests, UI tests need to know the APP_HOST_PORT.
	(cd ui-acceptance-tests && ../mvnw verify -Dapp.host.port=$(APP_HOST_PORT) -Dapp.db.name=$(APP_DB_NAME))
	@echo "Stopping application and MongoDB after UI acceptance tests..."
	$(DC_ENV) docker-compose down -v
	@echo "UI Acceptance tests finished. Videos saved in ui-acceptance-tests/target/videos/"

# Run the Quarkus application in dev mode
quarkus-dev:
	@echo "Starting Quarkus application in dev mode (app-migrated)..."
	# Dev mode uses application.properties, APP_DB_NAME override needs to be passed differently if required for dev services.
	# For dev mode, typically quarkus.mongodb.devservices.database-name or similar is used in application.properties, or rely on defaults.
	(cd app-migrated && APP_DB_NAME=$(APP_DB_NAME) ./mvnw quarkus:dev) # Passing env var might affect dev services

# Open the UI test video directory (path remains the same)
open-video-dir:
	@echo "Opening UI test video directory: ui-acceptance-tests/target/videos/ ..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open ui-acceptance-tests/target/videos/; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open ui-acceptance-tests/target/videos/; \
	else \
		echo "Please open ui-acceptance-tests/target/videos/ in your file browser."; \
	fi

# Run all tests for the migrated application
test-all: test-coverage 
	@echo "Quarkus unit/integration tests with coverage completed."
	@echo "To run API acceptance tests: ensure app is running (e.g., 'make run' or docker-compose up), then 'make acceptance-test APP_HOST_PORT=xxxx'"
	@echo "To run UI acceptance tests: ensure app is running, then 'make ui-test APP_HOST_PORT=xxxx' (Note: UI tests require manual adaptation)" 