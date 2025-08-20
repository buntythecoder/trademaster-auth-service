@echo off
echo ========================================
echo TradeMaster React App - Quick Start
echo ========================================
echo.

REM Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Node.js not found! 
    echo.
    echo Please install Node.js from: https://nodejs.org/
    echo Then restart your command prompt and try again.
    pause
    exit /b 1
)

echo ✅ Node.js found: 
node --version
echo.

REM Install dependencies and run the React app
echo Installing dependencies and starting React App...
echo This will take 2-3 minutes the first time...
echo.

npm install && npm run dev

if errorlevel 1 (
    echo.
    echo ❌ Something went wrong. Try these steps:
    echo.
    echo 1. npm cache clean --force
    echo 2. rmdir /s /q node_modules
    echo 3. del package-lock.json
    echo 4. npm install
    echo 5. npm run dev
    echo.
    pause
)