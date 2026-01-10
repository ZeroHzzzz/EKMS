#!/bin/bash

# Wrapper script to reset system data
# Usage: ./reset.sh [password]
# Password is optional, defaults to 'admin' if sudo is needed and not provided.

echo "Starting System Data Reset..."

# Ensure executable permissions on the actual script
chmod +x ./scripts/clean_system.sh

# Run the cleanup script with force flag
# Using 'bash' to run it directly
./scripts/clean_system.sh --force

echo "Reset sequence completed."
