@echo off
REM Comprehensive local setup for TradeMaster Frontend

echo ====================================
echo TradeMaster Frontend Local Setup
echo ====================================
echo.

REM Check Node.js
echo [1/5] Checking Node.js installation...
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Node.js is not installed
    echo.
    echo Please install Node.js 18+ from: https://nodejs.org/
    echo Choose the LTS version and restart your command prompt after installation.
    echo.
    pause
    exit /b 1
) else (
    echo ✅ Node.js found:
    node --version
)

REM Check npm
echo.
echo [2/5] Checking npm...
npm --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: npm is not available
    pause
    exit /b 1
) else (
    echo ✅ npm found:
    npm --version
)

REM Clean previous installations
echo.
echo [3/5] Cleaning previous installations...
if exist node_modules (
    echo Removing old node_modules...
    rmdir /s /q node_modules
)
if exist package-lock.json (
    echo Removing old package-lock.json...
    del package-lock.json
)

REM Install dependencies
echo.
echo [4/5] Installing dependencies...
echo This may take 2-3 minutes...
npm install
if errorlevel 1 (
    echo ❌ ERROR: Failed to install dependencies
    echo.
    echo Try running these commands manually:
    echo   npm cache clean --force
    echo   npm install --verbose
    echo.
    pause
    exit /b 1
)

REM Verify installation
echo.
echo [5/5] Verifying installation...
if exist node_modules\.bin\vite.cmd (
    echo ✅ Vite installed successfully
) else (
    echo ❌ ERROR: Vite not found in node_modules
    pause
    exit /b 1
)

if exist node_modules\.bin\storybook.cmd (
    echo ✅ Storybook installed successfully
) else (
    echo ❌ ERROR: Storybook not found in node_modules
    pause
    exit /b 1
)

echo.
echo ==========================================
echo ✅ Setup Complete! 
echo ==========================================
echo.
echo You can now run:
echo.
echo   npm run storybook    - Start Storybook UI
echo   npm run dev          - Start Vite dev server
echo   npm test             - Run tests
echo   npm run build        - Build for production
echo.
echo To start Storybook now, press any key...
pause >nul

echo Starting Storybook...
npm run storybook