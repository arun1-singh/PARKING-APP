from appium.webdriver.common.appiumby import AppiumBy
from tests.common import wait_for_element, assert_element_is_visible
from tests.auth_helpers import (
    register_user, login,
    generate_unique_email, generate_unique_phone,
    REGISTER_NAME, REGISTER_PASSWORD, REGISTER_ADDRESS
)
import time

def test_bottom_nav_bar_navigation(driver):
    """
    Tests that the bottom navigation bar correctly switches between all screens.
    Steps:
    1. Register and log in so we have a valid session.
    2. Tap the 'Sessions' icon and verify the Sessions screen loads.
    3. Tap the 'Bookings' icon and verify the Bookings screen loads.
    4. Tap the 'Profile' icon and verify the Profile screen loads.
    5. Tap the 'Home' icon and verify the app returns to the Home screen.
    """
    # 1. Register a fresh user so this test is self-contained
    unique_email = generate_unique_email()
    unique_phone = generate_unique_phone()

    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    register_user(driver, REGISTER_NAME, unique_email, REGISTER_PASSWORD, unique_phone, REGISTER_ADDRESS)
    # register_user leaves us on the Login screen; now log in
    login(driver, unique_email, REGISTER_PASSWORD)
    time.sleep(2)  # Allow home screen to settle

    # --- 2. Navigate to Sessions and Verify ---
    sessions_nav_locator = (AppiumBy.ID, 'com.example.visionpark:id/nav_sessions')
    wait_for_element(driver, sessions_nav_locator).click()
    time.sleep(1)  # Allow for screen transition

    # Look for a title element containing "Session"
    sessions_title_locator = (AppiumBy.XPATH, "//*[contains(@text, 'Session')]")
    assert_element_is_visible(driver, sessions_title_locator)

    # --- 3. Navigate to Bookings and Verify ---
    bookings_nav_locator = (AppiumBy.ID, 'com.example.visionpark:id/nav_bookings')
    wait_for_element(driver, bookings_nav_locator).click()
    time.sleep(1)

    bookings_title_locator = (AppiumBy.XPATH, "//*[contains(@text, 'My Bookings') or contains(@text, 'Bookings')]")
    assert_element_is_visible(driver, bookings_title_locator)

    # --- 4. Navigate to Profile and Verify ---
    profile_nav_locator = (AppiumBy.ID, 'com.example.visionpark:id/nav_profile')
    wait_for_element(driver, profile_nav_locator).click()
    time.sleep(1)

    # Verify the Profile screen by looking for the logged-in email
    profile_email_locator = (AppiumBy.XPATH, f"//*[contains(@text, '{unique_email}')]")
    assert_element_is_visible(driver, profile_email_locator)

    # --- 5. Navigate back to Home and Verify ---
    home_nav_locator = (AppiumBy.ID, 'com.example.visionpark:id/nav_home')
    wait_for_element(driver, home_nav_locator).click()
    time.sleep(1)

    # Verify we are back on the Home screen
    map_locator = (AppiumBy.ID, 'mapFragment')
    assert_element_is_visible(driver, map_locator)
