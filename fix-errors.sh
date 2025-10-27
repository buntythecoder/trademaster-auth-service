#!/bin/bash

# Fix all error.getMessage() calls where error is String (from Result types)
find src -name "*.java" -type f -exec sed -i 's/error\.getMessage()/error/g' {} \;

echo "Fixed all error.getMessage() calls"
