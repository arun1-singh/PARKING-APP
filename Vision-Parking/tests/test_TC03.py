import pytest
import time
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element, login
from .auth_helpers import register_user, generate_unique_email, generate_unique_phone, REGISTER_PASSWORD, REGISTER_NAME, REGISTER_ADDRESS

def test_login_after_registration(driver):
    unique_email = generate_unique_email()
    unique_phone = generate_unique_phone()
    
    # Click Get Started first to reach login screen
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    
    # Register new user
    register_user(driver, REGISTER_NAME, unique_email, REGISTER_PASSWORD, unique_phone, REGISTER_ADDRESS)
    time.sleep(2)
    
    # Login with the new user
    login(driver, unique_email, REGISTER_PASSWORD)
    
    # Verify login success by checking map fragment
    wait_for_element(driver, (AppiumBy.ID, 'mapFragment'), timeout=15)
    print("✓ Login successful")
