#!/bin/bash

# Fix all String→Throwable conversion errors
# Pattern: orElseThrow(error -> new RuntimeException("...", error))
# Fix to: orElseThrow(error -> new RuntimeException("..." + error))

echo "Fixing String→Throwable conversion errors..."

# Find all Java files and fix the pattern
find src -name "*.java" -type f -exec sed -i -E 's/new RuntimeException\("([^"]*)",\s*error\)/new RuntimeException("\1" + error)/g' {} \;

echo "Completed fixing String→Throwable conversions"
