@echo off
REM Start TradeMaster Frontend with Storybook for UI validation

echo Starting TradeMaster Frontend Development Environment...
echo.

REM Check if package-lock.json exists, if not generate it
if not exist package-lock.json (
    echo Generating package-lock.json...
    npm install --package-lock-only
)

REM Create network if it doesn't exist
docker network create trademaster-network 2>nul

echo Building and starting frontend container...
docker-compose -f docker-compose.yml -f docker-compose.frontend.yml up --build frontend

echo.
echo Frontend started successfully!
echo.
echo 🎨 Storybook UI: http://localhost:6006
echo ⚡ Vite Dev Server: http://localhost:5173
echo 📱 Mobile Registration Form: http://localhost:6006/?path=/story/auth-registrationform--default
echo.
echo To validate the registration form:
echo 1. Open Storybook at http://localhost:6006
echo 2. Navigate to Auth ^> RegistrationForm
echo 3. Try different stories to test various states
echo 4. Use the "Controls" tab to modify props
echo 5. Test mobile responsiveness with viewport controls
echo.

pause