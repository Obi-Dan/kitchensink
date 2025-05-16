.PHONY: build run stop logs clean test test-coverage test-report acceptance-test help test-all ui-test open-video-dir quarkus-dev

# Default target
help:
	@echo "Kitchensink Application Makefile (Migrated to Quarkus)"
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
	@echo "Building Kitchensink Quarkus Docker image (./app-migrated/Dockerfile, docker-compose.yml in root)..."
	docker-compose build kitchensink-quarkus

# Run the Quarkus application and MongoDB
run: build
	@echo "Starting Kitchensink Quarkus application and MongoDB..."
	docker-compose up -d kitchensink-quarkus mongodb
	@echo "Quarkus application will be available at http://localhost:8080"
	@echo "UI available at http://localhost:8080/rest/members/ui"
	@echo "MongoDB available at mongodb://localhost:27017"

# Stop the application and MongoDB
stop:
	@echo "Stopping Kitchensink Quarkus application and MongoDB..."
	docker-compose down -v # -v also removes volumes like mongo_data, omit if persistence is desired across stops

# Show Quarkus application logs
logs:
	@echo "Showing Kitchensink Quarkus application logs..."
	docker-compose logs -f kitchensink-quarkus

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources (containers, volumes)..."
	# docker-compose down -v --rmi local # --rmi local can be too aggressive, removes base images sometimes
	@echo "Cleanup complete (manual image pruning might be needed for 'maven' builder images if not using multi-stage for everything)."

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
	docker-compose up -d --build kitchensink-quarkus mongodb # Ensure fresh build and services are up
	@echo "Waiting for Quarkus application to start (checking health endpoint)..."
	@timeout_seconds=120; \
	start_time=$$(date +%s); \
	while ! curl -s -f http://localhost:8080/q/health/live | grep -q UP; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Quarkus application failed to start within $$timeout_seconds seconds."; \
			docker-compose logs kitchensink-quarkus && docker-compose down -v; \
			exit 1; \
		fi; \
		echo "Still waiting for app (http://localhost:8080/q/health/live)... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "Quarkus application started!"
	@echo "Running API acceptance tests from acceptance-tests/ directory..."
	(cd acceptance-tests && ../mvnw verify) # Use root mvnw
	@echo "Stopping application and MongoDB after API acceptance tests..."
	docker-compose down -v
	@echo "API Acceptance tests finished."

# Run UI acceptance tests
ui-test:
	@echo "Cleaning up old UI test videos..."
	rm -rf ui-acceptance-tests/target/videos/*
	@echo "Starting Quarkus application and MongoDB for UI acceptance tests..."
	docker-compose up -d --build kitchensink-quarkus mongodb
	@echo "Waiting for Quarkus application UI to be available..."
	@timeout_seconds=120; \
	start_time=$$(date +%s); \
	while ! curl -s -f http://localhost:8080/rest/members/ui > /dev/null; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Quarkus UI failed to start within $$timeout_seconds seconds."; \
			docker-compose logs kitchensink-quarkus && docker-compose down -v; \
			exit 1; \
		fi; \
		echo "Still waiting for UI (http://localhost:8080/rest/members/ui)... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "Quarkus application UI ready!"
	@echo "Running UI acceptance tests from ui-acceptance-tests/ directory..."
	(cd ui-acceptance-tests && ../mvnw verify) # Use root mvnw
	@echo "Stopping application and MongoDB after UI acceptance tests..."
	docker-compose down -v
	@echo "UI Acceptance tests finished. Videos saved in ui-acceptance-tests/target/videos/"

# Run the Quarkus application in dev mode
quarkus-dev:
	@echo "Starting Quarkus application in dev mode (app-migrated)..."
	(cd app-migrated && ./mvnw quarkus:dev)

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
test-all: test-coverage # API and UI acceptance tests require manual setup or adjustments beyond this script for now
	@echo "Quarkus unit/integration tests with coverage completed."
	@echo "To run API acceptance tests: ensure app is running (e.g., 'make run' or docker-compose up), then 'cd acceptance-tests && ../mvnw verify'"
	@echo "To run UI acceptance tests: ensure app is running, then 'cd ui-acceptance-tests && ../mvnw verify' (Note: UI tests require manual adaptation)" 