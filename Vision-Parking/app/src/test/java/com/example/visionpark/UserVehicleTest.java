package com.example.visionpark;

import com.example.visionpark.models.UserVehicle;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for UserVehicle model class
 * Tests vehicle data handling, display methods, and validation logic
 */
public class UserVehicleTest {
    
    private UserVehicle vehicle;
    
    @Before
    public void setUp() {
        vehicle = new UserVehicle();
    }
    
    @Test
    public void testDefaultConstructor() {
        UserVehicle defaultVehicle = new UserVehicle();
        assertNotNull(defaultVehicle);
        assertEquals(0, defaultVehicle.getVehicleId());
        assertTrue(defaultVehicle.isActive()); // Default should be active
    }
    
    @Test
    public void testConstructorWithEssentialFields() {
        UserVehicle testVehicle = new UserVehicle(1, "ABC-1234", "My Car", "car");
        
        assertEquals(1, testVehicle.getVehicleId());
        assertEquals("ABC-1234", testVehicle.getRegistrationNumber());
        assertEquals("My Car", testVehicle.getVehicleName());
        assertEquals("car", testVehicle.getVehicleType());
        assertTrue(testVehicle.isActive());
    }
    
    @Test
    public void testFullConstructor() {
        UserVehicle testVehicle = new UserVehicle(
            1, "ABC-1234", "My Car", "Toyota", "Camry", 2020, "car", "White"
        );
        
        assertEquals(1, testVehicle.getVehicleId());
        assertEquals("ABC-1234", testVehicle.getRegistrationNumber());
        assertEquals("My Car", testVehicle.getVehicleName());
        assertEquals("Toyota", testVehicle.getMake());
        assertEquals("Camry", testVehicle.getModel());
        assertEquals(Integer.valueOf(2020), testVehicle.getYear());
        assertEquals("car", testVehicle.getVehicleType());
        assertEquals("White", testVehicle.getColor());
        assertTrue(testVehicle.isActive());
    }
    
    @Test
    public void testGetDisplayName() {
        // Test with vehicle name
        vehicle.setVehicleName("My Car");
        vehicle.setRegistrationNumber("ABC-1234");
        
        String displayName = vehicle.getDisplayName();
        assertEquals("My Car (ABC-1234)", displayName);
    }
    
    @Test
    public void testGetDisplayNameWithoutVehicleName() {
        // Test without vehicle name
        vehicle.setVehicleName("");
        vehicle.setRegistrationNumber("ABC-1234");
        
        String displayName = vehicle.getDisplayName();
        assertEquals("ABC-1234", displayName);
    }
    
    @Test
    public void testGetDisplayNameWithNullVehicleName() {
        // Test with null vehicle name
        vehicle.setVehicleName(null);
        vehicle.setRegistrationNumber("ABC-1234");
        
        String displayName = vehicle.getDisplayName();
        assertEquals("ABC-1234", displayName);
    }
    
    @Test
    public void testGetVehicleDetailsComplete() {
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setYear(2020);
        
        String details = vehicle.getVehicleDetails();
        assertEquals("Toyota Camry, 2020", details);
    }
    
    @Test
    public void testGetVehicleDetailsPartial() {
        vehicle.setMake("Toyota");
        vehicle.setYear(2020);
        // No model
        
        String details = vehicle.getVehicleDetails();
        assertEquals("Toyota, 2020", details);
    }
    
    @Test
    public void testGetVehicleDetailsOnlyMake() {
        vehicle.setMake("Toyota");
        
        String details = vehicle.getVehicleDetails();
        assertEquals("Toyota", details);
    }
    
    @Test
    public void testGetVehicleDetailsEmpty() {
        // No make, model, or year
        String details = vehicle.getVehicleDetails();
        assertEquals("Vehicle details not available", details);
    }
    
    @Test
    public void testGetFormattedVehicleType() {
        vehicle.setVehicleType("car");
        assertEquals("Car", vehicle.getFormattedVehicleType());
        
        vehicle.setVehicleType("MOTORCYCLE");
        assertEquals("Motorcycle", vehicle.getFormattedVehicleType());
        
        vehicle.setVehicleType("tWo_WhEeLeR");
        assertEquals("Two_wheeler", vehicle.getFormattedVehicleType());
    }
    
    @Test
    public void testGetFormattedVehicleTypeNull() {
        vehicle.setVehicleType(null);
        assertEquals("Car", vehicle.getFormattedVehicleType());
    }
    
    @Test
    public void testGetFormattedVehicleTypeEmpty() {
        vehicle.setVehicleType("");
        assertEquals("Car", vehicle.getFormattedVehicleType());
    }
    
    @Test
    public void testSettersAndGetters() {
        vehicle.setVehicleId(123);
        assertEquals(123, vehicle.getVehicleId());
        
        vehicle.setRegistrationNumber("XYZ-5678");
        assertEquals("XYZ-5678", vehicle.getRegistrationNumber());
        
        vehicle.setVehicleName("Test Vehicle");
        assertEquals("Test Vehicle", vehicle.getVehicleName());
        
        vehicle.setMake("Honda");
        assertEquals("Honda", vehicle.getMake());
        
        vehicle.setModel("Civic");
        assertEquals("Civic", vehicle.getModel());
        
        vehicle.setYear(2019);
        assertEquals(Integer.valueOf(2019), vehicle.getYear());
        
        vehicle.setVehicleType("motorcycle");
        assertEquals("motorcycle", vehicle.getVehicleType());
        
        vehicle.setColor("Blue");
        assertEquals("Blue", vehicle.getColor());
        
        vehicle.setActive(false);
        assertFalse(vehicle.isActive());
    }
    
    @Test
    public void testToString() {
        vehicle.setVehicleId(1);
        vehicle.setRegistrationNumber("ABC-1234");
        vehicle.setVehicleName("My Car");
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setYear(2020);
        vehicle.setVehicleType("car");
        vehicle.setColor("White");
        vehicle.setActive(true);
        
        String toString = vehicle.toString();
        
        // Verify that toString contains key information
        assertTrue(toString.contains("vehicleId=1"));
        assertTrue(toString.contains("registrationNumber='ABC-1234'"));
        assertTrue(toString.contains("vehicleName='My Car'"));
        assertTrue(toString.contains("make='Toyota'"));
        assertTrue(toString.contains("model='Camry'"));
        assertTrue(toString.contains("year=2020"));
        assertTrue(toString.contains("vehicleType='car'"));
        assertTrue(toString.contains("color='White'"));
        assertTrue(toString.contains("isActive=true"));
    }
    
    @Test
    public void testVehicleEquality() {
        UserVehicle vehicle1 = new UserVehicle(1, "ABC-1234", "My Car", "car");
        UserVehicle vehicle2 = new UserVehicle(1, "ABC-1234", "My Car", "car");
        
        // Note: UserVehicle doesn't override equals(), so this tests object identity
        // In a real implementation, we might want to override equals() and hashCode()
        assertFalse(vehicle1.equals(vehicle2)); // Different objects
        assertTrue(vehicle1.equals(vehicle1)); // Same object
    }
    
    @Test
    public void testVehicleWithSpecialCharacters() {
        vehicle.setRegistrationNumber("KA-01-AB-1234");
        vehicle.setVehicleName("My Car's Name");
        vehicle.setMake("Mahindra & Mahindra");
        
        assertEquals("KA-01-AB-1234", vehicle.getRegistrationNumber());
        assertEquals("My Car's Name", vehicle.getVehicleName());
        assertEquals("Mahindra & Mahindra", vehicle.getMake());
        
        String displayName = vehicle.getDisplayName();
        assertEquals("My Car's Name (KA-01-AB-1234)", displayName);
    }
    
    @Test
    public void testVehicleWithZeroYear() {
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setYear(0);
        
        String details = vehicle.getVehicleDetails();
        assertEquals("Toyota Camry", details); // Year 0 should be excluded
    }
    
    @Test
    public void testVehicleWithNegativeYear() {
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setYear(-1);
        
        String details = vehicle.getVehicleDetails();
        assertEquals("Toyota Camry", details); // Negative year should be excluded
    }
}