#!/bin/bash

BASE_URL="http://localhost:8080/api"

# Login
echo "Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}')

# Extract token using grep and cut (simple JSON parsing)
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Login failed."
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "Login successful."

# Rebuild Index
echo "Triggering index rebuild..."
curl -s -X POST "$BASE_URL/knowledge/search/reindex" \
  -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" | grep -o '"message":"[^"]*"' | cut -d'"' -f4

echo ""
echo "Done."
