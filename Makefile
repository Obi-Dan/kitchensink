.PHONY: build run stop logs clean test test-coverage test-report acceptance-test help test-all ui-test open-video-dir

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
	@echo "  make ui-test         - Start app, run UI acceptance tests (with video), then stop app"
	@echo "  make test-all        - Run all tests (unit, coverage, acceptance, UI)"
	@echo "  make open-video-dir  - Open the UI test video recording directory"
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
	docker-compose logs -f app

# Clean up resources
clean: stop
	@echo "Cleaning up Docker resources (docker-compose.yml in root)..."
	docker-compose down -v --rmi local --remove-orphans
	@echo "Cleanup complete."

# Run unit tests for the main application
test:
	@echo "Running application tests in app-migrated/ directory..."
	(cd app-migrated && \
		echo "Validating application code style (Quarkus)..." && \
		mvn spotless:check && \
		echo "Running application unit tests (Quarkus)..." && \
		mvn test)

# Run unit tests with code coverage for the main application
test-coverage:
	@echo "Running application tests with coverage in app-migrated/ directory..."
	(cd app-migrated && \
		echo "Validating application code style (Quarkus)..." && \
		mvn spotless:check && \
		echo "Running application unit tests with JaCoCo code coverage (Quarkus)..." && \
		mvn test -Pcoverage)

# Open the coverage report in a browser (OS dependent)
# Note: This now expects the report to be in app-migrated/target/
test-report:
	@echo "Opening coverage report from app-migrated/target/site/jacoco/index.html..."
	@if [ "$(shell uname)" = "Darwin" ]; then \
		open app-migrated/target/site/jacoco/index.html; \
	elif [ "$(shell uname)" = "Linux" ]; then \
		xdg-open app-migrated/target/site/jacoco/index.html; \
	else \
		echo "Please open app-migrated/target/site/jacoco/index.html in your browser"; \
	fi

# Run acceptance tests
acceptance-test:
	@echo "Starting MIGRATED application for acceptance tests (docker-compose.yml in root)..."
	docker-compose rm -s -f mongo || true # Ensure mongo is gone, ignore error if not found
	touch app-migrated/pom.xml # Bust cache for POM
	touch app-migrated/src/main/java/org/jboss/as/quickstarts/kitchensink/model/Member.java # Bust cache for SRC
	docker-compose build app mongo # Ensure migrated app and mongo are built
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
	touch app-migrated/pom.xml # Bust cache for POM
	touch app-migrated/src/main/java/org/jboss/as/quickstarts/kitchensink/model/Member.java # Bust cache for SRC
	docker-compose build app ui-tests mongo # build all three
	@echo "Starting application services for UI acceptance tests (docker-compose.yml in root)..."
	docker-compose up -d app mongo # Start app and mongo
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