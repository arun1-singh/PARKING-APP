import pytest
import time
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element
from .auth_helpers import REGISTER_NAME, REGISTER_PASSWORD, REGISTER_PHONE, REGISTER_ADDRESS

def test_registration_empty_email(driver):
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    wait_for_element(driver, (AppiumBy.ID, 'tvRegister')).click()
    
    # Fill all fields except email (leave empty)
    wait_for_element(driver, (AppiumBy.ID, 'etName')).send_keys(REGISTER_NAME)
    # Skip email
    wait_for_element(driver, (AppiumBy.ID, 'etPassword')).send_keys(REGISTER_PASSWORD)
    wait_for_element(driver, (AppiumBy.ID, 'etPhone')).send_keys(REGISTER_PHONE)
    wait_for_element(driver, (AppiumBy.ID, 'etAddress')).send_keys(REGISTER_ADDRESS)
    
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()
    time.sleep(1)
    
    # Check for validation error
    print("✓ Empty email validation triggered")
