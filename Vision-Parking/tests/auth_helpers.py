import uuid
import random
import time
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element, handle_permission_dialog, dismiss_system_dialogs

def generate_unique_username():
    return f"testuser_{uuid.uuid4().hex[:8]}"

def generate_unique_email():
    return f"testuser_{uuid.uuid4().hex[:8]}@example.com"

def generate_unique_phone():
    return f"9{random.randint(100000000, 999999999)}"

def login(driver, email, password, expect_success=True):
    email_input = wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/etEmail'))
    email_input.clear()
    email_input.send_keys(email)
    password_input = wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/etPassword'))
    password_input.clear()
    password_input.send_keys(password)
    wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/btnLogin')).click()
    if expect_success:
        handle_permission_dialog(driver)
        time.sleep(2)

def register_user(driver, name, email, password, phone, address):
    # Click on "Register here" from login screen
    wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/tvRegister')).click()
    time.sleep(1)
    
    # Fill registration form (order must match layout: Name → Email → Phone → Password → Address)
    wait_for_element(driver, (AppiumBy.ID, 'etName')).send_keys(name)
    wait_for_element(driver, (AppiumBy.ID, 'etEmail')).send_keys(email)
    wait_for_element(driver, (AppiumBy.ID, 'etPhone')).send_keys(phone)
    wait_for_element(driver, (AppiumBy.ID, 'etPassword')).send_keys(password)
    wait_for_element(driver, (AppiumBy.ID, 'etAddress')).send_keys(address)
    
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()
    
    # Wait for the app to navigate back to the Login screen (meaning registration succeeded)
    try:
        wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/btnLogin'), timeout=25)
    except Exception:
        import pytest
        pytest.fail("Failed to return to Login screen after registration. Registration API call likely failed (check backend server status).")
    
    time.sleep(2)

# Test constants
REGISTER_NAME = "Arun Singh"
REGISTER_EMAIL = generate_unique_email()
REGISTER_PHONE = generate_unique_phone()
REGISTER_ADDRESS = "456Noida"
REGISTER_PASSWORD = "password123"
