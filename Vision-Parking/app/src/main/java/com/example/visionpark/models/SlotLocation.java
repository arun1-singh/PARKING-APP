package com.example.visionpark.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Model class representing the location of a parking slot
 * Contains floor, row, and slot information
 */
public class SlotLocation implements Serializable {
    @SerializedName("slot_id")
    private int slotId;
    
    @SerializedName("floor_id")
    private int floorId;
    
    @SerializedName("row_id")
    private int rowId;
    
    @SerializedName("floor_name")
    private String floorName;
    
    @SerializedName("row_name")
    private String rowName;
    
    @SerializedName("slot_name")
    private String slotName;
    
    // Default constructor
    public SlotLocation() {}
    
    // Constructor
    public SlotLocation(int slotId, int floorId, int rowId, String floorName, String rowName, String slotName) {
        this.slotId = slotId;
        this.floorId = floorId;
        this.rowId = rowId;
        this.floorName = floorName;
        this.rowName = rowName;
        this.slotName = slotName;
    }
    
    // Getters and Setters
    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }
    
    public int getFloorId() { return floorId; }
    public void setFloorId(int floorId) { this.floorId = floorId; }
    
    public int getRowId() { return rowId; }
    public void setRowId(int rowId) { this.rowId = rowId; }
    
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    
    public String getRowName() { return rowName; }
    public void setRowName(String rowName) { this.rowName = rowName; }
    
    public String getSlotName() { return slotName; }
    public void setSlotName(String slotName) { this.slotName = slotName; }
    
    // Helper methods
    public String getFormattedLocation() {
        StringBuilder location = new StringBuilder();
        
        if (floorName != null && !floorName.isEmpty()) {
            if (floorName.toLowerCase().contains("floor")) {
                location.append(floorName);
            } else {
                location.append("Floor ").append(floorName);
            }
        }
        
        if (rowName != null && !rowName.isEmpty()) {
            if (location.length() > 0) location.append(", ");
            location.append("Row ").append(rowName);
        }
        
        if (slotName != null && !slotName.isEmpty()) {
            if (location.length() > 0) location.append(", ");
            location.append("Slot ").append(slotName);
        }
        
        return location.length() > 0 ? location.toString() : "Location not available";
    }
    
    @Override
    public String toString() {
        return "SlotLocation{" +
                "slotId=" + slotId +
                ", floorName='" + floorName + '\'' +
                ", rowName='" + rowName + '\'' +
                ", slotName='" + slotName + '\'' +
                '}';
    }
}