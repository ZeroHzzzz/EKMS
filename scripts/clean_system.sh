#!/bin/bash

# Script to clean system data (Database and Files)
# Preserves Users and Departments

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
UPLOAD_DIR="$PROJECT_ROOT/uploads"
SQL_FILE="$PROJECT_ROOT/sql/clean_system_keep_users.sql"

echo "=========================================="
echo "      EKMS SYSTEM CLEANUP TOOL            "
echo "=========================================="
echo "This script will:"
echo "1. Delete all uploaded files in $UPLOAD_DIR"
echo "2. Clear all database tables EXCEPT 'user', 'department', 'roles', 'permissions'"
echo ""
echo "WARNING: THIS ACTION CANNOT BE UNDONE!"
echo "=========================================="

if [ "$1" == "--force" ]; then
    echo "Force mode detected. Skipping confirmation."
else
    read -p "Are you sure you want to proceed? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
fi

# 1. Clean Files
echo ""
echo "[1/2] Cleaning uploaded files..."
if [ -d "$UPLOAD_DIR" ]; then
    # Delete files recursively but keep the directory
    rm -rf "$UPLOAD_DIR"/*
    echo "Files deleted."
else
    echo "Upload directory not found: $UPLOAD_DIR"
fi

# 2. Clean Database
echo ""
echo "[2/2] Cleaning database..."
if [ -f "$SQL_FILE" ]; then
    # Try using docker exec (assuming standard setup from reinit_database.sh)
    if command -v docker &> /dev/null; then
        echo "Executing SQL via Docker (knowledge-mysql)..."
        docker exec -i knowledge-mysql mysql -uroot -proot knowledge_db < "$SQL_FILE"
        
        if [ $? -eq 0 ]; then
            echo "Database cleaned successfully."
        else
            echo "Error executing SQL via Docker. Please check if container 'knowledge-mysql' is running."
            # Fallback suggestion
            echo "You can manually run: mysql -u root -p knowledge_db < $SQL_FILE"
        fi
    else
        echo "Docker not found. Trying local mysql client..."
        mysql -u root -proot knowledge_db < "$SQL_FILE"
        if [ $? -ne 0 ]; then
             echo "Failed to execute SQL. Please ensure 'knowledge-mysql' container is running or you have 'mysql' client installed."
        fi
    fi
else
    echo "SQL script not found: $SQL_FILE"
fi

# 3. Clean Elasticsearch Index
echo ""
echo "[3/3] Cleaning Elasticsearch indices..."
if command -v curl &> /dev/null; then
    echo "Deleting 'knowledge_index'..."
    response=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:9200/knowledge_index")
    if [ "$response" -eq 200 ]; then
        echo "Index deleted successfully."
    elif [ "$response" -eq 404 ]; then
        echo "Index not found (already deleted)."
    else
        echo "Failed to delete index. HTTP Status: $response"
        echo "Check if Elasticsearch is running on localhost:9200"
    fi
else
    echo "curl not found. Cannot delete ES index."
fi

echo ""
echo "=========================================="
echo "           CLEANUP COMPLETED              "
echo "=========================================="
