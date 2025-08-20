#!/bin/bash
# Comprehensive local setup for TradeMaster Frontend

echo "===================================="
echo "TradeMaster Frontend Local Setup"
echo "===================================="
echo

# Check Node.js
echo "[1/5] Checking Node.js installation..."
if ! command -v node &> /dev/null; then
    echo "❌ ERROR: Node.js is not installed"
    echo
    echo "Please install Node.js 18+ from: https://nodejs.org/"
    echo "Choose the LTS version and restart your terminal after installation."
    echo
    exit 1
else
    echo "✅ Node.js found:"
    node --version
fi

# Check npm
echo
echo "[2/5] Checking npm..."
if ! command -v npm &> /dev/null; then
    echo "❌ ERROR: npm is not available"
    exit 1
else
    echo "✅ npm found:"
    npm --version
fi

# Clean previous installations
echo
echo "[3/5] Cleaning previous installations..."
if [ -d "node_modules" ]; then
    echo "Removing old node_modules..."
    rm -rf node_modules
fi
if [ -f "package-lock.json" ]; then
    echo "Removing old package-lock.json..."
    rm package-lock.json
fi

# Install dependencies
echo
echo "[4/5] Installing dependencies..."
echo "This may take 2-3 minutes..."
npm install
if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to install dependencies"
    echo
    echo "Try running these commands manually:"
    echo "  npm cache clean --force"
    echo "  npm install --verbose"
    echo
    exit 1
fi

# Verify installation
echo
echo "[5/5] Verifying installation..."
if [ -f "node_modules/.bin/vite" ]; then
    echo "✅ Vite installed successfully"
else
    echo "❌ ERROR: Vite not found in node_modules"
    exit 1
fi

if [ -f "node_modules/.bin/storybook" ]; then
    echo "✅ Storybook installed successfully"
else
    echo "❌ ERROR: Storybook not found in node_modules"
    exit 1
fi

echo
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo
echo "You can now run:"
echo
echo "  npm run storybook    - Start Storybook UI"
echo "  npm run dev          - Start Vite dev server"
echo "  npm test             - Run tests"
echo "  npm run build        - Build for production"
echo
echo "Starting Storybook..."
npm run storybook