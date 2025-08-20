@echo off
echo Fixing Storybook setup...
echo.

REM Kill any process on port 6006 or 6007
echo Checking for processes on ports 6006 and 6007...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :6006') do taskkill /pid %%a /f >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :6007') do taskkill /pid %%a /f >nul 2>&1

REM Create necessary directories
echo Creating directories...
if not exist public mkdir public
if not exist src mkdir src

REM Create a minimal index.css if it doesn't exist
if not exist src\index.css (
    echo Creating minimal CSS file...
    echo @tailwind base; > src\index.css
    echo @tailwind components; >> src\index.css
    echo @tailwind utilities; >> src\index.css
)

REM Ensure we have a favicon
if not exist public\favicon.ico (
    echo. > public\favicon.ico
)

REM Clean and reinstall if needed
if not exist node_modules (
    echo Installing dependencies...
    npm install
) else (
    echo Dependencies already installed
)

echo.
echo Starting Storybook on port 6006...
echo.
echo Direct link to registration form:
echo http://localhost:6006/?path=/story/auth-registrationform--default
echo.

REM Force port 6006
npm run storybook -- --port 6006