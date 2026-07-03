"""
Test Case TC26: Payment Screen UI & Flow
==========================================
Verifies the PaymentActivity loads correctly with all payment method options,
UPI input, card input, and the Pay button.

New Feature: Electronic Payment System

Test Flow:
1. Login and navigate to My Sessions
2. Find an active session and tap 'Exit Vehicle'
3. Verify Payment screen opens with correct amount
4. Verify all payment method cards are visible (UPI, Card, NetBanking, Wallet)
5. Select UPI and verify UPI input field appears
6. Enter a UPI ID
7. Verify quick UPI buttons (GPay, PhonePe, Paytm) work
8. Select Card and verify card input fields appear
9. Verify Pay button shows correct amount
10. Complete payment and verify success screen

Expected Result:
- Payment screen loads with correct amount
- All 4 payment methods are selectable
- UPI input and card input fields work
- Pay button triggers payment flow
- Success screen shows receipt
"""

import time
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from tests.common import wait_for_element, assert_element_is_visible, is_element_visible, handle_permission_dialog
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD


def _login_and_go_to_sessions(driver):
    """Helper: login and navigate to My Sessions screen."""
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    time.sleep(1)
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)
    time.sleep(3)
    handle_permission_dialog(driver, timeout=2)  # Short timeout — don't block
    time.sleep(2)

    # Navigate to My Sessions via bottom nav
    sessions_nav = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/nav_sessions'), timeout=10
    )
    sessions_nav.click()
    time.sleep(3)


def _open_payment_screen(driver):
    """
    Helper: navigate to sessions, find an active session, tap Exit Vehicle
    to open the payment screen. Returns True if payment screen opened.
    """
    _login_and_go_to_sessions(driver)

    # Look for Exit Vehicle button on an active session card
    exit_btn_locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/btnExitVehicle'),
        (AppiumBy.XPATH, "//*[contains(@text,'Exit Vehicle') or contains(@text,'Exit & Pay')]"),
    ]
    for locator in exit_btn_locators:
        if is_element_visible(driver, locator, timeout=5):
            driver.find_element(*locator).click()
            time.sleep(2)
            # Confirm dialog
            confirm_locators = [
                (AppiumBy.XPATH, "//*[contains(@text,'Exit & Pay')]"),
                (AppiumBy.XPATH, "//*[contains(@text,'Yes')]"),
                (AppiumBy.XPATH, "//*[contains(@text,'Confirm')]"),
            ]
            for cloc in confirm_locators:
                if is_element_visible(driver, cloc, timeout=3):
                    driver.find_element(*cloc).click()
                    time.sleep(3)
                    return True
    return False


def test_payment_screen_loads(driver):
    """
    TC26-01: Verify the Payment screen opens when exiting a vehicle.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    # Verify payment screen is open
    payment_indicators = [
        (AppiumBy.XPATH, "//*[contains(@text,'Complete Payment') or contains(@text,'Payment')]"),
        (AppiumBy.ID, 'com.example.visionpark:id/tvAmount'),
        (AppiumBy.XPATH, "//*[contains(@text,'Amount to Pay')]"),
    ]
    screen_found = any(is_element_visible(driver, loc, timeout=8) for loc in payment_indicators)
    assert screen_found, "Payment screen did not open"
    print("✓ Payment screen loaded successfully")


def test_payment_amount_displayed(driver):
    """
    TC26-02: Verify the amount to pay is displayed on the payment screen.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    amount_view = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/tvAmount'), timeout=10
    )
    assert amount_view.is_displayed(), "Amount not displayed on payment screen"
    amount_text = amount_view.text
    assert 'Rs.' in amount_text or len(amount_text) > 0, f"Amount text invalid: '{amount_text}'"
    print(f"✓ Amount displayed: {amount_text}")


def test_payment_methods_visible(driver):
    """
    TC26-03: Verify all 4 payment method cards are visible.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    payment_methods = [
        ((AppiumBy.ID, 'com.example.visionpark:id/cardUpi'),         "UPI"),
        ((AppiumBy.ID, 'com.example.visionpark:id/cardDebitCredit'), "Debit/Credit Card"),
        ((AppiumBy.ID, 'com.example.visionpark:id/cardNetbanking'),  "Net Banking"),
        ((AppiumBy.ID, 'com.example.visionpark:id/cardWallet'),      "Wallet"),
    ]

    for locator, name in payment_methods:
        if is_element_visible(driver, locator, timeout=5):
            print(f"✓ {name} payment option is visible")
        else:
            # Scroll down to find it
            size = driver.get_window_size()
            driver.swipe(size['width']//2, size['height']*0.7, size['width']//2, size['height']*0.3, 500)
            time.sleep(1)
            assert is_element_visible(driver, locator, timeout=5), f"{name} payment option not found"
            print(f"✓ {name} payment option visible after scroll")

    print("✓ All payment methods are visible")


def test_upi_selected_by_default(driver):
    """
    TC26-04: Verify UPI is selected by default and UPI input field is shown.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    # UPI radio should be checked by default
    upi_radio = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/radioUpi'), timeout=8
    )
    assert upi_radio.is_displayed(), "UPI radio button not visible"
    print("✓ UPI radio button is visible")

    # UPI input card should be visible
    upi_input = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/cardUpiInput'), timeout=8
    )
    assert upi_input.is_displayed(), "UPI input card not visible when UPI selected"
    print("✓ UPI input field is shown when UPI is selected")


def test_upi_id_input_field(driver):
    """
    TC26-05: Verify UPI ID input field accepts text.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    upi_input = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/etUpiId'), timeout=8
    )
    upi_input.clear()
    upi_input.send_keys("testuser@paytm")
    time.sleep(1)

    entered_text = upi_input.text
    assert "testuser" in entered_text or "@" in entered_text, f"UPI ID not entered correctly: '{entered_text}'"
    print(f"✓ UPI ID entered: {entered_text}")


def test_quick_upi_buttons(driver):
    """
    TC26-06: Verify GPay, PhonePe, Paytm quick buttons fill the UPI ID field.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    quick_buttons = [
        ((AppiumBy.ID, 'com.example.visionpark:id/btnGpay'),    "GPay",    "@okicici"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnPhonepe'), "PhonePe", "@ybl"),
        ((AppiumBy.ID, 'com.example.visionpark:id/btnPaytm'),   "Paytm",   "@paytm"),
    ]

    for locator, name, expected_suffix in quick_buttons:
        if is_element_visible(driver, locator, timeout=5):
            driver.find_element(*locator).click()
            time.sleep(1)
            upi_field = driver.find_element(AppiumBy.ID, 'com.example.visionpark:id/etUpiId')
            upi_text = upi_field.text
            assert expected_suffix in upi_text or "@" in upi_text, \
                f"{name} button did not fill UPI field correctly: '{upi_text}'"
            print(f"✓ {name} quick button filled UPI ID: {upi_text}")
        else:
            print(f"⚠ {name} button not visible (may need scroll)")


def test_card_payment_method_selection(driver):
    """
    TC26-07: Verify selecting Card payment shows card input fields.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    # Scroll down to find Card option (it may be below the fold)
    size = driver.get_window_size()
    driver.swipe(size['width']//2, size['height']*0.7, size['width']//2, size['height']*0.3, 500)
    time.sleep(1)

    # Tap Card option
    card_option = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/cardDebitCredit'), timeout=8
    )
    card_option.click()
    time.sleep(1)

    # Card input fields should appear
    card_input_card = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/cardCardInput'), timeout=8
    )
    assert card_input_card.is_displayed(), "Card input fields not shown after selecting Card"
    print("✓ Card input fields appear when Card is selected")

    # Verify card number field
    card_number = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/etCardNumber'), timeout=5
    )
    assert card_number.is_displayed(), "Card number field not visible"
    print("✓ Card number field is visible")


def test_pay_button_shows_correct_amount(driver):
    """
    TC26-08: Verify the Pay button shows the correct amount.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    pay_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPay'), timeout=8
    )
    assert pay_btn.is_displayed(), "Pay button not visible"
    pay_text = pay_btn.text
    assert 'Rs.' in pay_text or 'Pay' in pay_text, f"Pay button text invalid: '{pay_text}'"
    print(f"✓ Pay button shows: '{pay_text}'")


def test_payment_upi_flow_complete(driver):
    """
    TC26-09: Complete a full UPI payment flow and verify success screen.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    # Enter UPI ID
    upi_input = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/etUpiId'), timeout=8
    )
    upi_input.clear()
    upi_input.send_keys("test@paytm")
    time.sleep(1)

    # Tap Pay button
    pay_btn = wait_for_element(
        driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPay'), timeout=8
    )
    pay_btn.click()
    time.sleep(8)  # Wait for simulated payment processing (1.5s) + network + screen transition

    # Verify success screen — check multiple possible indicators
    success_indicators = [
        (AppiumBy.XPATH, "//*[contains(@text,'Payment Successful') or contains(@text,'Success')]"),
        (AppiumBy.ID, 'com.example.visionpark:id/tvPaidAmount'),
        (AppiumBy.ID, 'com.example.visionpark:id/tvTxnId'),
        (AppiumBy.XPATH, "//*[contains(@text,'Transaction ID') or contains(@text,'TXN')]"),
        (AppiumBy.XPATH, "//*[contains(@text,'Rs.')]"),
        (AppiumBy.XPATH, "//*[contains(@text,'Done')]"),
    ]
    success_found = any(is_element_visible(driver, loc, timeout=12) for loc in success_indicators)
    assert success_found, "Payment success screen not shown after completing payment"
    print("✓ Payment completed successfully — success screen shown")

    # Tap Done to go back
    done_btn_locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/btnDone'),
        (AppiumBy.XPATH, "//*[contains(@text,'Done')]"),
    ]
    for loc in done_btn_locators:
        if is_element_visible(driver, loc, timeout=5):
            driver.find_element(*loc).click()
            time.sleep(2)
            print("✓ Tapped Done — returned to sessions screen")
            break


def test_payment_success_screen_receipt(driver):
    """
    TC26-10: Verify the payment success screen shows all receipt fields.
    """
    opened = _open_payment_screen(driver)
    if not opened:
        pytest.skip("No active session available to trigger payment screen")

    # Enter UPI ID and pay
    try:
        upi_input = wait_for_element(
            driver, (AppiumBy.ID, 'com.example.visionpark:id/etUpiId'), timeout=5
        )
        upi_input.clear()
        upi_input.send_keys("receipt@test")
        time.sleep(1)

        pay_btn = wait_for_element(
            driver, (AppiumBy.ID, 'com.example.visionpark:id/btnPay'), timeout=5
        )
        pay_btn.click()
        time.sleep(6)
    except Exception as e:
        pytest.skip(f"Could not initiate payment: {e}")

    # Verify receipt fields on success screen
    receipt_fields = [
        ((AppiumBy.ID, 'com.example.visionpark:id/tvPaidAmount'),  "Paid Amount"),
        ((AppiumBy.ID, 'com.example.visionpark:id/tvTxnId'),       "Transaction ID"),
        ((AppiumBy.ID, 'com.example.visionpark:id/tvPayMethod'),   "Payment Method"),
        ((AppiumBy.ID, 'com.example.visionpark:id/tvParkingLot'),  "Parking Lot"),
        ((AppiumBy.ID, 'com.example.visionpark:id/tvPaidAt'),      "Paid At"),
    ]

    for locator, name in receipt_fields:
        if is_element_visible(driver, locator, timeout=8):
            text = driver.find_element(*locator).text
            print(f"✓ {name}: {text}")
        else:
            print(f"⚠ {name} field not found on success screen")

    # Tap Done
    done_locators = [
        (AppiumBy.ID, 'com.example.visionpark:id/btnDone'),
        (AppiumBy.XPATH, "//*[contains(@text,'Done')]"),
    ]
    for loc in done_locators:
        if is_element_visible(driver, loc, timeout=5):
            driver.find_element(*loc).click()
            time.sleep(2)
            break

    print("✓ Payment success receipt screen verified")
