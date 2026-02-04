import requests
import time
import json
import logging
import concurrent.futures

# Configuration
BASE_URL = "http://localhost:9090/api/v1"
ANALYTICS_BASE = f"{BASE_URL}/analytics"
ADMIN_EMAIL = "admin@befapress.com"  # Replace with actual admin credentials if known or create a test user
ADMIN_PASSWORD = "password"          # Replace with actual password

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def get_auth_token():
    """Login and return JWT token"""
    try:
        response = requests.post(f"{BASE_URL}/auth/login", json={
            "email": ADMIN_EMAIL, 
            "password": ADMIN_PASSWORD
        })
        if response.status_code == 200:
            return response.json().get('token') or response.json().get('accessToken')
        else:
            logging.error(f"Login failed: {response.status_code} - {response.text}")
            return None
    except Exception as e:
        logging.error(f"Login error: {str(e)}")
        return None

def test_endpoint(name, method, url, token=None, payload=None):
    """Test a single endpoint and measure latency"""
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    headers["Content-Type"] = "application/json"
    
    start_time = time.time()
    try:
        if method == "GET":
            response = requests.get(url, headers=headers)
        elif method == "POST":
            response = requests.post(url, headers=headers, json=payload or {})
        else:
            return None
            
        latency = (time.time() - start_time) * 1000 # in ms
        
        status = "PASS" if 200 <= response.status_code < 300 else "FAIL"
        
        logging.info(f"{status} | {method} {url} | Status: {response.status_code} | Latency: {latency:.2f}ms")
        return {
            "name": name,
            "url": url,
            "method": method,
            "status_code": response.status_code,
            "latency_ms": latency,
            "result": status
        }
    except Exception as e:
        logging.error(f"Error calling {url}: {str(e)}")
        return {
            "name": name,
            "url": url,
            "method": method,
            "status_code": 0,
            "latency_ms": 0,
            "result": "ERROR"
        }

def run_tests():
    token = get_auth_token()
    if not token:
        logging.warning("Proceeding without auth token (some endpoints may fail)")
    
    endpoints = [
        # Dashboard & Overview
        ("Overview", "GET", f"{ANALYTICS_BASE}/overview"),
        ("Metric Views", "GET", f"{ANALYTICS_BASE}/metrics/views"),
        ("Metric Duration", "GET", f"{ANALYTICS_BASE}/metrics/session-duration"),
        ("Metric Bounce Rate", "GET", f"{ANALYTICS_BASE}/metrics/bounce-rate"),
        ("Metric Subscribers", "GET", f"{ANALYTICS_BASE}/metrics/subscribers"),
        
        # Traffic & Trends
        ("Daily Traffic", "GET", f"{ANALYTICS_BASE}/traffic/daily"),
        ("Monthly Traffic", "GET", f"{ANALYTICS_BASE}/traffic/monthly"),
        ("Traffic Sources", "GET", f"{ANALYTICS_BASE}/traffic/sources"),
        ("Realtime Users", "GET", f"{ANALYTICS_BASE}/traffic/realtime"),
        
        # Content Performance
        ("Top Articles", "GET", f"{ANALYTICS_BASE}/top-articles"),
        ("Top Authors", "GET", f"{ANALYTICS_BASE}/top-authors"),
        ("Categories", "GET", f"{ANALYTICS_BASE}/categories"),
        ("Article Stats (ID 1)", "GET", f"{ANALYTICS_BASE}/article/1/stats"),
        ("Trending", "GET", f"{ANALYTICS_BASE}/trending"),
        
        # User Engagement
        ("User Growth", "GET", f"{ANALYTICS_BASE}/users/growth"),
        ("Retention", "GET", f"{ANALYTICS_BASE}/users/retention"),
        ("Comments Activity", "GET", f"{ANALYTICS_BASE}/comments/activity"),
        ("Top Commenters", "GET", f"{ANALYTICS_BASE}/comments/top-users"),
        ("Shares", "GET", f"{ANALYTICS_BASE}/shares"),
        
        # Audience
        ("Device Breakdown", "GET", f"{ANALYTICS_BASE}/audience/devices"),
        ("Geo Distribution", "GET", f"{ANALYTICS_BASE}/audience/geo"),
        ("Languages", "GET", f"{ANALYTICS_BASE}/audience/languages"),
        ("Roles", "GET", f"{ANALYTICS_BASE}/audience/roles"),
        
        # Tech & Ads
        ("Ad Performance", "GET", f"{ANALYTICS_BASE}/ads/performance"),
        ("Ad CTR", "GET", f"{ANALYTICS_BASE}/ads/ctr"),
        ("Page Load", "GET", f"{ANALYTICS_BASE}/technical/page-load"),
        ("Error Stats", "GET", f"{ANALYTICS_BASE}/technical/errors"),
    ]
    
    # Tracking endpoints (POST)
    tracking_endpoints = [
        ("Track Page Hit", "POST", f"{ANALYTICS_BASE}/track", {
            "entityType": "HOME",
            "entityId": 0,
            "sessionId": "test-session",
            "language": "en"
        }),
        ("Track Scroll", "POST", f"{ANALYTICS_BASE}/track/scroll", {
            "entityType": "NEWS",
            "entityId": 1,
            "sessionId": "test-session",
            "scrollDepth": 50,
            "timeOnPage": 30
        }),
        ("Track Share", "POST", f"{ANALYTICS_BASE}/track/share", {
            "entityType": "NEWS",
            "entityId": 1,
            "platform": "FACEBOOK",
            "sessionId": "test-session"
        }),
        ("Track Session", "POST", f"{ANALYTICS_BASE}/track/session", {"dummy": True}),
    ]
    
    results = []
    
    print("\n--- Starting API Performance & Security Test ---\n")
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        future_to_endpoint = {
            executor.submit(test_endpoint, name, method, url, token, payload): name 
            for name, method, url, payload in [(*e, None) for e in endpoints] + tracking_endpoints
        }
        
        for future in concurrent.futures.as_completed(future_to_endpoint):
            result = future.result()
            if result:
                results.append(result)
    
    # Security Check (Unauthenticated)
    print("\n--- Security Check (Unauthenticated Access) ---\n")
    unauth_test = test_endpoint("Unauth Overview", "GET", f"{ANALYTICS_BASE}/overview", None)
    if unauth_test['status_code'] in [401, 403]:
        print("PASS: Unauthenticated access blocked (403/401)")
    elif unauth_test['status_code'] == 200:
        print("WARN: Unauthenticated access allowed (200 OK) - Check Security Config")
    else:
        print(f"INFO: Unauthenticated access returned {unauth_test['status_code']}")

if __name__ == "__main__":
    try:
        run_tests()
    except KeyboardInterrupt:
        print("\nTest cancelled")
