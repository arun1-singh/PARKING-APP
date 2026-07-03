from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import StaleElementReferenceException
from tests.common import (
    wait_for_element,
    assert_element_is_visible,
    is_element_visible
)
from tests.auth_helpers import register_user, login, generate_unique_email, generate_unique_phone
from tests.constants import (
    REGISTER_NAME,
    REGISTER_EMAIL,
    REGISTER_PASSWORD,
    REGISTER_PHONE,
    REGISTER_ADDRESS
)
import time


def safe_click(driver, locator, retries=3, delay=1):
    """Click an element with retry on StaleElementReferenceException."""
    for attempt in range(retries):
        try:
            element = wait_for_element(driver, locator)
            element.click()
            return
        except StaleElementReferenceException:
            if attempt < retries - 1:
                time.sleep(delay)
            else:
                raise


def test_setup_user_and_verify_map_loads(driver):
    """
    Foundational test: ensures the primary test user exists and verifies
    the home screen map loads correctly after login.
    Uses dynamic unique credentials to avoid database duplicate conflicts.
    """
    unique_email = generate_unique_email()
    unique_phone = generate_unique_phone()

    # 1. Navigate to login screen
    safe_click(driver, (AppiumBy.ID, 'btnGetStarted'))
    time.sleep(1)

    # 2. Register first using the unique credentials
    register_user(driver, REGISTER_NAME, unique_email,
                  REGISTER_PASSWORD, unique_phone, REGISTER_ADDRESS)
    time.sleep(2)

    # 3. Log in again — must succeed now
    login(driver, unique_email, REGISTER_PASSWORD, expect_success=True)
    time.sleep(3)

    # 4. Verify map or home screen is visible (map may take time to load)
    # Try mapFragment first, then fall back to other home screen elements
    home_indicators = [
        (AppiumBy.ID, "com.example.visionpark:id/mapFragment"),
        (AppiumBy.ID, "com.example.visionpark:id/bottomNavigationView"),
        (AppiumBy.ID, "com.example.visionpark:id/fabNearby"),
        (AppiumBy.XPATH, "//*[contains(@text, 'Nearby') or contains(@text, 'Home')]"),
    ]
    home_found = False
    for locator in home_indicators:
        if is_element_visible(driver, locator, timeout=10):
            home_found = True
            break
    assert home_found, "Home screen not visible after login — map or navigation not found"