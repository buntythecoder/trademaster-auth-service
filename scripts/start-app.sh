#!/bin/bash
echo "======================================="
echo "TradeMaster React App - Direct Testing"
echo "======================================="
echo

# Kill any existing processes on port 5173
echo "Cleaning up port 5173..."
lsof -ti:5173 | xargs kill -9 2>/dev/null || true

# Make sure we have the public directory
mkdir -p public
touch public/favicon.ico

echo "Installing dependencies..."
npm install --silent

echo
echo "Starting React App with Vite..."
echo
echo "✅ React App will open at: http://localhost:5173"
echo "📱 Registration Form will be displayed directly"
echo
echo "Press Ctrl+C to stop"
echo

npm run dev