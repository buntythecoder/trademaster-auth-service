#!/bin/bash
# Simple TradeMaster Frontend startup with fallback approach

echo "Starting TradeMaster Frontend (Simple Mode)..."
echo

# Generate package-lock.json if it doesn't exist
if [ ! -f package-lock.json ]; then
    echo "Generating package-lock.json..."
    npm install --package-lock-only --silent
fi

echo "Building and starting frontend container (simple mode)..."
docker-compose -f docker-compose.frontend-simple.yml up --build

echo
echo "Frontend started successfully!"
echo
echo "🎨 Storybook UI: http://localhost:6006"
echo "⚡ Vite Dev Server: http://localhost:5173"
echo
echo "If you see any issues, try:"
echo "1. docker-compose -f docker-compose.frontend-simple.yml down -v"
echo "2. docker system prune -f"
echo "3. Run this script again"
echo