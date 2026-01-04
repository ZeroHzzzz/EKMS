import requests
import json
import sys

BASE_URL = "http://localhost:8080/api"

def login():
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "admin",
        "password": "password123"
    }
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            result = response.json()
            if result['code'] == 200:
                print("Login successful")
                return result['data']['token']
            else:
                print(f"Login failed: {result['message']}")
        else:
            print(f"Login failed with status code: {response.status_code}")
            print(response.text)
    except Exception as e:
        print(f"Login error: {e}")
        print("Make sure the backend services are running (Gateway at 8080).")
    return None

def rebuild_index(token):
    url = f"{BASE_URL}/knowledge/search/reindex"
    headers = {
        "Authorization": token
    }
    try:
        print("Triggering index rebuild...")
        response = requests.post(url, headers=headers)
        if response.status_code == 200:
            result = response.json()
            if result['code'] == 200:
                print(f"Rebuild successful: {result['data']}")
            else:
                print(f"Rebuild failed: {result['message']}")
        else:
            print(f"Rebuild failed with status code: {response.status_code}")
            print(response.text)
    except Exception as e:
        print(f"Rebuild error: {e}")

if __name__ == "__main__":
    token = login()
    if token:
        rebuild_index(token)
