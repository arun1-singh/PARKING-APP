import pytest
import sys
import os
import time
from appium import webdriver
from appium.options.android import UiAutomator2Options

# Ensure the tests directory and its parent are in sys.path
tests_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(tests_dir)
if tests_dir not in sys.path:
    sys.path.insert(0, tests_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)
import socket
import urllib.request
import json


def is_appium_running(host='127.0.0.1', port=4723):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(1.0)
    try:
        s.connect((host, port))
        s.close()
        return True
    except Exception:
        return False

def _detect_appium_url(host='127.0.0.1', port=4723):
    """
    Auto-detect whether Appium server is running with /wd/hub base path
    or without (Appium 2.x+ default). Try /wd/hub first since our CI
    starts Appium with --base-path /wd/hub, then fall back to bare URL.
    """
    import urllib.request
    urls = [
        f'http://{host}:{port}/wd/hub',
        f'http://{host}:{port}',
    ]
    for url in urls:
        try:
            req = urllib.request.Request(f'{url}/status', method='GET')
            with urllib.request.urlopen(req, timeout=3) as resp:
                if resp.status == 200:
                    return url
        except Exception:
            continue
    # Default fallback — try /wd/hub (matching CI config)
    return f'http://{host}:{port}/wd/hub'

def pytest_runtest_setup(item):
    if not is_appium_running():
        pytest.skip("Appium server is not running at 127.0.0.1:4723, skipping Appium tests.")

@pytest.fixture(scope="session", autouse=True)
def pre_register_shared_user():
    """
    Session-scoped fixture that pre-registers REGISTER_EMAIL/REGISTER_PASSWORD
    via the FastAPI backend HTTP API before any Appium tests run.
    Tests that use the static REGISTER_EMAIL constant (TC13-26) can then log
    in directly without having to register first via the UI.
    Runs once per test session. Ignores 409 Conflict (user already exists).
    """
    from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD, REGISTER_NAME, REGISTER_PHONE, REGISTER_ADDRESS
    
    # Wait for backend to be ready first
    backend_health_url = "http://127.0.0.1:5000/health"
    print("[conftest] Waiting for backend to be ready...")
    for i in range(30):  # Wait up to 30 seconds
        try:
            req = urllib.request.Request(backend_health_url, method='GET')
            with urllib.request.urlopen(req, timeout=2) as resp:
                if resp.status == 200:
                    print("[conftest] Backend is ready")
                    break
        except Exception:
            pass
        time.sleep(1)
    else:
        print("[conftest] WARNING: Backend may not be ready, proceeding anyway")
    
    backend_url = "http://127.0.0.1:5000/auth/register"
    payload = json.dumps({
        "user_name": REGISTER_NAME,
        "user_email": REGISTER_EMAIL,
        "user_password": REGISTER_PASSWORD,
        "user_phone_no": REGISTER_PHONE,
        "user_address": REGISTER_ADDRESS,
    }).encode("utf-8")
    
    # Retry registration up to 3 times
    for attempt in range(3):
        try:
            req = urllib.request.Request(
                backend_url,
                data=payload,
                headers={"Content-Type": "application/json"},
                method="POST"
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                print(f"[conftest] Pre-registered shared user: {REGISTER_EMAIL} (HTTP {resp.status})")
                break
        except urllib.error.HTTPError as e:
            if e.code == 409:
                print(f"[conftest] Shared user already exists: {REGISTER_EMAIL}")
                break
            else:
                print(f"[conftest] WARNING: pre-registration failed HTTP {e.code}: {e.reason} (attempt {attempt+1}/3)")
                if attempt < 2:
                    time.sleep(2)
        except Exception as ex:
            print(f"[conftest] WARNING: Could not pre-register shared user: {ex} (attempt {attempt+1}/3)")
            if attempt < 2:
                time.sleep(2)
    yield

@pytest.fixture(scope="function")
def driver():
    options = UiAutomator2Options()
    options.platform_name = 'Android'
    options.automation_name = 'UiAutomator2'
    options.device_name = 'Android Emulator'
    options.app_package = 'com.example.visionpark'
    options.app_activity = 'com.example.visionpark.activities.SplashScreenActivity'
    options.implicit_wait_timeout = 15000

    # Extended timeouts for CI environment
    options.set_capability('uiautomator2ServerInstallTimeout', 180000)  # 3 minutes
    options.set_capability('uiautomator2ServerLaunchTimeout', 180000)   # 3 minutes
    options.set_capability('adbExecTimeout', 120000)                    # 2 minutes
    options.set_capability('androidInstallTimeout', 120000)             # 2 minutes
    options.set_capability('newCommandTimeout', 900)                    # 15 minutes
    options.set_capability('autoGrantPermissions', True)
    options.set_capability('skipServerInstallation', False)
    options.set_capability('skipDeviceInitialization', False)
    options.set_capability('disableWindowAnimation', True)
    options.set_capability('skipLogcatCapture', True)
    options.no_reset = False

    # Auto-detect Appium URL (with or without /wd/hub)
    appium_url = _detect_appium_url()
    print(f"Connecting to Appium at: {appium_url}")

    driver = webdriver.Remote(appium_url, options=options)

    # Allow extra time for the app to fully load on CI
    time.sleep(3)

    # Dismiss any "System UI isn't responding" or ANR dialogs
    from tests.common import dismiss_system_dialogs
    dismiss_system_dialogs(driver, timeout=8)

    yield driver
    driver.quit()
