# 🚀 Start TradeMaster React App

Test the registration form directly in the React app (not Storybook).

## ✅ **Quick Start**

**Windows:**
```cmd
cd trademaster
scripts\start-app.bat
```

**Linux/Mac:**
```bash
cd trademaster
chmod +x scripts/start-app.sh
./scripts/start-app.sh
```

## 📱 **What You'll See**

- **URL**: http://localhost:5173
- **Display**: Mobile registration form centered on the page
- **Background**: Light gray background with the form in the center
- **Features**: All the features you requested working live

## 🎯 **Test Features**

When the app loads, you can test:

### ✅ **Real-time Validation**
- Type invalid email → See red border and error message
- Type valid email → See green border and checkmark

### ✅ **Indian Phone Formatting**
- Type `9876543210` → Automatically formats to `+91 98765 43210`
- Type `09876543210` → Formats to `+91 98765 43210`

### ✅ **Password Strength**
- Type `weak` → Red "weak" indicator
- Type `StrongPass123!` → Green "strong" indicator with progress bar

### ✅ **Mobile Responsiveness**
- Resize browser to 375px width
- Form should fit perfectly without scrolling
- Touch targets are 44px+ for mobile

### ✅ **Form Submission**
- Fill all fields with valid data
- Submit button becomes enabled
- Click submit → Alert shows success + console logs data

## 🔧 **Manual Commands**

If the script doesn't work:

```cmd
cd trademaster
npm install
npm run dev
```

## 🎨 **Expected Behavior**

1. **Page loads** with centered registration form
2. **Progress indicator** shows "Step 1 of 3"
3. **Real-time validation** works as you type
4. **Phone formatting** happens automatically
5. **Password strength** updates in real-time
6. **Form submission** works with console logging

## 📱 **Mobile Testing**

To test mobile view:
1. Open Chrome DevTools (F12)
2. Click device toggle (phone icon)
3. Select "iPhone SE" or custom 375px width
4. Test touch interactions and formatting

---

**This shows your actual registration form code running live!** 🎉