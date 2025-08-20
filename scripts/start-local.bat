@echo off
REM Start frontend locally without Docker for quick validation

echo Starting TradeMaster Frontend Locally (No Docker)...
echo.

REM Check if Node.js is installed
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js 18+ from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if npm is available
npm --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: npm is not available
    pause
    exit /b 1
)

echo Node.js version:
node --version
echo npm version:
npm --version
echo.

REM Install dependencies if node_modules doesn't exist
if not exist node_modules (
    echo Installing dependencies...
    npm install
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies
        pause
        exit /b 1
    )
)

echo Starting Storybook...
echo.
echo This will open Storybook in your browser at:
echo 🎨 http://localhost:6006
echo.
echo To stop, press Ctrl+C
echo.

REM Start Storybook
npm run storybook