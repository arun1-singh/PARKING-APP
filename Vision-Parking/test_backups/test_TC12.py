from appium.webdriver.common.appiumby import AppiumBy
from tests.common import wait_for_element, assert_element_is_visible
from tests.auth_helpers import login
from tests.constants import REGISTER_EMAIL, REGISTER_PASSWORD
import time

def test_map_search_updates_location(driver):
    """
    Tests that searching for a location updates the map view and the search bar text.
    Steps:
    1. Log in with the known, pre-registered user.
    2. Tap on the search bar to open the Places Autocomplete screen.
    3. Type "Red Fort" into the search input field.
    4. Select the first result from the list.
    5. Verify that the search bar on the home screen now contains the selected location's name.
    """
    # 1. Log in to get to the home screen
    wait_for_element(driver, (AppiumBy.ID, 'btnGetStarted')).click()
    login(driver, REGISTER_EMAIL, REGISTER_PASSWORD)

    # 2. Tap on the search fragment to open the autocomplete search activity
    search_fragment_locator = (AppiumBy.ID, "autocomplete_fragment")
    wait_for_element(driver, search_fragment_locator).click()

    # Add a brief pause to ensure the new activity's animation is complete.
    time.sleep(2)

    # 3. Type a location into the search input field on the new screen.
    search_input_locator = (AppiumBy.CLASS_NAME, "android.widget.EditText")
    search_location = "Red Fort"
    search_box = wait_for_element(driver, search_input_locator)
    search_box.send_keys(search_location)

    # 4. Select the first search result from the list that appears.
    first_result_locator = (AppiumBy.XPATH, f"//android.widget.TextView[contains(@text, 'Red Fort')]")
    wait_for_element(driver, first_result_locator).click()

    # Add a brief pause for the map to settle after returning from search.
    time.sleep(2)

    # 5. Verify that the search bar now contains the location's name.
    # This specifically looks for text inside the autocomplete fragment.
    # This is the correct check based on your screenshot.
    # It finds the search bar by its ID and then looks for any text containing "Red Fort" within it.
    final_search_text_locator = (AppiumBy.XPATH, "//*[@resource-id='com.example.visionpark:id/autocomplete_fragment']//*[contains(@text, 'Red Fort')]")
    assert_element_is_visible(driver, final_search_text_locator)
