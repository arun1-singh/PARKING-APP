import pytest
import time
from appium.webdriver.common.appiumby import AppiumBy
from tests.common import wait_for_element, assert_element_is_visible
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD

def test_burger_menu_and_bottom_nav_lead_to_same_sessions_screen(driver):
    """
    Tests that the bottom nav and burger menu lead to the same "Sessions" screen.
    Steps:
    1. Log in to get to the home screen.
    2. Tap the 'Sessions' icon in the bottom nav and verify the screen.
    3. Go back to the home screen.
    4. Open the burger menu, tap 'My Sessions', and verify the same screen loads.
    """
    # 1. Log in to get to the home screen
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)
    time.sleep(2) # Allow home screen to settle

    # Define a reusable locator for the "Sessions" screen title
    sessions_title_locator = (AppiumBy.XPATH, "//*[contains(@text, 'Session')]")

    # --- 2. Navigate via Bottom Nav and Verify ---
    sessions_nav_locator = (AppiumBy.ID, "nav_sessions")
    wait_for_element(driver, sessions_nav_locator).click()
    time.sleep(1) # Allow for screen transition

    # Verify the "My Sessions" screen is visible
    assert_element_is_visible(driver, sessions_title_locator)

    # --- 3. Go Back Home ---
    driver.back()
    time.sleep(1)
    # Verify we are back on the Home screen by looking for the map fragment
    map_locator = (AppiumBy.ID, "mapFragment")
    assert_element_is_visible(driver, map_locator)

    # --- 4. Navigate via Burger Menu and Verify ---
    burger_menu_locator = (AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer")
    wait_for_element(driver, burger_menu_locator).click()
    time.sleep(1) # Allow for slide-in animation

    # Find and click 'My Sessions' in the navigation drawer
    # Target the drawer item specifically using the parent layout to avoid ambiguity
    drawer_sessions_locator = (AppiumBy.XPATH, "//*[@resource-id='com.example.visionpark:id/navigation_view']//*[@resource-id='com.example.visionpark:id/nav_sessions']")
    wait_for_element(driver, drawer_sessions_locator).click()
    time.sleep(1) # Allow for screen transition

    # Verify the "My Sessions" screen is visible again
    assert_element_is_visible(driver, sessions_title_locator)
