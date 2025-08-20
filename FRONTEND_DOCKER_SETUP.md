# 🎨 TradeMaster Frontend Docker Setup

Docker configuration to visually validate the mobile registration form with Storybook.

## 🚀 Quick Start (Recommended)

### Option 1: Simple Mode (Recommended for first-time setup)

**Windows:**
```cmd
cd trademaster
scripts\start-frontend-simple.bat
```

**Linux/Mac:**
```bash
cd trademaster
chmod +x scripts/start-frontend-simple.sh
./scripts/start-frontend-simple.sh
```

### Option 2: Full Integration Mode

**Windows:**
```cmd
cd trademaster
scripts\start-frontend.bat
```

**Linux/Mac:**
```bash
cd trademaster
chmod +x scripts/start-frontend.sh
./scripts/start-frontend.sh
```

**After startup, access:**
- 🎨 **Storybook UI**: http://localhost:6006
- ⚡ **Vite Dev Server**: http://localhost:5173

## 📱 How to Validate the Registration Form

### 1. **Open Storybook** 
Navigate to: http://localhost:6006

### 2. **Find the Registration Form**
- In the sidebar: `Auth` → `RegistrationForm`
- Or direct link: http://localhost:6006/?path=/story/auth-registrationform--default

### 3. **Test Different Stories**
Try these stories to validate features:

#### **Core Functionality**
- `Default` - Empty form state
- `ReadyToSubmit` - Valid form ready for submission
- `WithValidationErrors` - Error states and messages

#### **Feature-Specific Tests**
- `PhoneNumberFormatting` - Indian +91 formatting
- `PasswordStrengthDemo` - Real-time password strength
- `MobileView` - Mobile-first responsive design

#### **Accessibility & Usability**
- `AccessibilityDemo` - Keyboard navigation and screen readers
- `DarkMode` - Dark theme support

### 4. **Interactive Testing**
- Use the **Controls** tab to modify props in real-time
- Use **Viewport** controls to test different screen sizes
- Test keyboard navigation with Tab key
- Validate touch interactions on mobile viewports

## 🛠️ Manual Docker Commands

### Start Frontend Only
```bash
# Create network (if not exists)
docker network create trademaster-network

# Start frontend with Storybook
docker-compose -f docker-compose.yml -f docker-compose.frontend.yml up --build frontend
```

### Start Full Stack (Backend + Frontend)
```bash
# Start all services including backend
docker-compose --profile development up --build

# Or specific services
docker-compose up postgres redis auth-service frontend
```

### Production Build Testing
```bash
# Test production build
docker-compose --profile frontend-prod up --build frontend-prod

# Access at: http://localhost:3000
# Storybook at: http://localhost:3000/storybook
```

## 🐛 Troubleshooting

### Container Won't Start

**Simple Mode (Recommended):**
```bash
# Check logs
docker-compose -f docker-compose.frontend-simple.yml logs

# Rebuild from scratch
docker-compose -f docker-compose.frontend-simple.yml down -v
docker system prune -f
scripts/start-frontend-simple.bat  # Windows
# or
./scripts/start-frontend-simple.sh  # Linux/Mac
```

**Full Mode:**
```bash
# Check logs
docker-compose logs frontend

# Rebuild from scratch
docker-compose down -v
docker system prune -f
docker-compose up --build frontend
```

### Port Already in Use
```bash
# Kill processes on ports 6006/5173
# Windows:
netstat -ano | findstr :6006
taskkill /PID <PID> /F

# Linux/Mac:
lsof -ti:6006 | xargs kill -9
```

### Storybook Not Loading
1. Wait 30-60 seconds for initial build
2. Check container health: `docker ps`
3. Access container: `docker exec -it trademaster-frontend-dev bash`
4. Check inside container: `npm run storybook`

### Hot Reload Not Working
The setup includes volume mounts for hot reload:
- Source files: `./src:/app/src:cached`
- Config files: `./.storybook:/app/.storybook:cached`

## 📂 Container File Structure

```
/app/ (inside container)
├── src/
│   ├── components/auth/
│   │   ├── RegistrationForm.tsx
│   │   ├── FormField.tsx
│   │   └── RegistrationForm.stories.tsx
│   └── components/ui/
├── .storybook/
├── package.json
└── node_modules/ (volume mounted)
```

## 🎯 Validation Checklist

When using Storybook, validate these features:

### ✅ Mobile Responsiveness
- [ ] Form fits 375px width without scrolling
- [ ] Touch targets are 44px+ height
- [ ] Text size is 16px+ (prevents mobile zoom)
- [ ] Responsive layout changes at breakpoints

### ✅ Indian Phone Formatting
- [ ] `9876543210` → `+91 98765 43210`
- [ ] `09876543210` → `+91 98765 43210`
- [ ] `+919876543210` → `+91 98765 43210`
- [ ] Real-time formatting as user types

### ✅ Password Strength
- [ ] "weak" shows red indicator
- [ ] "StrongPass123!" shows green indicator
- [ ] Individual requirements show checkmarks
- [ ] Updates in real-time

### ✅ Real-time Validation
- [ ] Invalid email shows red border + error
- [ ] Valid email shows green border + checkmark
- [ ] Progress bar updates as fields complete
- [ ] Submit button enables when form valid

### ✅ Accessibility
- [ ] Tab navigation works through all fields
- [ ] Error messages are announced (use screen reader)
- [ ] Color contrast meets WCAG standards
- [ ] Focus indicators are visible

## 🔧 Development Workflow

### Making Changes
1. Edit files in `src/components/auth/`
2. Changes auto-reload in Storybook
3. Test different component states
4. Run tests: `docker exec -it trademaster-frontend-dev npm test`

### Adding New Stories
1. Edit `RegistrationForm.stories.tsx`
2. Add new story configurations
3. Storybook automatically updates

### Debugging
```bash
# Access container shell
docker exec -it trademaster-frontend-dev bash

# Run tests inside container
npm test

# Build for production
npm run build

# Check bundle size
npm run build && ls -la dist/
```

## 🚦 Service Health Checks

The container includes health checks:
- **Storybook**: http://localhost:6006
- **Container Health**: `docker ps` (should show "healthy")
- **Logs**: `docker-compose logs frontend`

## 🌐 Network Configuration

Services communicate via `trademaster-network`:
- Frontend: `http://frontend:5173`
- Backend API: `http://auth-service:8080`
- Database: `postgres:5432`

## 📈 Performance

Container optimizations:
- **Multi-stage build**: Separate dev/prod stages
- **Volume caching**: `node_modules` cached in named volume
- **File watching**: Optimized polling for Docker
- **Health checks**: Automatic restart on failure

---

🎉 **You're all set!** Open http://localhost:6006 to start validating the mobile registration form.