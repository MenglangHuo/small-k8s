# ==============================================================================
# Makefile for Spring WebFlux Ticket Microservice
# ==============================================================================

SHELL := /bin/bash
.DEFAULT_GOAL := help

# Colors for terminal output
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RESET := \033[0m

##@ General Commands

.PHONY: help
help: ## Show this help menu
	@echo -e "$(CYAN)Ticket Management Microservice - Make Commands$(RESET)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "Usage: make $(GREEN)<target>$(RESET)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-18s$(RESET) %s\n", $$1, $$2 } /^##@/ { printf "\n$(YELLOW)%s$(RESET)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development & Local Execution

.PHONY: run
run: ## Run Spring Boot application locally with active .env profile
	@echo -e "$(GREEN)==> Running Spring Boot application locally on port 8000...$(RESET)"
	@set -a; [ -f .env ] && . .env; set +a; ./gradlew bootRun --no-daemon

.PHONY: test
test: ## Run automated unit and integration tests
	@echo -e "$(GREEN)==> Running JUnit 5 tests with Reactor StepVerifier...$(RESET)"
	./gradlew test --info

.PHONY: build
build: ## Compile application and package production JAR
	@echo -e "$(GREEN)==> Compiling and building bootJar...$(RESET)"
	./gradlew clean bootJar -x test

.PHONY: clean
clean: ## Clean Gradle build artifacts
	@echo -e "$(YELLOW)==> Cleaning build directories...$(RESET)"
	./gradlew clean

##@ Docker & Containers

.PHONY: docker-build
docker-build: ## Build optimized multi-stage Docker image
	@echo -e "$(GREEN)==> Executing ./scripts/build.sh...$(RESET)"
	@chmod +x scripts/build.sh
	./scripts/build.sh

.PHONY: docker-up
docker-up: ## Start MongoDB and Ticket Service in background
	@echo -e "$(GREEN)==> Starting Docker Compose services...$(RESET)"
	docker compose up -d

.PHONY: docker-down
docker-down: ## Stop all Docker Compose services
	@echo -e "$(YELLOW)==> Stopping Docker Compose services...$(RESET)"
	docker compose down

.PHONY: docker-logs
docker-logs: ## Follow live container logs
	docker compose logs -f ticket-app

.PHONY: docker-db-gui
docker-db-gui: ## Start Mongo Express UI on http://localhost:8081
	@echo -e "$(GREEN)==> Starting Mongo Express GUI on http://localhost:8081...$(RESET)"
	docker compose --profile debug up -d mongo-express

##@ CI/CD & Production Operations

.PHONY: deploy
deploy: ## Run safe automated zero-downtime deployment with health check
	@echo -e "$(GREEN)==> Executing ./scripts/deploy.sh...$(RESET)"
	@chmod +x scripts/deploy.sh scripts/rollback.sh
	./scripts/deploy.sh

.PHONY: rollback
rollback: ## Rollback container to the previous stable release
	@echo -e "$(YELLOW)==> Executing ./scripts/rollback.sh...$(RESET)"
	@chmod +x scripts/rollback.sh
	./scripts/rollback.sh

.PHONY: health
health: ## Check microservice health status
	@curl -s -f http://localhost:8000/actuator/health | jq . || curl -s http://localhost:8000/actuator/health
