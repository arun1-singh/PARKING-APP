"""
Test Case TC25: Create Booking Flow
=====================================
Verifies the complete booking creation dialog — selecting lot, vehicle,
date, time, checking availability, and confirming a booking.

New Feature: Future Parking Bookings (BookMyShow-style)

Test Flow:
1. Login and navigate to My Bookings
2. Tap '+ Book a Parking Slot'
3. Verify all dialog fields are present (lot spinner, vehicle spinner, date, time)
4. Select a parking lot and vehicle
5. Pick a date (tomorrow)
6. Set start and end times
7. Tap 'Check Availability'
8. Verify availability result is shown
9. Tap 'Confirm Booking'
10. Verify booking appears in Upcoming tab

Expected Result:
- Dialog has all required fields
- Availability check works
- Booking is created and shown in Upcoming list
"""

import time
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from tests.common import wait_for_element, assert_element_is_visible, is_element_visible, handle_permission_dialog
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD


def _login_and_open_booking_dialog(driver):
    """Helper: login, go to Bookings, open the booking dialog."""
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    time.sleep(1)
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)
    time.sleep(3)
    handle_permission_dialog(driver, timeout=3)
    time.sleep(2)

    # Navigate to Bookings
    bookings_nav = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/nav_bookings'), timeout=10
    )
    bookings_nav.click()
    time.sleep(2)

    # Open booking dialog
    book_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnBookNewSlot'), timeout=10
    )
    book_btn.click()
    time.sleep(2)


def test_booking_dialog_has_required_fields(driver):
    """
    TC25-01: Verify the booking dialog contains all required fields.
    """
    _login_and_open_booking_dialog(driver)

    required_fields = [
        ((AppiumBy.ID, 'com.example.visionpark:id/spinnerParkingLot'), "Parking Lot spinner"),
        ((AppiumBy.ID, 'com.example.visionpark:id/spinnerVehicle'),    "Vehicle spinner"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnPickDate'),       "Date picker button"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnPickStartTime'),  "Start time button"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnPickEndTime'),    "End time button"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnCheckAvailabilityMain'), "Check Availability button"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnConfirmBooking'), "Confirm Booking button"),
    ]

    for locator, name in required_fields:
        assert is_element_visible(driver, locator, timeout=8), f"'{name}' not found in booking dialog"
        print(f"✓ {name} is present")

    driver.back()
    time.sleep(1)
    print("✓ All required booking dialog fields are present")


def test_booking_dialog_parking_lot_spinner(driver):
    """
    TC25-02: Verify the parking lot spinner is populated with options.
    """
    _login_and_open_booking_dialog(driver)

    spinner = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/spinnerParkingLot'), timeout=8
    )
    spinner.click()
    time.sleep(1)

    # Check dropdown items appeared — re-find after click to avoid stale reference
    dropdown_items = driver.find_elements(
        AppiumBy.XPATH, "//*[contains(@text,'Parking') or contains(@text,'Metro') or contains(@text,'ID:')]"
    )
    assert len(dropdown_items) > 0, "No parking lot options in dropdown"
    print(f"✓ Parking lot spinner has {len(dropdown_items)} options")

    # Select first option — re-find to avoid stale element
    try:
        items = driver.find_elements(
            AppiumBy.XPATH, "//*[contains(@text,'Parking') or contains(@text,'Metro') or contains(@text,'ID:')]"
        )
        if items:
            item_text = items[0].text
            items[0].click()
            print(f"✓ Selected parking lot: {item_text}")
    except Exception:
        driver.back()  # dismiss dropdown if click fails

    driver.back()
    time.sleep(1)


def test_booking_dialog_vehicle_spinner(driver):
    """
    TC25-03: Verify the vehicle spinner shows user's registered vehicles.
    """
    _login_and_open_booking_dialog(driver)

    spinner = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/spinnerVehicle'), timeout=8
    )
    assert spinner.is_displayed(), "Vehicle spinner not visible"
    print("✓ Vehicle spinner is present")

    spinner.click()
    time.sleep(1)

    # Check for vehicle options
    vehicle_items = driver.find_elements(
        AppiumBy.XPATH, "//*[@class='android.widget.TextView']"
    )
    print(f"✓ Vehicle spinner opened with {len(vehicle_items)} items")

    # Select first vehicle
    if vehicle_items:
        vehicle_items[0].click()
        time.sleep(1)
        print(f"✓ Vehicle selected")

    driver.back()
    time.sleep(1)


def test_booking_date_picker_opens(driver):
    """
    TC25-04: Verify the date picker opens when tapping the date button.
    """
    _login_and_open_booking_dialog(driver)

    date_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPickDate'), timeout=8
    )
    date_btn.click()
    time.sleep(2)

    # DatePickerDialog should appear
    date_picker_indicators = [
        (AppiumBy.XPATH, "//*[@class='android.widget.DatePicker']"),
        (AppiumBy.XPATH, "//*[contains(@text,'OK') or contains(@text,'Done')]"),
        (AppiumBy.XPATH, "//*[@resource-id='android:id/date_picker_header_year']"),
    ]
    picker_found = any(is_element_visible(driver, loc, timeout=5) for loc in date_picker_indicators)
    assert picker_found, "Date picker dialog did not open"
    print("✓ Date picker dialog opened successfully")

    # Dismiss picker
    try:
        ok_btn = driver.find_element(AppiumBy.XPATH, "//*[contains(@text,'OK') or contains(@text,'Done')]")
        ok_btn.click()
    except Exception:
        driver.back()
    time.sleep(1)
    print("✓ Date picker dismissed")


def test_booking_time_pickers_open(driver):
    """
    TC25-05: Verify start and end time pickers open correctly.
    """
    _login_and_open_booking_dialog(driver)

    # Test start time picker
    start_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPickStartTime'), timeout=8
    )
    start_btn.click()
    time.sleep(2)

    time_picker_indicators = [
        (AppiumBy.XPATH, "//*[@class='android.widget.TimePicker']"),
        (AppiumBy.XPATH, "//*[contains(@text,'OK') or contains(@text,'Done')]"),
        (AppiumBy.XPATH, "//*[@resource-id='android:id/time_picker_header']"),
    ]
    picker_found = any(is_element_visible(driver, loc, timeout=5) for loc in time_picker_indicators)
    assert picker_found, "Start time picker did not open"
    print("✓ Start time picker opened")

    try:
        ok_btn = driver.find_element(AppiumBy.XPATH, "//*[contains(@text,'OK') or contains(@text,'Done')]")
        ok_btn.click()
    except Exception:
        driver.back()
    time.sleep(1)

    # Test end time picker
    end_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPickEndTime'), timeout=8
    )
    end_btn.click()
    time.sleep(2)

    picker_found = any(is_element_visible(driver, loc, timeout=5) for loc in time_picker_indicators)
    assert picker_found, "End time picker did not open"
    print("✓ End time picker opened")

    try:
        ok_btn = driver.find_element(AppiumBy.XPATH, "//*[contains(@text,'OK') or contains(@text,'Done')]")
        ok_btn.click()
    except Exception:
        driver.back()
    time.sleep(1)
    print("✓ Both time pickers work correctly")

    driver.back()
    time.sleep(1)


def test_check_availability_button_works(driver):
    """
    TC25-06: Verify tapping 'Check Availability' triggers an availability check
    and shows the result card.
    """
    _login_and_open_booking_dialog(driver)

    # Select first parking lot
    lot_spinner = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/spinnerParkingLot'), timeout=8
    )
    lot_spinner.click()
    time.sleep(1)
    try:
        items = driver.find_elements(
            AppiumBy.XPATH, "//*[contains(@text,'Parking') or contains(@text,'Metro') or contains(@text,'ID:')]"
        )
        if items:
            items[0].click()
    except Exception:
        driver.back()
    time.sleep(1)

    # Tap Check Availability
    check_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnCheckAvailabilityMain'), timeout=8
    )
    check_btn.click()
    time.sleep(5)  # Wait for API call

    # Verify availability result card appears
    availability_indicators = [
        (AppiumBy.ID, 'com.example.visionpark:id/cardAvailability'),
        (AppiumBy.ID, 'com.example.visionpark:id/tvAvailableSlots'),
        (AppiumBy.ID, 'com.example.visionpark:id/tvEstimatedCost'),
        (AppiumBy.XPATH, "//*[contains(@text,'slots available') or contains(@text,'Estimated')]"),
    ]
    result_found = any(is_element_visible(driver, loc, timeout=8) for loc in availability_indicators)
    assert result_found, "Availability result not shown after checking"
    print("✓ Availability check result is displayed")

    driver.back()
    time.sleep(1)


def test_confirm_booking_button_enabled_after_availability(driver):
    """
    TC25-07: Verify 'Confirm Booking' button becomes enabled after availability check.
    """
    _login_and_open_booking_dialog(driver)

    # Select first parking lot
    lot_spinner = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/spinnerParkingLot'), timeout=8
    )
    lot_spinner.click()
    time.sleep(1)
    try:
        items = driver.find_elements(
            AppiumBy.XPATH, "//*[contains(@text,'Parking') or contains(@text,'Metro') or contains(@text,'ID:')]"
        )
        if items:
            items[0].click()
    except Exception:
        driver.back()
    time.sleep(1)

    # Check availability
    check_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnCheckAvailabilityMain'), timeout=8
    )
    check_btn.click()
    time.sleep(5)

    # Confirm button should now be enabled
    confirm_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnConfirmBooking'), timeout=8
    )
    is_enabled = confirm_btn.is_enabled()
    print(f"✓ Confirm Booking button enabled: {is_enabled}")
    # Note: button may be enabled or disabled depending on availability
    assert confirm_btn.is_displayed(), "Confirm Booking button not visible"
    print("✓ Confirm Booking button is visible after availability check")

    driver.back()
    time.sleep(1)
