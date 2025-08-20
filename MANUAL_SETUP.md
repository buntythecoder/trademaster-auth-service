# 🛠️ Manual Local Setup Guide

If you're getting "vite is not recognized" errors, follow these manual steps:

## 📋 **Prerequisites Check**

### 1. **Install Node.js** (if not installed)
- Go to: https://nodejs.org/
- Download the **LTS version** (18.x or higher)
- Install and **restart your command prompt/terminal**
- Verify: `node --version` and `npm --version`

### 2. **Navigate to Project Directory**
```cmd
cd trademaster
```

## 🔧 **Manual Setup Steps**

### **Step 1: Clean Installation**
```cmd
REM Remove old files (if they exist)
rmdir /s /q node_modules
del package-lock.json

REM Clear npm cache
npm cache clean --force
```

### **Step 2: Install Dependencies**
```cmd
npm install
```

**If this fails, try:**
```cmd
npm install --verbose
```

### **Step 3: Verify Installation**
Check if these files exist:
```cmd
dir node_modules\.bin\vite.cmd
dir node_modules\.bin\storybook.cmd
```

### **Step 4: Start Storybook**
```cmd
npm run storybook
```

---

## 🚨 **Troubleshooting Common Issues**

### **Issue: "npm is not recognized"**
**Solution:** Node.js not installed properly
- Reinstall Node.js from https://nodejs.org/
- Choose "Add to PATH" during installation
- Restart command prompt

### **Issue: "vite is not recognized"**
**Solution:** Dependencies not installed
```cmd
npm install
npm list vite
```

### **Issue: "Permission denied" (Linux/Mac)**
**Solution:**
```bash
sudo npm install -g npm@latest
npm install
```

### **Issue: Installation hangs or fails**
**Solution:**
```cmd
npm config set registry https://registry.npmjs.org/
npm cache clean --force
npm install --no-optional
```

### **Issue: "Module not found" errors**
**Solution:**
```cmd
npm install --save-dev @types/react @types/react-dom
npm install
```

---

## 🎯 **Alternative: Use npx (No Installation Required)**

If regular installation fails, try using npx:

```cmd
npx storybook@latest dev -p 6006
```

This downloads and runs Storybook temporarily.

---

## ✅ **Quick Test Commands**

After installation, test these work:

```cmd
REM Check if scripts are available
npm run --help

REM List installed packages
npm list --depth=0

REM Test Storybook
npm run storybook

REM Test Vite (in new terminal)
npm run dev
```

---

## 🎨 **Expected Results**

When successful:
- ✅ `npm run storybook` opens http://localhost:6006
- ✅ You see Storybook interface
- ✅ "Auth/RegistrationForm" appears in sidebar
- ✅ Registration form works with all features

---

## 🆘 **Still Not Working?**

### **Option 1: Use Global Installation**
```cmd
npm install -g @storybook/cli
npm install -g vite
storybook dev -p 6006
```

### **Option 2: Use Online IDE**
- Open the project in **VS Code** with **Live Server extension**
- Or use **CodeSandbox** or **StackBlitz** online

### **Option 3: Manual File Check**
Verify these key files exist:
- ✅ `package.json`
- ✅ `src/components/auth/RegistrationForm.tsx`
- ✅ `src/components/auth/RegistrationForm.stories.tsx`
- ✅ `.storybook/main.ts`

### **Option 4: Minimal Test**
Create a simple test file:
```cmd
echo console.log('Node.js works!') > test.js
node test.js
del test.js
```

---

## 🚀 **Automated Setup Script**

For the easiest setup, run:

**Windows:**
```cmd
scripts\setup-local.bat
```

**Linux/Mac:**
```bash
chmod +x scripts/setup-local.sh
./scripts/setup-local.sh
```

This handles everything automatically! 🎉