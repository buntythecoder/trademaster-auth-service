#!/bin/bash

echo "Fixing remaining compilation errors..."

# Fix 1: Replace fold() patterns that return ResponseEntity directly
# Pattern: return ResponseEntity.ok(...) inside SafeOperations.safelyToResult()
echo "Step 1: Fixing fold() with ResponseEntity returns..."
find src -name "*.java" -type f -exec sed -i -E '/fold\(/,/\);$/ {
    s/return ResponseEntity\.ok\((.*)\);/return \1;/g
}' {} \;

# Fix 2: Add missing Optional import where needed
echo "Step 2: Adding missing Optional imports..."
for file in $(grep -l "cannot find symbol" build.log 2>/dev/null | grep -o "src/.*\.java" | sort -u); do
    if [ -f "$file" ] && ! grep -q "import java.util.Optional;" "$file"; then
        # Add Optional import after other java.util imports
        sed -i '/import java\.util\./a import java.util.Optional;' "$file" 2>/dev/null || true
    fi
done

# Fix 3: Fix String to Long conversions
echo "Step 3: Fixing String→Long conversions..."
# This is context-specific, so we'll handle individually

echo "Completed batch fixes. Please review remaining errors manually."
