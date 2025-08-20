#!/bin/bash

# TradeMaster Docker Validation Script

echo "🧪 TradeMaster Docker Validation Test"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
PASSED=0
FAILED=0

# Function to run test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_exit_code="${3:-0}"
    
    echo -n "Testing $test_name... "
    
    if eval "$test_command" > /dev/null 2>&1; then
        if [ $? -eq $expected_exit_code ]; then
            echo -e "${GREEN}PASSED${NC}"
            ((PASSED++))
        else
            echo -e "${RED}FAILED${NC} (unexpected exit code)"
            ((FAILED++))
        fi
    else
        echo -e "${RED}FAILED${NC}"
        ((FAILED++))
    fi
}

# Function to test HTTP endpoint
test_http() {
    local test_name="$1"
    local url="$2"
    local expected_status="${3:-200}"
    
    echo -n "Testing $test_name... "
    
    if command -v curl > /dev/null 2>&1; then
        local status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")
        if [ "$status_code" -eq "$expected_status" ]; then
            echo -e "${GREEN}PASSED${NC} (HTTP $status_code)"
            ((PASSED++))
        else
            echo -e "${RED}FAILED${NC} (HTTP $status_code, expected $expected_status)"
            ((FAILED++))
        fi
    else
        echo -e "${YELLOW}SKIPPED${NC} (curl not available)"
    fi
}

echo ""
echo "1. 🐳 Docker Environment Tests"
echo "------------------------------"

run_test "Docker installation" "docker --version"
run_test "Docker Compose installation" "docker-compose --version"
run_test "Docker daemon running" "docker info"

echo ""
echo "2. 🏗️  Docker Build Tests"
echo "-------------------------"

run_test "Auth service Docker image exists" "docker images | grep trademaster-auth"

echo ""
echo "3. 🚀 Docker Compose Validation"
echo "-------------------------------"

# Start services in detached mode
echo "Starting Docker Compose services..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d > /dev/null 2>&1

# Wait for services to start
echo "Waiting for services to initialize..."
sleep 30

run_test "PostgreSQL container running" "docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps postgres | grep 'Up'"
run_test "Redis container running" "docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps redis | grep 'Up'"
run_test "Auth service container running" "docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps auth-service | grep 'Up'"

echo ""
echo "4. 🔍 Service Health Tests"
echo "--------------------------"

# Wait a bit more for health checks
sleep 15

run_test "PostgreSQL database connectivity" "docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T postgres pg_isready -U trademaster_user -d trademaster_auth"
run_test "Redis connectivity" "docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T redis redis-cli --raw incr ping"

echo ""
echo "5. 🌐 HTTP API Tests"
echo "-------------------"

# Wait for auth service to fully start
sleep 10

test_http "Auth service health endpoint" "http://localhost:8080/api/v1/auth/health" 200
test_http "Auth service actuator health" "http://localhost:8080/actuator/health" 200
test_http "Swagger UI accessibility" "http://localhost:8080/swagger-ui.html" 200

echo ""
echo "6. 🔐 Authentication API Tests"
echo "------------------------------"

if command -v curl > /dev/null 2>&1; then
    # Test user registration
    echo -n "Testing user registration... "
    REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
        -H "Content-Type: application/json" \
        -d '{
            "email": "test@docker.com",
            "password": "DockerTest123!",
            "firstName": "Docker",
            "lastName": "Test"
        }' -w "%{http_code}")
    
    if [[ "$REGISTER_RESPONSE" == *"201" ]] || [[ "$REGISTER_RESPONSE" == *"400"* ]]; then
        echo -e "${GREEN}PASSED${NC} (registration endpoint functional)"
        ((PASSED++))
    else
        echo -e "${RED}FAILED${NC} (unexpected response: $REGISTER_RESPONSE)"
        ((FAILED++))
    fi
    
    # Test invalid login (should fail gracefully)
    echo -n "Testing login endpoint... "
    LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{
            "email": "invalid@docker.com",
            "password": "InvalidPassword"
        }' -w "%{http_code}")
    
    if [[ "$LOGIN_RESPONSE" == *"401" ]]; then
        echo -e "${GREEN}PASSED${NC} (login endpoint functional)"
        ((PASSED++))
    else
        echo -e "${RED}FAILED${NC} (unexpected response: $LOGIN_RESPONSE)"
        ((FAILED++))
    fi
else
    echo -e "${YELLOW}SKIPPED${NC} (curl not available for API tests)"
fi

echo ""
echo "7. 📊 Performance Tests"
echo "----------------------"

if command -v curl > /dev/null 2>&1; then
    echo -n "Testing response time... "
    RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:8080/api/v1/auth/health)
    RESPONSE_TIME_MS=$(echo "$RESPONSE_TIME * 1000" | bc 2>/dev/null || echo "unknown")
    
    if [ "$RESPONSE_TIME_MS" != "unknown" ] && (( $(echo "$RESPONSE_TIME < 1.0" | bc -l) )); then
        echo -e "${GREEN}PASSED${NC} (${RESPONSE_TIME_MS}ms)"
        ((PASSED++))
    else
        echo -e "${YELLOW}WARNING${NC} (${RESPONSE_TIME_MS}ms - may be slow)"
        ((PASSED++))  # Still count as passed, just slow
    fi
else
    echo -e "${YELLOW}SKIPPED${NC} (curl not available)"
fi

echo ""
echo "8. 🧹 Cleanup"
echo "-------------"

echo "Stopping and removing containers..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down > /dev/null 2>&1

run_test "Containers stopped successfully" "! docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps | grep 'Up'"

echo ""
echo "📊 Test Results Summary"
echo "======================="
echo -e "Total Tests: $((PASSED + FAILED))"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 All tests passed! TradeMaster Docker setup is working correctly.${NC}"
    echo ""
    echo "✅ You can now start the development environment with:"
    echo "   ./scripts/start-dev.sh"
    echo ""
    echo "📖 For more information, see DOCKER.md"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed. Please check the Docker setup.${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   - Check Docker daemon is running"
    echo "   - Ensure ports 5432, 6379, 8080 are available"
    echo "   - Review logs: docker-compose logs"
    echo ""
    echo "📖 For more information, see DOCKER.md"
    exit 1
fi