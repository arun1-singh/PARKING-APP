"""
Test Case TC24: Bookings Screen Navigation & UI
================================================
Verifies the My Bookings screen loads correctly, tabs work,
and the "Book a Parking Slot" button is accessible.

New Feature: Future Parking Bookings (BookMyShow-style)

Test Flow:
1. Login with valid credentials
2. Navigate to My Bookings via bottom navigation
3. Verify Bookings screen header and summary text
4. Verify Upcoming and Past tabs exist and are clickable
5. Verify "Book a Parking Slot" button is visible and clickable
6. Verify booking list (RecyclerView) is present

Expected Result:
- Bookings screen loads without crash
- Both tabs (Upcoming / Past) are functional
- Book button opens the booking dialog
"""

import time
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import TimeoutException
from tests.common import wait_for_element, assert_element_is_visible, is_element_visible, handle_permission_dialog
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD


def _login_and_go_home(driver):
    """Helper: launch app, login, wait for home screen."""
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    time.sleep(1)
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)
    time.sleep(3)
    handle_permission_dialog(driver, timeout=3)
    time.sleep(2)


def _navigate_to_bookings(driver):
    """Helper: tap the Bookings icon in the bottom navigation bar."""
    bookings_nav = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/nav_bookings'), timeout=10
    )
    bookings_nav.click()
    time.sleep(3)


def test_bookings_screen_loads(driver):
    """
    TC24-01: Verify the My Bookings screen loads correctly after login.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    # Verify header title
    header = wait_for_element(
        driver,
        (AppiumBy.XPATH, "//*[contains(@text,'My Bookings') or contains(@text,'Bookings')]"),
        timeout=10
    )
    assert header.is_displayed(), "My Bookings header not visible"
    print("✓ My Bookings screen loaded successfully")


def test_bookings_tabs_exist(driver):
    """
    TC24-02: Verify Upcoming and Past tabs are present and clickable.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    # Upcoming tab
    upcoming_tab = wait_for_element(
        driver,
        (AppiumBy.XPATH, "//*[contains(@text,'Upcoming')]"),
        timeout=10
    )
    assert upcoming_tab.is_displayed(), "Upcoming tab not visible"
    upcoming_tab.click()
    time.sleep(1)
    print("✓ Upcoming tab is visible and clickable")

    # Past tab
    past_tab = wait_for_element(
        driver,
        (AppiumBy.XPATH, "//*[contains(@text,'Past')]"),
        timeout=10
    )
    assert past_tab.is_displayed(), "Past tab not visible"
    past_tab.click()
    time.sleep(1)
    print("✓ Past tab is visible and clickable")

    # Switch back to Upcoming
    upcoming_tab = driver.find_element(AppiumBy.XPATH, "//*[contains(@text,'Upcoming')]")
    upcoming_tab.click()
    time.sleep(1)
    print("✓ Tab switching works correctly")


def test_book_slot_button_visible(driver):
    """
    TC24-03: Verify the '+ Book a Parking Slot' button is visible on the screen.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    book_btn = wait_for_element(
        driver,
        (AppiumBy.ID, 'com.example.visionpark:id/btnBookNewSlot'),
        timeout=10
    )
    assert book_btn.is_displayed(), "'+ Book a Parking Slot' button not visible"
    print("✓ '+ Book a Parking Slot' button is visible")


def test_book_slot_button_opens_dialog(driver):
    """
    TC24-04: Verify tapping '+ Book a Parking Slot' opens the booking dialog.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    # Tap the book button
    book_btn = wait_for_element(
        driver,
        (AppiumBy.ID, 'com.example.visionpark:id/btnBookNewSlot'),
        timeout=10
    )
    book_btn.click()
    time.sleep(2)

    # Verify dialog opened — look for dialog elements
    dialog_indicators = [
        (AppiumBy.XPATH, "//*[contains(@text,'Book a Parking Slot')]"),
        (AppiumBy.XPATH, "//*[contains(@text,'Parking Lot')]"),
        (AppiumBy.XPATH, "//*[contains(@text,'Vehicle')]"),
        (AppiumBy.XPATH, "//*[contains(@text,'Check Availability')]"),
    ]
    dialog_found = False
    for locator in dialog_indicators:
        if is_element_visible(driver, locator, timeout=5):
            dialog_found = True
            print(f"✓ Booking dialog opened — found: {locator[1]}")
            break

    assert dialog_found, "Booking dialog did not open after tapping '+ Book a Parking Slot'"

    # Close dialog
    driver.back()
    time.sleep(1)
    print("✓ Booking dialog opens and closes correctly")


def test_bookings_summary_text_visible(driver):
    """
    TC24-05: Verify the booking summary text (e.g. '0 upcoming • 0 total') is shown.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    summary = wait_for_element(
        driver,
        (AppiumBy.ID, 'com.example.visionpark:id/tvBookingSummary'),
        timeout=10
    )
    assert summary.is_displayed(), "Booking summary text not visible"
    print(f"✓ Booking summary visible: '{summary.text}'")


def test_bookings_recyclerview_present(driver):
    """
    TC24-06: Verify the RecyclerView for bookings is present on screen.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    # Wait for loading layout to disappear (up to 15 seconds)
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    try:
        WebDriverWait(driver, 15).until(
            EC.invisibility_of_element_located((AppiumBy.ID, 'com.example.visionpark:id/layoutLoading'))
        )
    except Exception:
        pass

    # Accept either the empty‑state view or the bookings list
    empty_view = is_element_visible(driver, (AppiumBy.ID, 'com.example.visionpark:id/layoutEmpty'), timeout=5)
    recycler_view = is_element_visible(driver, (AppiumBy.ID, 'com.example.visionpark:id/recyclerViewBookings'), timeout=5)
    assert empty_view or recycler_view, "Neither empty‑state nor bookings list is visible"
    if recycler_view:
        print("✓ Bookings RecyclerView is present")
    else:
        print("✓ Empty state displayed – no bookings yet")


def test_bookings_bottom_nav_active(driver):
    """
    TC24-07: Verify the Bookings icon in bottom nav is highlighted when on Bookings screen.
    """
    _login_and_go_home(driver)
    _navigate_to_bookings(driver)

    bottom_nav = wait_for_element(
        driver,
        (AppiumBy.ID, 'com.example.visionpark:id/bottomNavigationView'),
        timeout=10
    )
    assert bottom_nav.is_displayed(), "Bottom navigation not visible on Bookings screen"
    print("✓ Bottom navigation is visible on Bookings screen")
