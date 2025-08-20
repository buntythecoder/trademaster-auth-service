#!/bin/bash
echo "========================================"
echo "TradeMaster Frontend - Quick Start"
echo "========================================"
echo

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found!"
    echo
    echo "Please install Node.js from: https://nodejs.org/"
    echo "Then restart your terminal and try again."
    exit 1
fi

echo "✅ Node.js found:"
node --version
echo

# Install dependencies and run
echo "Installing dependencies and starting Storybook..."
echo "This will take 2-3 minutes the first time..."
echo

npm install && npm run storybook

if [ $? -ne 0 ]; then
    echo
    echo "❌ Something went wrong. Try these steps:"
    echo
    echo "1. npm cache clean --force"
    echo "2. rm -rf node_modules"
    echo "3. rm package-lock.json"
    echo "4. npm install"
    echo "5. npm run storybook"
    echo
fi