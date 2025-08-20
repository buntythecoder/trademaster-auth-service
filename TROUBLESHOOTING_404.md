# 🚨 Fixing 404 Error on localhost:6006

The 404 error means Storybook isn't starting properly. Here are the solutions in order of simplicity:

## 🎯 **Option 1: Local Development (Recommended)**

Skip Docker entirely and run locally with Node.js:

**Windows:**
```cmd
scripts\start-local.bat
```

**Linux/Mac:**
```bash
chmod +x scripts/start-local.sh
./scripts/start-local.sh
```

**Requirements:**
- Node.js 18+ installed
- npm available in PATH

**What this does:**
- Installs dependencies locally
- Starts Storybook on http://localhost:6006
- No Docker required

---

## 🔍 **Option 2: Debug Docker Issue**

If you want to fix the Docker setup:

**1. Run diagnostics:**
```cmd
scripts\debug-frontend.bat  # Windows
./scripts/debug-frontend.sh  # Linux/Mac
```

**2. Check container status:**
```bash
docker ps -a | grep trademaster-frontend
docker logs trademaster-frontend-simple
```

**3. Common fixes:**
```bash
# Clean everything
docker-compose -f docker-compose.frontend-simple.yml down -v
docker system prune -f
docker volume prune -f

# Restart
scripts\start-frontend-simple.bat
```

---

## 🎯 **Option 3: Direct Vite Server**

Run the Vite development server to see the form in a simple app:

```bash
npm run dev
```

Then access:
- **Vite App**: http://localhost:5173 (registration form demo)
- **Storybook**: http://localhost:6006 (component library)

---

## 🐛 **Common Issues & Solutions**

### Issue: "npm: command not found"
**Solution:** Install Node.js from https://nodejs.org/

### Issue: "EADDRINUSE: port 6006 already in use"
**Solution:**
```bash
# Windows
netstat -ano | findstr :6006
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:6006 | xargs kill -9
```

### Issue: Docker container exits immediately
**Solution:**
```bash
# Check logs
docker logs trademaster-frontend-simple

# Common fix - rebuild
docker-compose -f docker-compose.frontend-simple.yml up --build --force-recreate
```

### Issue: Dependencies not installing
**Solution:**
```bash
# Clear npm cache
npm cache clean --force

# Delete and reinstall
rm -rf node_modules package-lock.json
npm install
```

---

## ✅ **Quick Validation Steps**

1. **Check if Node.js is installed:**
   ```bash
   node --version  # Should show v18.x.x or higher
   npm --version   # Should show npm version
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start Storybook locally:**
   ```bash
   npm run storybook
   ```

4. **Access registration form:**
   - Open: http://localhost:6006
   - Navigate to: Auth → RegistrationForm
   - Try different stories to test features

---

## 🎨 **Expected Result**

When working, you should see:
- ✅ Storybook interface at http://localhost:6006
- ✅ "Auth/RegistrationForm" in the sidebar
- ✅ Interactive registration form with all features
- ✅ Different stories for testing various states

---

## 📞 **Still Having Issues?**

Try this minimal test:

```bash
# Test if basic Node.js works
node -e "console.log('Node.js works!')"

# Test if npm works
npm --version

# Test if dependencies install
npm install --dry-run

# Start with verbose logging
npm run storybook -- --verbose
```

The local approach (Option 1) should work immediately if you have Node.js installed! 🚀