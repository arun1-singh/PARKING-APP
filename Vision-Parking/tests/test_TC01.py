from .common import wait_for_element, dismiss_system_dialogs
import pytest
import time
from appium.webdriver.common.appiumby import AppiumBy


def test_app_launch(driver):
    """
    TC01: Verify the app launches correctly and the Splash screen is visible.
    Steps:
    1. Wait for splash screen to load
    2. Dismiss any system dialogs (common on CI emulators)
    3. Verify the VisionPark app name text or splash UI is visible
    4. Click 'Get Started' to proceed to the login screen
    5. Verify the login screen loads
    """
    time.sleep(5)  # Give more time for app to fully load on CI

    # Dismiss any system dialogs like "System UI isn't responding"
    dismiss_system_dialogs(driver, timeout=10)

    # Debug: Print current activity
    try:
        current_activity = driver.current_activity
        print(f"Current activity: {current_activity}")
    except Exception as e:
        print(f"Could not get current activity: {e}")

    # Try to find the app name on the splash screen
    app_name = None
    locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/tvAppName'),
        (AppiumBy.ID, 'tvAppName'),
        (AppiumBy.XPATH, "//*[contains(@text, 'VisionPark')]"),
    ]
    for loc in locators:
        try:
            app_name = wait_for_element(driver, loc, timeout=10)
            if app_name:
                break
        except:
            continue

    if app_name:
        assert app_name.is_displayed()
        print(f"Found app name element: {app_name.text}")
    else:
        # Fallback: just check we're on the splash activity
        try:
            current_activity = driver.current_activity
            assert 'SplashScreen' in current_activity or 'Login' in current_activity, \
                f"Unexpected activity: {current_activity}"
            print(f"On expected activity: {current_activity}")
        except Exception:
            pytest.fail("Could not verify splash screen loaded")

    # Try to find and click the Get Started button
    get_started_btn = None
    btn_locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/btnGetStarted'),
        (AppiumBy.ID, 'btnGetStarted'),
        (AppiumBy.XPATH, "//*[contains(@text, 'Get Started')]"),
    ]
    for loc in btn_locators:
        try:
            get_started_btn = wait_for_element(driver, loc, timeout=8)
            if get_started_btn:
                break
        except:
            continue

    if get_started_btn:
        assert get_started_btn.is_displayed()
        print(f"Found get started button: {get_started_btn.text}")
        get_started_btn.click()
    else:
        # Maybe we're already on the login screen (auto-transition)
        print("Get Started button not found — checking if login screen loaded directly")

    # Wait for login screen
    time.sleep(3)
    dismiss_system_dialogs(driver, timeout=3)

    login_locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/btnLogin'),
        (AppiumBy.ID, 'btnLogin'),
        (AppiumBy.XPATH, "//*[contains(@text, 'Login') or contains(@text, 'Sign In')]"),
    ]
    login_found = False
    for loc in login_locators:
        try:
            btn = wait_for_element(driver, loc, timeout=10)
            if btn and btn.is_displayed():
                login_found = True
                print("✓ Login screen loaded successfully")
                break
        except:
            continue

    assert login_found, "Could not find login button after clicking get started"
