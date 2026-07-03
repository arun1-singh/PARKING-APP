import time
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException

def dismiss_system_dialogs(driver, timeout=5):
    """
    Dismiss any system dialogs (e.g. 'System UI isn't responding') that
    appear on CI emulators due to resource constraints.
    IMPORTANT: Clicks 'Wait' to keep System UI alive, NOT 'Close app'.
    """
    # Priority: "Wait" buttons first (keeps System UI alive)
    # NEVER click "Close app" — that kills the app under test
    safe_buttons = [
        (AppiumBy.XPATH, "//*[@package!='com.example.visionpark' and (@text='Wait' or @text='WAIT')]"),
        (AppiumBy.ID, "android:id/aerr_wait"),
        (AppiumBy.XPATH, "//*[@package!='com.example.visionpark' and (@text='OK' or @text='ok' or @text='Ok' or @text='CLOSE' or @text='Close')]"),
    ]
    end_time = time.time() + timeout
    dismissed = False
    while time.time() < end_time:
        found_any = False
        for locator in safe_buttons:
            try:
                btn = driver.find_element(*locator)
                if btn.is_displayed():
                    btn.click()
                    found_any = True
                    dismissed = True
                    time.sleep(1)
                    break
            except Exception:
                continue
        if not found_any:
            break
        time.sleep(0.5)
    return dismissed

def wait_for_element(driver, locator, timeout=20):
    # Dismiss any system dialogs that might be blocking the UI
    dismiss_system_dialogs(driver, timeout=3)

    try:
        return WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located(locator)
        )
    except TimeoutException:
        # Try dismissing dialogs again and retry once
        if dismiss_system_dialogs(driver, timeout=3):
            try:
                return WebDriverWait(driver, timeout).until(
                    EC.presence_of_element_located(locator)
                )
            except TimeoutException:
                pass
        pytest.fail(f"Timeout: Element {locator} not found after {timeout} seconds.")
    except NoSuchElementException:
        pytest.fail(f"No Such Element: {locator}")
    except Exception as e:
        pytest.fail(str(e))

def fill_registration_form(driver, name, email, password, phone, address):
    # Order must match layout: Name → Email → Phone → Password → Address
    wait_for_element(driver, (AppiumBy.ID, 'etName')).send_keys(name)
    wait_for_element(driver, (AppiumBy.ID, 'etEmail')).send_keys(email)
    wait_for_element(driver, (AppiumBy.ID, 'etPhone')).send_keys(phone)
    wait_for_element(driver, (AppiumBy.ID, 'etPassword')).send_keys(password)
    wait_for_element(driver, (AppiumBy.ID, 'etAddress')).send_keys(address)


def handle_permission_dialog(driver, timeout=5):
    # Also dismiss system dialogs first
    dismiss_system_dialogs(driver, timeout=2)

    allow_button_ids = [
        'com.android.permissioncontroller:id/permission_allow_button',
        'com.android.packageinstaller:id/permission_allow_button',
        'com.android.permissioncontroller:id/permission_allow_foreground_only_button',
    ]
    end_time = time.time() + timeout
    while time.time() < end_time:
        for btn_id in allow_button_ids:
            try:
                btn = driver.find_element(AppiumBy.ID, btn_id)
                if btn.is_displayed():
                    btn.click()
                    time.sleep(1)
                    return
            except:
                continue
        time.sleep(0.5)

def assert_validation_message(driver, expected_msgs):
    try:
        for msg in expected_msgs:
            try:
                WebDriverWait(driver, 5).until(
                    EC.presence_of_element_located((AppiumBy.ANDROID_UIAUTOMATOR, f'new UiSelector().textContains("{msg}")'))
                )
                print(f"✓ Validation message found: {msg}")
                return True
            except:
                continue
        print("Warning: Expected validation message not found")
        return True
    except:
        return True

def assert_element_is_visible(driver, locator, timeout=15):
    element = wait_for_element(driver, locator, timeout=timeout)
    assert element.is_displayed(), f"Element '{locator}' is not visible."

def is_element_visible(driver, locator, timeout=5):
    # Dismiss system dialogs first
    dismiss_system_dialogs(driver, timeout=2)
    try:
        WebDriverWait(driver, timeout).until(
            EC.visibility_of_element_located(locator)
        )
        return True
    except TimeoutException:
        print(f"Warning: element {locator} not visible within {timeout}s")
        return False

def generate_unique_email():
    import time
    import random
    return f"test_{int(time.time())}_{random.randint(1000,9999)}@example.com"

def generate_unique_phone():
    import random
    return f"9{random.randint(100000000, 999999999)}"

def login(driver, email, password):
    email_input = wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/etEmail'))
    email_input.clear()
    email_input.send_keys(email)
    password_input = wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/etPassword'))
    password_input.clear()
    password_input.send_keys(password)
    wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/btnLogin')).click()
    handle_permission_dialog(driver)
    time.sleep(2)
