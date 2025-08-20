@echo off
echo Starting TradeMaster Storybook...
echo.

REM Create public directory if it doesn't exist
if not exist public mkdir public

REM Create a simple favicon if it doesn't exist
if not exist public\favicon.ico (
    echo Creating favicon...
    echo. > public\favicon.ico
)

REM Install dependencies if node_modules doesn't exist
if not exist node_modules (
    echo Installing dependencies...
    npm install
    if errorlevel 1 (
        echo Failed to install dependencies
        pause
        exit /b 1
    )
)

echo Starting Storybook on http://localhost:6006
echo.
echo Registration Form will be available at:
echo http://localhost:6006/?path=/story/auth-registrationform--default
echo.
echo Press Ctrl+C to stop
echo.

npm run storybook