@echo off
echo =======================================
echo TradeMaster React App - Direct Testing
echo =======================================
echo.

REM Kill any existing processes on port 5173
echo Cleaning up port 5173...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173') do taskkill /pid %%a /f >nul 2>&1

REM Make sure we have the public directory
if not exist public mkdir public
if not exist public\favicon.ico echo. > public\favicon.ico

echo Installing dependencies...
npm install --silent

echo.
echo Starting React App with Vite...
echo.
echo ✅ React App will open at: http://localhost:5173
echo 📱 Registration Form will be displayed directly
echo.
echo Press Ctrl+C to stop
echo.

npm run dev