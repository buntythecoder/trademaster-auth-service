#!/bin/bash
# Start frontend locally without Docker for quick validation

echo "Starting TradeMaster Frontend Locally (No Docker)..."
echo

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed or not in PATH"
    echo "Please install Node.js 18+ from https://nodejs.org/"
    exit 1
fi

# Check if npm is available
if ! command -v npm &> /dev/null; then
    echo "ERROR: npm is not available"
    exit 1
fi

echo "Node.js version:"
node --version
echo "npm version:"
npm --version
echo

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to install dependencies"
        exit 1
    fi
fi

echo "Starting Storybook..."
echo
echo "This will open Storybook in your browser at:"
echo "🎨 http://localhost:6006"
echo
echo "To stop, press Ctrl+C"
echo

# Start Storybook
npm run storybook