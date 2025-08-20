@echo off
echo ========================================
echo TradeMaster React App - Verbose Start
echo ========================================
echo.

REM Check Node.js
echo [1/4] Checking Node.js...
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Node.js not found! 
    echo Please install from: https://nodejs.org/
    pause
    exit /b 1
) else (
    echo ✅ Node.js found: 
    node --version
)

REM Check npm
echo.
echo [2/4] Checking npm...
npm --version >nul 2>&1
if errorlevel 1 (
    echo ❌ npm not found!
    pause
    exit /b 1
) else (
    echo ✅ npm found: 
    npm --version
)

REM Install dependencies
echo.
echo [3/4] Installing dependencies...
echo This may take a few minutes...
npm install
if errorlevel 1 (
    echo ❌ Failed to install dependencies
    pause
    exit /b 1
) else (
    echo ✅ Dependencies installed successfully
)

REM Start the React app
echo.
echo [4/4] Starting React App...
echo.
echo ✅ React App will start at: http://localhost:5173
echo 📱 Registration Form will be displayed directly
echo 🎯 You can test all the features you requested
echo.
echo Starting Vite development server...
echo.

npm run dev

echo.
echo App stopped.
pause