package com.example.visionpark;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.visionpark.activities.ParkingLotDetailsActivity;
import com.example.visionpark.models.ParkingLot;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * UI tests for ParkingLotDetailsActivity
 */
@RunWith(AndroidJUnit4.class)
public class ParkingLotDetailsActivityUITest {

    private Intent createTestIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        // Create test parking lot object
        ParkingLot parkingLot = new ParkingLot(
            1,                                  // id
            "Test Parking Lot",                 // name
            28.6139,                            // latitude
            77.2090,                            // longitude
            "₹50/hr",                           // carFee
            "₹20/hr",                           // twoWheelerFee
            15,                                 // availableCarSlots
            20,                                 // totalCarSlots
            8,                                  // availableTwoWheelerSlots
            10,                                 // totalTwoWheelerSlots
            "Cash, Card, UPI"                   // paymentMode
        );
        parkingLot.setAddress("123 Test Street, Test City");
        parkingLot.setDistance(1.5);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        return intent;
    }

    @Test
    public void testParkingLotDetailsActivityLaunch() {
        // Test that ParkingLotDetailsActivity launches successfully with test data
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the main components are displayed
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.tvParkingLotName)).check(matches(isDisplayed()));
            onView(withId(R.id.btnParkVehicle)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testParkingLotInformationDisplay() {
        // Test that parking lot information is displayed correctly
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify parking lot name
            onView(withId(R.id.tvParkingLotName)).check(matches(withText("Test Parking Lot")));
            
            // Verify address
            onView(withId(R.id.tvParkingLotAddress)).check(matches(withText("123 Test Street, Test City")));
            
            // Verify distance
            onView(withId(R.id.tvDistance)).check(matches(withText(containsString("1.5 km"))));
            
            // Verify pricing information
            onView(withId(R.id.tvCarPricing)).check(matches(withText(containsString("₹50/hr"))));
            onView(withId(R.id.tvTwoWheelerPricing)).check(matches(withText(containsString("₹20/hr"))));
            
            // Verify capacity information
            onView(withId(R.id.tvCarCapacity)).check(matches(withText(containsString("15 available out of 20"))));
            onView(withId(R.id.tvTwoWheelerCapacity)).check(matches(withText(containsString("8 available out of 10"))));
            
            // Verify payment modes
            onView(withId(R.id.tvPaymentModes)).check(matches(withText(containsString("Cash, Card, UPI"))));
        }
    }

    @Test
    public void testAvailabilityStatusDisplay() {
        // Test availability status display for available parking
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify availability status shows "Available" (since we have good availability)
            onView(withId(R.id.tvAvailabilityStatus)).check(matches(withText(containsString("Available"))));
            
            // Verify Park Vehicle button is enabled
            onView(withId(R.id.btnParkVehicle)).check(matches(isEnabled()));
            onView(withId(R.id.btnParkVehicle)).check(matches(withText("Park Vehicle")));
        }
    }

    @Test
    public void testFullParkingLotDisplay() {
        // Test display when parking lot is full
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        // Create parking lot with no available slots
        ParkingLot parkingLot = new ParkingLot(
            1, "Test Parking Lot", 28.6139, 77.2090,
            "₹50/hr", "₹20/hr",
            0, 20,  // No car slots available
            0, 10,  // No two-wheeler slots available
            "Cash, Card, UPI"
        );
        parkingLot.setAddress("123 Test Street, Test City");
        parkingLot.setDistance(1.5);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify availability status shows "Full"
            onView(withId(R.id.tvAvailabilityStatus)).check(matches(withText(containsString("Full"))));
            
            // Verify Park Vehicle button shows "Parking Full" and is disabled
            onView(withId(R.id.btnParkVehicle)).check(matches(withText("Parking Full")));
        }
    }

    @Test
    public void testLimitedAvailabilityDisplay() {
        // Test display when parking lot has limited availability
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        // Create parking lot with limited availability
        ParkingLot parkingLot = new ParkingLot(
            1, "Test Parking Lot", 28.6139, 77.2090,
            "₹50/hr", "₹20/hr",
            2, 20,  // Limited car slots
            1, 10,  // Limited two-wheeler slots
            "Cash, Card, UPI"
        );
        parkingLot.setAddress("123 Test Street, Test City");
        parkingLot.setDistance(1.5);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify availability status shows "Limited"
            onView(withId(R.id.tvAvailabilityStatus)).check(matches(withText(containsString("Limited"))));
            
            // Verify Park Vehicle button is still enabled
            onView(withId(R.id.btnParkVehicle)).check(matches(isEnabled()));
            onView(withId(R.id.btnParkVehicle)).check(matches(withText("Park Vehicle")));
        }
    }

    @Test
    public void testFreeParkingDisplay() {
        // Test display for free parking
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        ParkingLot parkingLot = new ParkingLot(
            1, "Test Parking Lot", 28.6139, 77.2090,
            "Free", "Free",
            15, 20, 8, 10,
            "Cash, Card, UPI"
        );
        parkingLot.setAddress("123 Test Street, Test City");
        parkingLot.setDistance(1.5);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify free parking is displayed
            onView(withId(R.id.tvCarPricing)).check(matches(withText(containsString("Free"))));
            onView(withId(R.id.tvTwoWheelerPricing)).check(matches(withText(containsString("Free"))));
        }
    }

    @Test
    public void testParkVehicleButtonClick() {
        // Test Park Vehicle button functionality
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Click the Park Vehicle button
            onView(withId(R.id.btnParkVehicle)).perform(click());
            
            // Note: In the current implementation, this shows a toast message
            // In a real test, we would verify navigation to VehicleListActivity
        }
    }

    @Test
    public void testToolbarBackNavigation() {
        // Test toolbar back navigation
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify toolbar is displayed with back button
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
            
            // Note: Testing actual back navigation would require more complex setup
            // This test verifies the toolbar is present
        }
    }

    @Test
    public void testAllInformationCardsAreVisible() {
        // Test that all information cards are visible
        Intent intent = createTestIntent();
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify all information sections are displayed
            onView(withId(R.id.ivParkingLotImage)).check(matches(isDisplayed()));
            onView(withId(R.id.tvOperatingHours)).check(matches(isDisplayed()));
            onView(withId(R.id.tvCarPricing)).check(matches(isDisplayed()));
            onView(withId(R.id.tvTwoWheelerPricing)).check(matches(isDisplayed()));
            onView(withId(R.id.tvCarCapacity)).check(matches(isDisplayed()));
            onView(withId(R.id.tvTwoWheelerCapacity)).check(matches(isDisplayed()));
            onView(withId(R.id.tvPaymentModes)).check(matches(isDisplayed()));
            onView(withId(R.id.tvAvailabilityStatus)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testIntegerIdHandling() {
        // Test that activity handles Integer parking lot ID correctly
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        // Pass parking lot as object with Integer ID
        ParkingLot parkingLot = new ParkingLot(
            123, "Integer ID Test Lot", 28.6139, 77.2090,
            "₹30/hr", "₹15/hr",
            10, 15, 5, 8,
            "Cash"
        );
        parkingLot.setAddress("Test Address");
        parkingLot.setDistance(2.0);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the activity launches successfully with Integer ID
            onView(withId(R.id.tvParkingLotName)).check(matches(withText("Integer ID Test Lot")));
            onView(withId(R.id.tvParkingLotAddress)).check(matches(withText("Test Address")));
            onView(withId(R.id.btnParkVehicle)).check(matches(isDisplayed()));
            onView(withId(R.id.btnParkVehicle)).check(matches(isEnabled()));
        }
    }

    @Test
    public void testStringIdHandling() {
        // Test that activity handles parking lot correctly (ID is always int in ParkingLot model)
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParkingLotDetailsActivity.class);
        
        // Pass parking lot as object
        ParkingLot parkingLot = new ParkingLot(
            456, "String ID Test Lot", 28.7041, 77.1025,
            "₹40/hr", "₹20/hr",
            8, 12, 3, 6,
            "UPI"
        );
        parkingLot.setAddress("Test Address 2");
        parkingLot.setDistance(1.2);
        
        intent.putExtra(ParkingLotDetailsActivity.EXTRA_PARKING_LOT, parkingLot);
        
        try (ActivityScenario<ParkingLotDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the activity launches successfully
            onView(withId(R.id.tvParkingLotName)).check(matches(withText("String ID Test Lot")));
            onView(withId(R.id.tvParkingLotAddress)).check(matches(withText("Test Address 2")));
            onView(withId(R.id.btnParkVehicle)).check(matches(isDisplayed()));
            onView(withId(R.id.btnParkVehicle)).check(matches(isEnabled()));
        }
    }
}