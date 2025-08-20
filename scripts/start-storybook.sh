#!/bin/bash
echo "Starting TradeMaster Storybook..."
echo

# Create public directory if it doesn't exist
mkdir -p public

# Create a simple favicon if it doesn't exist
if [ ! -f public/favicon.ico ]; then
    echo "Creating favicon..."
    touch public/favicon.ico
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d node_modules ]; then
    echo "Installing dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo "Failed to install dependencies"
        exit 1
    fi
fi

echo "Starting Storybook on http://localhost:6006"
echo
echo "Registration Form will be available at:"
echo "http://localhost:6006/?path=/story/auth-registrationform--default"
echo
echo "Press Ctrl+C to stop"
echo

npm run storybook