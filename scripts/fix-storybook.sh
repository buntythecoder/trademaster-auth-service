#!/bin/bash
echo "Fixing Storybook setup..."
echo

# Kill any process on port 6006 or 6007
echo "Checking for processes on ports 6006 and 6007..."
lsof -ti:6006 | xargs kill -9 2>/dev/null || true
lsof -ti:6007 | xargs kill -9 2>/dev/null || true

# Create necessary directories
echo "Creating directories..."
mkdir -p public src

# Create a minimal index.css if it doesn't exist
if [ ! -f src/index.css ]; then
    echo "Creating minimal CSS file..."
    cat > src/index.css << 'EOF'
@tailwind base;
@tailwind components;
@tailwind utilities;
EOF
fi

# Ensure we have a favicon
if [ ! -f public/favicon.ico ]; then
    touch public/favicon.ico
fi

# Clean and reinstall if needed
if [ ! -d node_modules ]; then
    echo "Installing dependencies..."
    npm install
else
    echo "Dependencies already installed"
fi

echo
echo "Starting Storybook on port 6006..."
echo
echo "Direct link to registration form:"
echo "http://localhost:6006/?path=/story/auth-registrationform--default"
echo

# Force port 6006
npm run storybook -- --port 6006