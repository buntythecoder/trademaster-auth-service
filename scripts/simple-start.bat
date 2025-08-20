@echo off
echo =======================================
echo TradeMaster Frontend - Simple Start
echo =======================================
echo.

REM Kill any existing processes on common ports
echo Cleaning up ports...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :6006') do taskkill /pid %%a /f >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :6007') do taskkill /pid %%a /f >nul 2>&1

REM Make sure we have the public directory
if not exist public mkdir public
if not exist public\favicon.ico echo. > public\favicon.ico

echo Installing dependencies...
npm install --silent

echo.
echo Starting Storybook...
echo.
echo ✅ Storybook will open at: http://localhost:6006
echo 📱 Registration Form: http://localhost:6006/?path=/story/auth-registrationform--default
echo.
echo Press Ctrl+C to stop
echo.

npx storybook dev -p 6006 --no-open