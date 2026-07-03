import pytest
from appium.webdriver.common.appiumby import AppiumBy
from tests.common import wait_for_element, assert_element_is_visible
from tests.auth_helpers import (
    register_user,
    login,
    generate_unique_username,
    generate_unique_email,
    generate_unique_phone
)
from tests.constants import REGISTER_PASSWORD, REGISTER_ADDRESS

def test_login_with_incorrect_password(driver):
    """
    Tests that a user cannot log in with a valid email but an incorrect password.
    Steps:
    1. Generate unique credentials for a new user.
    2. Navigate to the registration screen.
    3. Register the new user. The app will be on the login screen after registration.
    4. Attempt to log in with the correct email but a deliberately incorrect password.
    5. Verify that an error message is shown and the user remains on the login screen.
    """
    # 1. Generate unique credentials and an incorrect password
    unique_username = generate_unique_username()
    unique_email = generate_unique_email()
    correct_password = REGISTER_PASSWORD
    incorrect_password = "thisIsTheWrongPassword123"
    phone = generate_unique_phone()
    address = REGISTER_ADDRESS

    # 2. Navigate to registration screen
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()

    # 3. Register the user to ensure the account exists
    register_user(driver, unique_username, unique_email, correct_password, phone, address)

    # 4. Attempt to log in with the incorrect password
    # The 'register_user' helper leaves us on the login screen, so we can call login directly.
    login(driver, unique_email, incorrect_password, expect_success=False)

    # 5. Verify the user is still on the login screen
    # We check for the "Don't have an account? Register" text view, which only exists on the login screen.
    login_screen_indicator = (AppiumBy.ID, 'tvRegister')
    assert_element_is_visible(driver, login_screen_indicator)
