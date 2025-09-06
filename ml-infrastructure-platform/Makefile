# ML Infrastructure Platform Makefile
# Production-Grade MLOps Platform Build and Deployment

.PHONY: help build test docker docker-dev deploy clean install

# Default target
.DEFAULT_GOAL := help

# Variables
PROJECT_NAME := ml-infrastructure-platform
VERSION := 1.0.0
DOCKER_REGISTRY := docker.io/trademaster
KUBERNETES_NAMESPACE := ml-platform
HELM_CHART := infrastructure/helm/ml-platform

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m # No Color

## help: Show this help message
help:
	@echo "ML Infrastructure Platform - Production-Grade MLOps Platform"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

## install: Install project dependencies
install:
	@echo "$(GREEN)Installing project dependencies...$(NC)"
	./gradlew build --refresh-dependencies
	@echo "$(GREEN)Dependencies installed successfully!$(NC)"

## build: Build the application
build:
	@echo "$(GREEN)Building ML Infrastructure Platform...$(NC)"
	./gradlew clean bootJar
	@echo "$(GREEN)Build completed successfully!$(NC)"

## test: Run all tests
test:
	@echo "$(GREEN)Running tests...$(NC)"
	./gradlew test integrationTest
	@echo "$(GREEN)All tests passed!$(NC)"

## test-unit: Run unit tests only
test-unit:
	@echo "$(GREEN)Running unit tests...$(NC)"
	./gradlew test
	@echo "$(GREEN)Unit tests passed!$(NC)"

## test-integration: Run integration tests only
test-integration:
	@echo "$(GREEN)Running integration tests...$(NC)"
	./gradlew integrationTest
	@echo "$(GREEN)Integration tests passed!$(NC)"

## lint: Run code quality checks
lint:
	@echo "$(GREEN)Running code quality checks...$(NC)"
	./gradlew check
	@echo "$(GREEN)Code quality checks passed!$(NC)"

## dev: Start development environment
dev:
	@echo "$(GREEN)Starting development environment...$(NC)"
	docker-compose up --build -d
	@echo "$(YELLOW)Development environment started!$(NC)"
	@echo "$(YELLOW)ML Platform: http://localhost:8080$(NC)"
	@echo "$(YELLOW)MLflow: http://localhost:5000$(NC)"
	@echo "$(YELLOW)Grafana: http://localhost:3000 (admin/admin)$(NC)"
	@echo "$(YELLOW)Prometheus: http://localhost:9090$(NC)"
	@echo "$(YELLOW)MinIO: http://localhost:9001 (minioadmin/minioadmin)$(NC)"

## dev-logs: Show development environment logs
dev-logs:
	@echo "$(GREEN)Showing development environment logs...$(NC)"
	docker-compose logs -f

## dev-stop: Stop development environment
dev-stop:
	@echo "$(GREEN)Stopping development environment...$(NC)"
	docker-compose down
	@echo "$(GREEN)Development environment stopped!$(NC)"

##@ Docker

## docker: Build production Docker image
docker:
	@echo "$(GREEN)Building production Docker image...$(NC)"
	docker build -t $(DOCKER_REGISTRY)/$(PROJECT_NAME):$(VERSION) .
	docker tag $(DOCKER_REGISTRY)/$(PROJECT_NAME):$(VERSION) $(DOCKER_REGISTRY)/$(PROJECT_NAME):latest
	@echo "$(GREEN)Docker image built successfully!$(NC)"

## docker-dev: Build development Docker image
docker-dev:
	@echo "$(GREEN)Building development Docker image...$(NC)"
	docker build --target development -t $(DOCKER_REGISTRY)/$(PROJECT_NAME):dev .
	@echo "$(GREEN)Development Docker image built successfully!$(NC)"

## docker-push: Push Docker image to registry
docker-push: docker
	@echo "$(GREEN)Pushing Docker image to registry...$(NC)"
	docker push $(DOCKER_REGISTRY)/$(PROJECT_NAME):$(VERSION)
	docker push $(DOCKER_REGISTRY)/$(PROJECT_NAME):latest
	@echo "$(GREEN)Docker image pushed successfully!$(NC)"

## docker-scan: Scan Docker image for vulnerabilities
docker-scan: docker
	@echo "$(GREEN)Scanning Docker image for vulnerabilities...$(NC)"
	docker scout cves $(DOCKER_REGISTRY)/$(PROJECT_NAME):$(VERSION) || echo "$(YELLOW)Docker Scout not available, skipping vulnerability scan$(NC)"

##@ Kubernetes

## k8s-namespace: Create Kubernetes namespace
k8s-namespace:
	@echo "$(GREEN)Creating Kubernetes namespace...$(NC)"
	kubectl create namespace $(KUBERNETES_NAMESPACE) --dry-run=client -o yaml | kubectl apply -f -
	@echo "$(GREEN)Namespace $(KUBERNETES_NAMESPACE) ready!$(NC)"

## k8s-secrets: Create Kubernetes secrets
k8s-secrets: k8s-namespace
	@echo "$(GREEN)Creating Kubernetes secrets...$(NC)"
	kubectl create secret generic ml-platform-secrets \
		--from-literal=postgres-password=postgres \
		--from-literal=redis-password="" \
		--from-literal=minio-access-key=minioadmin \
		--from-literal=minio-secret-key=minioadmin \
		--from-literal=jwt-secret=ml-platform-secret-key-change-in-production \
		--namespace=$(KUBERNETES_NAMESPACE) \
		--dry-run=client -o yaml | kubectl apply -f -
	@echo "$(GREEN)Secrets created successfully!$(NC)"

## helm-deps: Install Helm chart dependencies
helm-deps:
	@echo "$(GREEN)Installing Helm chart dependencies...$(NC)"
	cd $(HELM_CHART) && helm dependency update
	@echo "$(GREEN)Helm dependencies installed!$(NC)"

## deploy: Deploy to Kubernetes with Helm
deploy: k8s-namespace k8s-secrets helm-deps
	@echo "$(GREEN)Deploying ML Infrastructure Platform to Kubernetes...$(NC)"
	helm upgrade --install ml-platform $(HELM_CHART) \
		--namespace $(KUBERNETES_NAMESPACE) \
		--set global.namespace=$(KUBERNETES_NAMESPACE) \
		--set mlPlatform.image.tag=$(VERSION) \
		--wait --timeout 600s
	@echo "$(GREEN)Deployment completed successfully!$(NC)"
	@echo "$(YELLOW)Getting service URLs...$(NC)"
	@make k8s-status

## deploy-dev: Deploy development environment to Kubernetes
deploy-dev: k8s-namespace k8s-secrets helm-deps
	@echo "$(GREEN)Deploying development environment to Kubernetes...$(NC)"
	helm upgrade --install ml-platform-dev $(HELM_CHART) \
		--namespace $(KUBERNETES_NAMESPACE) \
		--set global.namespace=$(KUBERNETES_NAMESPACE) \
		--set mlPlatform.image.tag=dev \
		--set mlPlatform.replicaCount=1 \
		--set postgresql.primary.persistence.size=10Gi \
		--set redis.master.persistence.size=5Gi \
		--wait --timeout 600s
	@echo "$(GREEN)Development deployment completed!$(NC)"

## k8s-status: Show Kubernetes deployment status
k8s-status:
	@echo "$(GREEN)Kubernetes Deployment Status:$(NC)"
	@echo "$(YELLOW)Pods:$(NC)"
	kubectl get pods -n $(KUBERNETES_NAMESPACE)
	@echo "$(YELLOW)Services:$(NC)"
	kubectl get services -n $(KUBERNETES_NAMESPACE)
	@echo "$(YELLOW)Ingresses:$(NC)"
	kubectl get ingresses -n $(KUBERNETES_NAMESPACE) || echo "No ingresses found"

## k8s-logs: Show application logs in Kubernetes
k8s-logs:
	@echo "$(GREEN)Showing application logs...$(NC)"
	kubectl logs -f deployment/ml-platform -n $(KUBERNETES_NAMESPACE)

## k8s-describe: Describe ML Platform deployment
k8s-describe:
	@echo "$(GREEN)Describing ML Platform deployment...$(NC)"
	kubectl describe deployment ml-platform -n $(KUBERNETES_NAMESPACE)

## k8s-port-forward: Port forward to access services locally
k8s-port-forward:
	@echo "$(GREEN)Setting up port forwarding...$(NC)"
	@echo "$(YELLOW)ML Platform will be available at http://localhost:8080$(NC)"
	@echo "$(YELLOW)Press Ctrl+C to stop port forwarding$(NC)"
	kubectl port-forward service/ml-platform 8080:8080 -n $(KUBERNETES_NAMESPACE)

##@ Infrastructure

## infra-up: Start all infrastructure services
infra-up:
	@echo "$(GREEN)Starting infrastructure services...$(NC)"
	docker-compose up -d postgres redis kafka minio prometheus grafana elasticsearch kibana
	@echo "$(GREEN)Infrastructure services started!$(NC)"

## infra-down: Stop all infrastructure services
infra-down:
	@echo "$(GREEN)Stopping infrastructure services...$(NC)"
	docker-compose down -v
	@echo "$(GREEN)Infrastructure services stopped!$(NC)"

## monitoring-up: Start monitoring stack
monitoring-up:
	@echo "$(GREEN)Starting monitoring stack...$(NC)"
	docker-compose up -d prometheus grafana
	@echo "$(GREEN)Monitoring stack started!$(NC)"
	@echo "$(YELLOW)Prometheus: http://localhost:9090$(NC)"
	@echo "$(YELLOW)Grafana: http://localhost:3000 (admin/admin)$(NC)"

## logging-up: Start logging stack
logging-up:
	@echo "$(GREEN)Starting logging stack...$(NC)"
	docker-compose up -d elasticsearch kibana
	@echo "$(GREEN)Logging stack started!$(NC)"
	@echo "$(YELLOW)Kibana: http://localhost:5601$(NC)"

##@ Utilities

## clean: Clean build artifacts and Docker resources
clean:
	@echo "$(GREEN)Cleaning build artifacts...$(NC)"
	./gradlew clean
	docker system prune -f
	docker volume prune -f
	@echo "$(GREEN)Cleanup completed!$(NC)"

## clean-all: Clean everything including Docker images
clean-all: clean
	@echo "$(GREEN)Cleaning Docker images...$(NC)"
	docker image prune -a -f
	@echo "$(GREEN)Full cleanup completed!$(NC)"

## format: Format code
format:
	@echo "$(GREEN)Formatting code...$(NC)"
	./gradlew spotlessApply
	@echo "$(GREEN)Code formatted successfully!$(NC)"

## security-scan: Run security scans
security-scan:
	@echo "$(GREEN)Running security scans...$(NC)"
	./gradlew dependencyCheckAnalyze
	@echo "$(GREEN)Security scan completed!$(NC)"

## performance-test: Run performance tests
performance-test:
	@echo "$(GREEN)Running performance tests...$(NC)"
	./gradlew performanceTest
	@echo "$(GREEN)Performance tests completed!$(NC)"

## docs: Generate documentation
docs:
	@echo "$(GREEN)Generating documentation...$(NC)"
	./gradlew javadoc
	@echo "$(GREEN)Documentation generated in build/docs/javadoc/$(NC)"

## backup: Create backup of data volumes
backup:
	@echo "$(GREEN)Creating backup of data volumes...$(NC)"
	mkdir -p backups
	docker run --rm -v ml-infrastructure-platform_postgres-data:/data -v $(PWD)/backups:/backup alpine tar czf /backup/postgres-backup-$(shell date +%Y%m%d-%H%M%S).tar.gz /data
	docker run --rm -v ml-infrastructure-platform_minio-data:/data -v $(PWD)/backups:/backup alpine tar czf /backup/minio-backup-$(shell date +%Y%m%d-%H%M%S).tar.gz /data
	@echo "$(GREEN)Backup created in ./backups/$(NC)"

## migrate: Run database migrations
migrate:
	@echo "$(GREEN)Running database migrations...$(NC)"
	./gradlew flywayMigrate
	@echo "$(GREEN)Database migrations completed!$(NC)"

##@ CI/CD

## ci: Run CI pipeline (build, test, lint, security-scan)
ci: install build test lint security-scan
	@echo "$(GREEN)CI pipeline completed successfully!$(NC)"

## cd: Run CD pipeline (docker, docker-push)
cd: docker docker-push
	@echo "$(GREEN)CD pipeline completed successfully!$(NC)"

## version: Show version information
version:
	@echo "$(GREEN)ML Infrastructure Platform Version Information:$(NC)"
	@echo "Version: $(VERSION)"
	@echo "Project: $(PROJECT_NAME)"
	@echo "Docker Registry: $(DOCKER_REGISTRY)"
	@echo "Kubernetes Namespace: $(KUBERNETES_NAMESPACE)"

## health: Check all service health
health:
	@echo "$(GREEN)Checking service health...$(NC)"
	@curl -s http://localhost:8080/actuator/health || echo "$(RED)ML Platform not available$(NC)"
	@curl -s http://localhost:5000/api/2.0/mlflow/experiments/search || echo "$(RED)MLflow not available$(NC)"
	@curl -s http://localhost:9090/-/healthy || echo "$(RED)Prometheus not available$(NC)"
	@curl -s http://localhost:3000/api/health || echo "$(RED)Grafana not available$(NC)"
	@echo "$(GREEN)Health check completed!$(NC)"

## undeploy: Remove deployment from Kubernetes
undeploy:
	@echo "$(GREEN)Removing ML Platform deployment...$(NC)"
	helm uninstall ml-platform --namespace $(KUBERNETES_NAMESPACE) || echo "Release not found"
	kubectl delete namespace $(KUBERNETES_NAMESPACE) --ignore-not-found=true
	@echo "$(GREEN)Deployment removed successfully!$(NC)"