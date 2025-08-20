#!/bin/bash
echo "======================================="
echo "TradeMaster Frontend - Simple Start"
echo "======================================="
echo

# Kill any existing processes on common ports
echo "Cleaning up ports..."
lsof -ti:6006 | xargs kill -9 2>/dev/null || true
lsof -ti:6007 | xargs kill -9 2>/dev/null || true

# Make sure we have the public directory
mkdir -p public
touch public/favicon.ico

echo "Installing dependencies..."
npm install --silent

echo
echo "Starting Storybook..."
echo
echo "✅ Storybook will open at: http://localhost:6006"
echo "📱 Registration Form: http://localhost:6006/?path=/story/auth-registrationform--default"
echo
echo "Press Ctrl+C to stop"
echo

npx storybook dev -p 6006 --no-open