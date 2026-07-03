import pytest
from .common import wait_for_element, fill_registration_form, assert_validation_message
from .auth_helpers import generate_unique_email, generate_unique_phone, generate_unique_username
from appium.webdriver.common.appiumby import AppiumBy
import time

def test_duplicate_registration(driver):
    """
    Ensures the app prevents registration with credentials that already exist.
    This test is self-contained:
    1. It registers a new user and confirms the app navigates to the login page.
    2. It navigates back to the registration screen.
    3. It attempts to register with the same credentials to trigger the duplicate error.
    """
    # --- SETUP: Generate all necessary credentials within the test ---
    test_username = generate_unique_username()
    test_email = generate_unique_email()
    test_phone = generate_unique_phone()
    test_password = "password123"
    test_address = "123 Test Street, Test City"

    # 1. Navigate to the registration screen for the first time
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    wait_for_element(driver, (AppiumBy.ID, 'tvRegister')).click()

    # 2. Register the user for the first time with the generated credentials.
    print(f"Registering a new unique user: {test_email}")
    fill_registration_form(driver, test_username, test_email,
                           test_password, test_phone, test_address)
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()

    # 3. Assert that the first registration was successful by verifying
    #    that the app has navigated to the login page.
    print("First registration submitted. Verifying navigation to login page...")
    wait_for_element(driver, (AppiumBy.ID, 'btnLogin'), timeout=15)
    print("Successfully navigated to login page.")

    # --- TEST: Attempt to register the same user again ---
    # 4. From the login page, navigate back to the registration screen.
    wait_for_element(driver, (AppiumBy.ID, 'tvRegister')).click()
    print("Navigated back to registration screen.")

    # 5. Fill the form with the EXACT SAME credentials
    print(f"Attempting to register duplicate user: {test_email}")
    # The 'fill_registration_form' helper does not clear fields first, so we will do it here
    # to be certain we are not accidentally appending to any existing text.
    name_field = wait_for_element(driver, (AppiumBy.ID, 'etName'))
    name_field.clear()
    name_field.send_keys(test_username)

    email_field = wait_for_element(driver, (AppiumBy.ID, 'etEmail'))
    email_field.clear()
    email_field.send_keys(test_email)

    password_field = wait_for_element(driver, (AppiumBy.ID, 'etPassword'))
    password_field.clear()
    password_field.send_keys(test_password)

    phone_field = wait_for_element(driver, (AppiumBy.ID, 'etPhone'))
    phone_field.clear()
    phone_field.send_keys(test_phone)

    address_field = wait_for_element(driver, (AppiumBy.ID, 'etAddress'))
    address_field.clear()
    address_field.send_keys(test_address)

    # Click the register button for the second time
    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()

    # --- ASSERT: Verify that the duplicate user error message is displayed ---
    print("Verifying duplicate registration error message...")
    assert_validation_message(driver, [
        "Registration Failed: CONFLICT", "Email or phone number already registered", "already", "exists", "duplicate", "registered"
    ])
    print("Test passed: Duplicate registration was correctly blocked.")