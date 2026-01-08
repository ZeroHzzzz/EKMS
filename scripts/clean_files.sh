#!/bin/bash

# Define the upload directory
UPLOAD_DIR="/mnt/c/Users/31903/Desktop/EKMS/uploads"

# Check if directory exists
if [ -d "$UPLOAD_DIR" ]; then
    echo "Cleaning up files in $UPLOAD_DIR..."
    # Remove all files and subdirectories inside uploads, but keep the uploads directory itself
    rm -rf "$UPLOAD_DIR"/*
    echo "Files cleaned successfully."
else
    echo "Directory $UPLOAD_DIR does not exist."
fi
