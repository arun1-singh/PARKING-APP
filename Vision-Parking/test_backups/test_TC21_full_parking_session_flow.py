import time
from appium.webdriver.common.appiumby import AppiumBy
from tests.common import wait_for_element, assert_element_is_visible, handle_permission_dialog
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD


def test_full_parking_session_flow(driver):
    """
    Test Case TC21: Complete Parking Session Flow
    ---------------------------------------------------
    This test performs the entire user flow:

    1.  Login and navigate to the map.
    2.  Interact with a parking pin to see lot details.
    3.  Start the parking process and create a new vehicle.
    4.  Verify the vehicle is created and selected.
    5.  Navigate to 'My Sessions' and find the created session.
    6.  Find the specific 'Exit Vehicle' button for that session and click it.
    7.  Confirm the exit and payment to complete the flow.
    """

    # --- Step 1: Login ---
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)
    time.sleep(3)

    handle_permission_dialog(driver, timeout=3)
    time.sleep(2)

    # --- Step 2: Ensure map is loaded ---
    map_fragment = (AppiumBy.ID, 'mapFragment')
    assert_element_is_visible(driver, map_fragment)
    print("✓ Map is displayed")
    time.sleep(6)  # Let map pins load

    # --- Step 3: Try to interact with map pins ---
    # Get map element for tapping
    map_element = wait_for_element(driver, (AppiumBy.ID, 'mapFragment'))
    
    # Try multiple approaches to click on a parking pin
    pin_clicked = False
    
    # Approach 1: Try to find and click a marker by content description
    try:
        # Try different marker descriptions
        marker_descriptions = [
            "//android.view.View[contains(@content-desc, 'Marker')]",
            "//android.view.View[contains(@content-desc, 'marker')]",
            "//android.view.View[contains(@content-desc, 'Parking')]",
            "//android.view.View[contains(@content-desc, 'parking')]"
        ]
        
        for desc in marker_descriptions:
            try:
                pin = driver.find_element(AppiumBy.XPATH, desc)
                pin.click()
                print(f"✓ Parking pin clicked using: {desc}")
                pin_clicked = True
                time.sleep(2)
                break
            except:
                continue
    except:
        pass
    
    # Approach 2: Tap on map center (where pins likely are)
    if not pin_clicked:
        try:
            size = driver.get_window_size()
            # Tap slightly above center where parking pins typically appear
            tap_x = size['width'] // 2
            tap_y = size['height'] // 3
            
            driver.tap([(tap_x, tap_y)])
            print(f"✓ Tapped map at coordinates ({tap_x}, {tap_y})")
            time.sleep(2)
            pin_clicked = True
        except:
            print("⚠ Could not tap on map")
    
    # Approach 3: Try tapping multiple locations on the map
    if not pin_clicked:
        try:
            size = driver.get_window_size()
            # Try tapping at different locations where pins might be
            locations = [
                (size['width'] // 2, size['height'] // 3),
                (size['width'] // 3, size['height'] // 2),
                (size['width'] * 2 // 3, size['height'] // 2),
            ]
            
            for x, y in locations:
                driver.tap([(x, y)])
                time.sleep(1)
                print(f"✓ Tapped map at ({x}, {y})")
                
                # Check if any dialog or bottom sheet appeared
                try:
                    # Look for any parking lot name or details
                    parking_info = driver.find_element(
                        AppiumBy.XPATH,
                        "//*[contains(@text, 'Parking') or contains(@text, 'parking')]"
                    )
                    if parking_info.is_displayed():
                        print("✓ Parking lot info appeared after tap")
                        pin_clicked = True
                        break
                except:
                    continue
        except:
            pass
    
    time.sleep(3)

    # Step 4: After clicking pin, scroll down and click "Park Vehicle" ---
    if pin_clicked:
        print("✓ Successfully interacted with parking pin")
        time.sleep(3)
        
        # Scroll down to find "Park Vehicle" button
        size = driver.get_window_size()
        for scroll_attempt in range(3):
            try:
                # Look for "Park Vehicle" button
                park_vehicle_button = driver.find_element(
                    AppiumBy.XPATH,
                    "//*[contains(@text, 'Park Vehicle') or contains(@text, 'park vehicle')]"
                )
                if park_vehicle_button.is_displayed():
                    park_vehicle_button.click()
                    print("✓ Clicked 'Park Vehicle' button")
                    time.sleep(2)
                    break
            except:
                # Scroll down
                driver.swipe(size['width']//2, size['height']*0.7, size['width']//2, size['height']*0.3, 500)
                time.sleep(1)
                print(f"✓ Scrolled down (attempt {scroll_attempt + 1})")

    # --- Step 5: Add a new vehicle ---
    try:
        add_vehicle_button = wait_for_element(driver, (AppiumBy.ID, 'fabAddVehicle'), timeout=5)
        add_vehicle_button.click()
        print("✓ Clicked '+' button to add a new vehicle")
        time.sleep(2)
    except Exception as e:
        print(f"❌ Could not find the add vehicle button. Test cannot continue. Error: {e}")
        raise e

    # --- Step 6: Fill vehicle information form ---
    size = driver.get_window_size()
    unique_reg = f"PARK{int(time.time()) % 10000}"
    
    wait_for_element(driver, (AppiumBy.ID, 'etRegistrationNumber')).send_keys(unique_reg)
    wait_for_element(driver, (AppiumBy.ID, 'etVehicleName')).send_keys("E2E Test Car")
    wait_for_element(driver, (AppiumBy.ID, 'etMake')).send_keys("TestMake")
    wait_for_element(driver, (AppiumBy.ID, 'etModel')).send_keys("TestModel")
    wait_for_element(driver, (AppiumBy.ID, 'etYear')).send_keys("2024")
    
    # Select vehicle type
    wait_for_element(driver, (AppiumBy.ID, 'actvVehicleType')).click()
    time.sleep(0.5)
    wait_for_element(driver, (AppiumBy.XPATH, "//*[@text='Car']"), timeout=3).click()
    print("✓ Filled basic vehicle info")

    # Scroll down to fill the rest
    driver.swipe(size['width']//2, size['height']*0.7, size['width']//2, size['height']*0.3, 500)
    time.sleep(1)
    
    wait_for_element(driver, (AppiumBy.ID, 'etColor')).send_keys("Silver")
    print("✓ Entered color")

    wait_for_element(driver, (AppiumBy.ID, 'btnSaveVehicle')).click()
    print("✓ Clicked Save Vehicle button")
    time.sleep(3)

    # --- Step 7: Handle success dialog and select the vehicle ---
    try:
        wait_for_element(driver, (AppiumBy.XPATH, "//*[@text='OK' or @text='Ok']"), timeout=5).click()
        print("✓ Clicked OK on success dialog")
        time.sleep(2)
    except:
        print("⚠ OK button not found, dialog may have auto-dismissed.")

    # Select the newly created vehicle from the list
    try:
        created_vehicle = wait_for_element(driver, (AppiumBy.XPATH, f"//*[contains(@text, '{unique_reg}')]"))
        created_vehicle.click()
        print(f"✓ Selected the new vehicle: {unique_reg}")
        time.sleep(2)
    except Exception as e:
        print(f"❌ Could not select the newly created vehicle from the list. Error: {e}")
        raise e

    # --- Step 8: Navigate to My Sessions ---
    print("\n✓ Navigating to My Sessions...")
    try:
        sessions_nav = wait_for_element(driver, (AppiumBy.ID, 'nav_sessions'), timeout=5)
        sessions_nav.click()
        print("✓ Clicked 'My Sessions' in the bottom navigation.")
        time.sleep(5)  # Wait for sessions to load
    except Exception as e:
        print(f"❌ Could not navigate to My Sessions. Test cannot continue. Error: {e}")
        raise e

    # --- Step 9: Find and Exit the Specific Session ---
    exit_button_clicked = False
    # Try scrolling up to 5 times to find the session card
    for i in range(5):
        try:
            # Precise XPath: Find the button INSIDE the ViewGroup that contains the unique registration text
            specific_exit_xpath = (
                f"//android.view.ViewGroup[descendant::*[contains(@text, '{unique_reg}')]]"
                f"//android.widget.Button[contains(@text, 'Exit Vehicle')]"
            )
            exit_button = wait_for_element(driver, (AppiumBy.XPATH, specific_exit_xpath), timeout=5)
            
            print(f"✓ Found 'Exit Vehicle' button for {unique_reg} on screen.")
            exit_button.click()
            print(f"✓ ✓ ✓ Clicked 'Exit Vehicle' button for {unique_reg}")
            exit_button_clicked = True
            break  # Exit the loop since we clicked the button
        except Exception:
            print(f"⚠ Session for '{unique_reg}' not found on screen, scrolling... (Attempt {i+1})")
            driver.swipe(size['width']//2, size['height']*0.8, size['width']//2, size['height']*0.2, 500)
            time.sleep(1)

    # --- CRITICAL VERIFICATION ---
    # If the button was never clicked after all attempts, fail the test now.
    if not exit_button_clicked:
        raise Exception(f"❌ TEST FAILED: Could not find and click the 'Exit Vehicle' button for '{unique_reg}' after multiple scroll attempts.")

    # --- Step 10: Confirm Exit and Pay Dialog ---
    try:
        print("✓ Waiting for 'Exit & Pay' dialog...")
        exit_pay_button = wait_for_element(
            driver,
            (AppiumBy.XPATH, "//*[contains(@text, 'Exit') and (contains(@text, 'Pay') or contains(@text, '& Pay'))]"),
            timeout=10
        )
        print("✓ 'Exit & Pay' dialog appeared. Clicking...")
        exit_pay_button.click()

        # --- Step 11: Final Confirmation ---
        print("✓ Waiting for final confirmation...")
        time.sleep(3) # Wait for processing
        ok_button = wait_for_element(driver, (AppiumBy.XPATH, "//*[@text='OK' or @text='Ok']"), timeout=15)
        ok_button.click()
        print("✓ Clicked final OK button.")

    except Exception as e:
        print("\n" + "!"*60)
        print("❌ FAIL: The 'Exit & Pay' dialog did not appear or the final confirmation failed.")
        print(f"Error details: {e}")
        print("!"*60 + "\n")
        raise e # Re-raise the exception to fail the test

    # --- Final Success Message ---
    print("\n" + "="*60)
    print("✓ ✓ ✓ SESSION COMPLETED AND VERIFIED SUCCESSFULLY!")
    print("="*60 + "\n")
