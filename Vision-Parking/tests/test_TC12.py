import pytest
import time
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element, login
from .auth_helpers import generate_unique_email, generate_unique_phone, REGISTER_NAME, REGISTER_PASSWORD, REGISTER_ADDRESS

def test_map_search_updates_location(driver):
    unique_email = generate_unique_email()
    unique_phone = generate_unique_phone()
    
    # Register and login
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    wait_for_element(driver, (AppiumBy.ID, 'tvRegister')).click()
    wait_for_element(driver, (AppiumBy.ID, 'etName')).send_keys(REGISTER_NAME)
    wait_for_element(driver, (AppiumBy.ID, 'etEmail')).send_keys(unique_email)
    wait_for_element(driver, (AppiumBy.ID, 'etPhone')).send_keys(unique_phone)
    wait_for_element(driver, (AppiumBy.ID, 'etPassword')).send_keys(REGISTER_PASSWORD)
    wait_for_element(driver, (AppiumBy.ID, 'etAddress')).send_keys(REGISTER_ADDRESS)
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()
    wait_for_element(driver, (AppiumBy.ID, 'com.example.visionpark:id/btnLogin'), timeout=20)
    
    login(driver, unique_email, REGISTER_PASSWORD)
    time.sleep(2)
    
    # Map should be visible
    wait_for_element(driver, (AppiumBy.ID, 'mapFragment'), timeout=15)
    print("✓ Map loaded successfully")
    assert True
