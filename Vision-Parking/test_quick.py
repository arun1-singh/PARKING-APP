import pytest
from selenium.common.exceptions import WebDriverException
from appium import webdriver
from appium.options.android import UiAutomator2Options

caps = {
    "platformName": "Android",
    "deviceName": "emulator-5554",
    "automationName": "UiAutomator2",
    "appPackage": "com.example.visionpark",
    "appActivity": ".activities.LoginActivity",
}
options = UiAutomator2Options().load_capabilities(caps)

try:
    driver = webdriver.Remote("http://localhost:4723/wd/hub", options=options)
    print("Connected!")
    driver.quit()
except WebDriverException as e:
    pytest.skip(f"Appium server not available: {e}", allow_module_level=True)
