import pytest
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element, fill_registration_form, assert_validation_message
from .auth_helpers import generate_unique_email, register_user

def test_registration_short_password(driver):
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    wait_for_element(driver, (AppiumBy.ID, 'tvRegister')).click()
    email = generate_unique_email()
    fill_registration_form(driver, "Test User", email, "123", "1234567890", "Test Address")
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()
    assert_validation_message(driver, ["password", "short", "minimum"])
