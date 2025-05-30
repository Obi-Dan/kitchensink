.PHONY: build run stop logs clean test test-coverage test-report acceptance-test help test-all ui-test open-video-dir purge-mongo-data start-clean format launch

# Variables
MVN := mvn
MAVEN_OPTS := $(MAVEN_OPTS)

# Default target
help:
	@echo "Kitchensink Application Makefile"
	@echo ""
	@echo "Usage:"
	@echo "  make build                - Build the Docker image for the application"
	@echo "  make run                  - Run the application (builds if not built yet)"
	@echo "  make start                - Build and run the application"
	@echo "  make stop                 - Stop the running application"
	@echo "  make logs                 - Show application logs"
	@echo "  make clean                - Remove containers, volumes, and images for the application"
	@echo "  make purge-mongo-data     - Purge existing MongoDB data volume"
	@echo "  make start-clean          - Purge MongoDB data and then start the application"
	@echo "  make test                 - Run application unit tests (includes style check)"
	@echo "  make format               - Run the auto-formatter (Spotless) on the application code"
	@echo "  make test-coverage        - Run application unit tests with code coverage (includes style check)"
	@echo "  make test-report          - Open the application's coverage report in a browser"
	@echo "  make acceptance-test      - Start app, run acceptance tests, then stop app"
	@echo "  make ui-test              - Start app, run UI acceptance tests (with video), then stop app"
	@echo "  make test-all             - Run all tests (unit, coverage, acceptance, UI)"
	@echo "  make open-video-dir       - Open the UI test video recording directory"
	@echo "  make help                 - Show this help message"
	@echo "  make launch               - Run 'start-clean' then open the application UI in a browser"
	@echo ""

# Build the Docker image
build: format
	@echo "Building Kitchensink Docker image (Dockerfile in app/, docker-compose.yml in root)..."
	docker-compose build

# Run the application
run: format
	@echo "Starting Kitchensink application (Dockerfile in app/, docker-compose.yml in root)..."
	docker-compose up -d
	@echo "Application will be available at http://localhost:8080/rest/app/ui"

# Start the application
start:
	@echo "Starting Kitchensink application..."
	$(MAKE) build
	$(MAKE) run

# Stop the application
stop:
	@echo "Stopping Kitchensink application (docker-compose.yml in root)..."
	docker-compose down

# Show application logs
logs:
	@echo "Showing Kitchensink application logs (docker-compose.yml in root)..."
	docker-compose logs -f app

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources (docker-compose.yml in root)..."
	docker-compose down -v --rmi local --remove-orphans
	@echo "Cleanup complete."

# Purge MongoDB data
purge-mongo-data:
	@echo "Stopping mongo service (if running)..."
	docker-compose stop mongo || true
	@echo "Removing mongo service container (if it exists)..."
	docker-compose rm -f -s mongo || true
	@echo "Purging MongoDB data volume ($(shell basename $(CURDIR))_mongodb_data)..."
	docker volume rm $(shell basename $(CURDIR))_mongodb_data || echo "INFO: Volume $(shell basename $(CURDIR))_mongodb_data not found or already removed."
	@echo "MongoDB data volume purge attempt finished."

# Start application with a clean slate of data
start-clean: purge-mongo-data start
	@echo "Application started with a clean slate of MongoDB data."

# Run unit tests for the main application
test: format
	@echo "Running application tests in app/ directory..."
	(cd app && \
		$(MVN) clean test $(MAVEN_OPTS))

# Run unit tests with code coverage for the main application
test-coverage: format
	@echo "Running application tests with coverage in app/ directory (using quarkus-jacoco extension)..."
	(cd app && \
		$(MVN) clean verify $(MAVEN_OPTS))

# Open the coverage report in a browser (OS dependent)
# Note: This now expects the report to be in app/target/jacoco-report/
test-report: test-coverage
	@echo "Attempting to open coverage report from app/target/jacoco-report/index.html..."
	@if [ -f app/target/jacoco-report/index.html ]; then \
		if [ "$(shell uname)" = "Darwin" ]; then \
			open app/target/jacoco-report/index.html; \
		elif [ "$(shell uname)" = "Linux" ]; then \
			xdg-open app/target/jacoco-report/index.html; \
		else \
			echo "Coverage report is available at app/target/jacoco-report/index.html. Please open it in your browser."; \
		fi; \
	else \
		echo "ERROR: Coverage report not found at app/target/jacoco-report/index.html. Ensure 'make test-coverage' completed successfully."; \
		false; \
	fi

# Run acceptance tests
acceptance-test: format
	@echo "Starting MIGRATED application for acceptance tests (docker-compose.yml in root)..."
	docker-compose rm -s -f mongo || true # Ensure mongo is gone, ignore error if not found
	touch app/pom.xml # Bust cache for POM
	touch app/src/main/java/org/jboss/as/quickstarts/kitchensink/model/Member.java # Bust cache for SRC
	docker-compose build --no-cache \
		--build-arg BUILDKIT_INLINE_CACHE=1 \
		--build-arg MAVEN_ARGS="$(MAVEN_OPTS)" \
		app mongo
	docker-compose up -d app # This will also start mongo due to depends_on
	@echo "Waiting for MIGRATED application to start (using healthcheck and additional sleep)..."
	@sleep 20 # Increased sleep slightly more
	@echo "Initial logs from app container:"
	docker-compose logs --tail="100" app
	@echo "Running acceptance tests from acceptance-tests/ directory (against migrated app)..."
	@(cd acceptance-tests && mvn test -Dapp.base.url=http://localhost:8080/rest/app/api) || \
		(echo "Acceptance tests FAILED. Displaying app logs:" && docker-compose logs --tail="500" app && docker-compose down -v && exit 1)
	@echo "Acceptance tests PASSED."
	@echo "Stopping application after acceptance tests (docker-compose.yml in root)..."
	docker-compose down -v
	@echo "Acceptance tests finished."

# Run UI acceptance tests
ui-test:
	@echo "Cleaning up old UI test videos..."
	rm -rf ui-acceptance-tests/target/videos/*
	@echo "Building services if necessary (docker-compose.yml in root)..."
	touch app/pom.xml # Bust cache for POM
	touch app/src/main/java/org/jboss/as/quickstarts/kitchensink/model/Member.java # Bust cache for SRC
	docker-compose build --no-cache \
		--build-arg BUILDKIT_INLINE_CACHE=1 \
		--build-arg MAVEN_ARGS="$(MAVEN_OPTS)" \
		app ui-tests  # Mongo removed from explicit build target
	@echo "Starting application service for UI acceptance tests (docker-compose.yml in root)..."
	docker-compose up -d app # App service explicitly started (mongo will follow due to depends_on)
	@echo "Waiting for app service to be healthy (UI tests depend on app)..."
	@timeout_seconds=180; \
	start_time=$$(date +%s); \
	while ! docker-compose ps app | grep -q 'healthy'; do \
		current_time=$$(date +%s); \
		elapsed_time=$$((current_time - start_time)); \
		if [ $$elapsed_time -ge $$timeout_seconds ]; then \
			echo "Application service (app) failed to become healthy within $$timeout_seconds seconds."; \
			docker-compose logs app; \
			docker-compose down -v; \
			exit 1; \
		fi; \
		echo "Still waiting for app service to be healthy... $$elapsed_time/$$timeout_seconds s"; \
		sleep 5; \
	done
	@echo "App service is healthy. Showing initial app logs:"
	docker-compose logs --tail="100" app
	@echo "Running UI acceptance tests in a container (service: ui-tests)..."
	@(docker-compose run --rm ui-tests) || \
		(echo "UI tests FAILED. Displaying full app logs:" && docker-compose logs --tail="500" app && echo "Displaying full ui-tests container logs:" && docker-compose logs ui-tests && docker-compose down -v && exit 1)
	@echo "UI tests PASSED or completed."
	@echo "Stopping application services after UI acceptance tests (docker-compose.yml in root)..."
	docker-compose down -v 
	@echo "UI Acceptance tests finished. Videos and reports should be in ui-acceptance-tests/target/"

# Open the UI test video directory
open-video-dir:
	@echo "Opening UI test video directory: ui-acceptance-tests/target/videos/ ..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open ui-acceptance-tests/target/videos/; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open ui-acceptance-tests/target/videos/; \
	else \
		echo "Please open ui-acceptance-tests/target/videos/ in your file browser."; \
	fi

# Run all tests
test-all: test-coverage acceptance-test ui-test
	@echo "All tests (unit, coverage, acceptance, UI) completed."

# Add the new format target
format:
	@echo "Running auto-formatter (Spotless) on application code in app/ directory..."
	(cd app && \
		$(MVN) spotless:apply $(MAVEN_OPTS))
	@echo "Auto-formatting attempt finished."

# Launch the application: start-clean then open UI
launch:
	$(MAKE) start-clean
	@echo "Waiting a few seconds for the application to be fully up..."
	@sleep 15 # Adjust sleep time as needed
	@echo "Attempting to open application UI at http://localhost:8080/rest/app/ui..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open http://localhost:8080/rest/app/ui; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open http://localhost:8080/rest/app/ui; \
	else \
		echo "Please open http://localhost:8080/rest/app/ui in your browser."; \
	fi 