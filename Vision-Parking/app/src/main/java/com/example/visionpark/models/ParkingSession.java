package com.example.visionpark.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

/**
 * Model class representing a parking session
 * Contains information about active and completed parking sessions
 */
public class ParkingSession implements Serializable {
    @SerializedName("ticket_id")
    private String ticketId;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("vehicle_id")
    private int vehicleId;
    
    @SerializedName("parkinglot_id")
    private int parkinglotId;
    
    @SerializedName("parking_lot_name")
    private String parkingLotName;
    
    @SerializedName("parking_lot_address")
    private String parkingLotAddress;
    
    @SerializedName("vehicle_reg_no")
    private String vehicleRegNo;
    
    @SerializedName("vehicle_type")
    private String vehicleType;
    
    @SerializedName("start_time")
    private Date startTime;
    
    @SerializedName("end_time")
    private Date endTime;
    
    @SerializedName("duration_hours")
    private double durationHrs;
    
    @SerializedName("total_amount")
    private double totalAmount;
    
    @SerializedName("payment_status")
    private String paymentStatus;
    
    @SerializedName("payment_method")
    private String paymentMethod;
    
    @SerializedName("receipt_url")
    private String receiptUrl;
    
    @SerializedName("status")
    private String sessionStatus;
    
    @SerializedName("slot_location")
    private SlotLocation slotLocation;
    
    // Default constructor
    public ParkingSession() {}
    
    // Constructor for new session
    public ParkingSession(String ticketId, int userId, int vehicleId, int parkinglotId, 
                         String vehicleRegNo, String vehicleType, Date startTime) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.parkinglotId = parkinglotId;
        this.vehicleRegNo = vehicleRegNo;
        this.vehicleType = vehicleType;
        this.startTime = startTime;
        this.sessionStatus = "active";
        this.paymentStatus = "pending";
    }
    
    // Getters and Setters
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    
    public int getParkinglotId() { return parkinglotId; }
    public void setParkinglotId(int parkinglotId) { this.parkinglotId = parkinglotId; }
    
    public String getParkingLotName() { return parkingLotName; }
    public void setParkingLotName(String parkingLotName) { this.parkingLotName = parkingLotName; }
    
    public String getParkingLotAddress() { return parkingLotAddress; }
    public void setParkingLotAddress(String parkingLotAddress) { this.parkingLotAddress = parkingLotAddress; }
    
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    
    public double getDurationHrs() { return durationHrs; }
    public void setDurationHrs(double durationHrs) { this.durationHrs = durationHrs; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    
    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }
    
    public SlotLocation getSlotLocation() { return slotLocation; }
    public void setSlotLocation(SlotLocation slotLocation) { this.slotLocation = slotLocation; }
    
    // Helper methods
    public boolean isActive() {
        return "active".equals(sessionStatus);
    }
    
    public boolean isCompleted() {
        return "completed".equals(sessionStatus);
    }
    
    public String getCurrentDuration() {
        if (startTime == null) return "0 min";
        
        long currentTime = System.currentTimeMillis();
        long durationMillis = currentTime - startTime.getTime();
        
        return formatDuration(durationMillis);
    }
    
    public double getCurrentCost() {
        // Use actual amount from backend if available
        if (totalAmount > 0.0) {
            return totalAmount;
        }
        // Fallback: calculate from start time at default rate
        if (startTime == null) return 0.0;
        long currentTime = System.currentTimeMillis();
        long durationMillis = currentTime - startTime.getTime();
        double hours = durationMillis / (1000.0 * 60 * 60);
        return Math.ceil(hours) * 50.0; // fallback rate
    }
    
    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + " hr " + (minutes % 60) + " min";
        } else if (minutes > 0) {
            return minutes + " min";
        } else {
            return "< 1 min";
        }
    }
    
    @Override
    public String toString() {
        return "ParkingSession{" +
                "ticketId='" + ticketId + '\'' +
                ", vehicleRegNo='" + vehicleRegNo + '\'' +
                ", parkingLotName='" + parkingLotName + '\'' +
                ", sessionStatus='" + sessionStatus + '\'' +
                ", startTime=" + startTime +
                '}';
    }
}