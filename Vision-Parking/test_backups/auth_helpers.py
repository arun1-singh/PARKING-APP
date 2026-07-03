import uuid
import random
from appium.webdriver.common.appiumby import AppiumBy
from .common import wait_for_element, handle_permission_dialog

def generate_unique_username():
    return f"testuser_{uuid.uuid4().hex[:8]}"

def generate_unique_email():
    return f"testuser_{uuid.uuid4().hex[:8]}@example.com"

def generate_unique_phone():
    return f"9{random.randint(100000000, 999999999)}"

def login(driver, email, password, expect_success=True):
    """
    Handles the login process.

    Args:
        driver: The Appium driver instance.
        email: The email to enter.
        password: The password to enter.
        expect_success (bool): If True, will wait for the home screen to appear
                             after login. If False, it will not.
    """
    email_input = wait_for_element(driver, (AppiumBy.ID, 'etEmail'))
    email_input.clear()
    email_input.send_keys(email)

    password_input = wait_for_element(driver, (AppiumBy.ID, 'etPassword'))
    password_input.clear()
    password_input.send_keys(password)

    submit_button = wait_for_element(driver, (AppiumBy.ID, 'btnLogin'))
    submit_button.click()

    # If we expect the login to be successful, wait for the next screen.
    if expect_success:
        handle_permission_dialog(driver)
        # Wait for an element that only exists on the Home screen
        wait_for_element(driver, (AppiumBy.ID, 'topAppBar'), timeout=15)


def register_user(driver, name, email, password, phone, address):
    wait_for_element(driver, (AppiumBy.ID, 'etName')).send_keys(name)
    wait_for_element(driver, (AppiumBy.ID, 'etEmail')).send_keys(email)
    wait_for_element(driver, (AppiumBy.ID, 'etPassword')).send_keys(password)
    wait_for_element(driver, (AppiumBy.ID, 'etPhone')).send_keys(phone)
    wait_for_element(driver, (AppiumBy.ID, 'etAddress')).send_keys(address)

    wait_for_element(driver, (AppiumBy.ID, 'btnRegister')).click()
    handle_permission_dialog(driver)
    wait_for_element(driver, (AppiumBy.ID, 'btnLogin'), timeout=15)
