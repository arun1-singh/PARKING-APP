package com.example.visionpark.api;

import android.content.Context;
import android.util.Log;
import com.example.visionpark.models.ParkingLot;
import com.example.visionpark.utils.TokenManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ParkingApiService {
    private static final String TAG = "ParkingApiService";
    private static final String BASE_URL = "http://10.0.2.2:5000/api/v1"; // Android emulator localhost
    private static final String FALLBACK_URL = "http://10.0.2.2:80"; // Try nginx proxy as fallback
    // For physical device, use your computer's IP: "http://192.168.1.XXX:5000"
    
    private final OkHttpClient client;
    private final Context context;
    
    public ParkingApiService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)  // Reduced read timeout to avoid hanging
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new okhttp3.ConnectionPool(10, 2, TimeUnit.MINUTES)) // More connections, shorter keep-alive
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request();
                    Log.d(TAG, "Making request to: " + request.url());
                    Log.d(TAG, "Request headers: " + request.headers());
                    
                    try {
                        okhttp3.Response response = chain.proceed(request);
                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response headers: " + response.headers());
                        
                        // Check for potential issues with the response
                        String connection = response.header("Connection");
                        String contentLength = response.header("Content-Length");
                        Log.d(TAG, "Connection: " + connection + ", Content-Length: " + contentLength);
                        
                        return response;
                    } catch (Exception e) {
                        Log.e(TAG, "Request failed with exception: " + e.getMessage(), e);
                        throw e;
                    }
                })
                .build();
    }
    
    public interface ParkingLotsCallback {
        void onSuccess(List<ParkingLot> parkingLots);
        void onError(String error);
    }
    
    public interface ConnectivityCallback {
        void onSuccess(String workingUrl);
        void onError(String error);
    }
    
    public void testConnectivity(ConnectivityCallback callback) {
        Log.d(TAG, "Testing connectivity to backend servers...");
        
        // Test primary URL first
        testUrl(BASE_URL, new ConnectivityCallback() {
            @Override
            public void onSuccess(String workingUrl) {
                Log.d(TAG, "Primary URL working: " + workingUrl);
                callback.onSuccess(workingUrl);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Primary URL failed, testing fallback...");
                // Test fallback URL
                testUrl(FALLBACK_URL, callback);
            }
        });
    }
    
    private void testUrl(String baseUrl, ConnectivityCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Connectivity test failed for " + baseUrl + ": " + e.getMessage());
                callback.onError("Failed to connect to " + baseUrl + ": " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Connectivity test successful for " + baseUrl);
                    callback.onSuccess(baseUrl);
                } else {
                    Log.e(TAG, "Connectivity test failed for " + baseUrl + ": " + response.code());
                    callback.onError("Server responded with error: " + response.code());
                }
                response.close();
            }
        });
    }
    
    public void getNearbyParkingLots(double latitude, double longitude, double radiusKm, ParkingLotsCallback callback) {
        String token = TokenManager.getToken(context);
        if (token == null) {
            callback.onError("Authentication token not found. Please login again.");
            return;
        }
        
        // Convert radius from km to meters for API
        int radiusMeters = (int) (radiusKm * 1000);
        
        // Try primary URL first, then fallback
        makeNearbyRequest(BASE_URL, latitude, longitude, radiusMeters, token, callback, true);
    }
    
    private void makeNearbyRequest(String baseUrl, double latitude, double longitude, int radiusMeters, String token, ParkingLotsCallback callback, boolean canRetry) {
        String url = baseUrl + "/parking/lots/nearby" +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&radius=" + radiusMeters;
        
        Log.d(TAG, "🚀 Preparing nearby parking request:");
        Log.d(TAG, "   Base URL: " + baseUrl);
        Log.d(TAG, "   Full URL: " + url);
        Log.d(TAG, "   Token present: " + (token != null ? "YES (length: " + token.length() + ")" : "NO"));
        Log.d(TAG, "   Can retry: " + canRetry);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        
        Log.d(TAG, "📡 Making API request to: " + url);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed to " + baseUrl + ": " + e.getMessage(), e);
                
                // If this was the primary URL and we can retry, try the fallback
                if (canRetry && baseUrl.equals(BASE_URL)) {
                    Log.w(TAG, "Retrying with fallback URL: " + FALLBACK_URL);
                    makeNearbyRequest(FALLBACK_URL, latitude, longitude, radiusMeters, token, callback, false);
                } else {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = null;
                try {
                    Log.d(TAG, "📥 Received response:");
                    Log.d(TAG, "   Status: " + response.code() + " " + response.message());
                    Log.d(TAG, "   Headers: " + response.headers());
                    
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "❌ API request unsuccessful: " + response.code());
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        Log.e(TAG, "   Error body: " + errorBody);
                        callback.onError("Server error: " + response.code() + " - " + errorBody);
                        return;
                    }
                    
                    // Read response body carefully to avoid stream issues
                    if (response.body() == null) {
                        Log.e(TAG, "❌ Response body is null");
                        callback.onError("Empty response from server");
                        return;
                    }
                    
                    // Try to read response body with better error handling
                    try {
                        okhttp3.ResponseBody body = response.body();
                        long contentLength = body.contentLength();
                        Log.d(TAG, "📄 Expected content length: " + contentLength + " bytes");
                        
                        // Read the response body with timeout protection
                        responseBody = body.string();
                        Log.d(TAG, "📄 Actual response body length: " + responseBody.length());
                        Log.d(TAG, "📄 Response body preview: " + (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody));
                        
                        // Verify we got the expected amount of data
                        if (contentLength > 0 && responseBody.length() != contentLength) {
                            Log.w(TAG, "⚠️ Content length mismatch! Expected: " + contentLength + ", Got: " + responseBody.length());
                            // Don't fail on content length mismatch, just log it
                        }
                        
                    } catch (IOException e) {
                        Log.e(TAG, "❌ Failed to read response body: " + e.getMessage(), e);
                        
                        // If this was the primary URL and we can retry, try the fallback
                        if (canRetry && baseUrl.equals(BASE_URL)) {
                            Log.w(TAG, "Retrying with fallback URL due to stream error: " + FALLBACK_URL);
                            makeNearbyRequest(FALLBACK_URL, latitude, longitude, radiusMeters, token, callback, false);
                            return;
                        } else {
                            callback.onError("Failed to read server response: " + e.getMessage());
                            return;
                        }
                    } finally {
                        // Ensure response is properly closed
                        response.close();
                    }
                    
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (!jsonResponse.getBoolean("success")) {
                        String error = jsonResponse.optString("error", "Unknown error");
                        callback.onError("API error: " + error);
                        return;
                    }
                    
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    List<ParkingLot> parkingLots = parseParkingLots(dataArray);
                    
                    Log.d(TAG, "Successfully parsed " + parkingLots.size() + " parking lots");
                    callback.onSuccess(parkingLots);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "❌ JSON parsing error: " + e.getMessage(), e);
                    Log.e(TAG, "   Raw response that failed to parse: " + responseBody);
                    callback.onError("Data parsing error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "❌ IO error while reading response: " + e.getMessage(), e);
                    callback.onError("Network IO error: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "❌ Unexpected error: " + e.getMessage(), e);
                    Log.e(TAG, "   Error type: " + e.getClass().getSimpleName());
                    callback.onError("Unexpected error: " + e.getMessage());
                } finally {
                    // Ensure response is properly closed
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }
    
    private List<ParkingLot> parseParkingLots(JSONArray jsonArray) throws JSONException {
        List<ParkingLot> parkingLots = new ArrayList<>();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject lotJson = jsonArray.getJSONObject(i);
            ParkingLot parkingLot = parseParkingLotFromJson(lotJson);
            
            if (parkingLot != null) {
                parkingLots.add(parkingLot);
            }
        }
        
        return parkingLots;
    }
    
    private ParkingLot parseParkingLotFromJson(JSONObject jsonObject) throws JSONException {
        try {
            // Backend returns "parkinglot_id", not "id"
            int id = jsonObject.has("id") ? jsonObject.getInt("id") : jsonObject.getInt("parkinglot_id");
            String name = jsonObject.getString("name");
            
            // Handle coordinates
            double latitude = jsonObject.getDouble("latitude");
            double longitude = jsonObject.getDouble("longitude");
            
            String carFee = jsonObject.optString("car_fee", "Free");
            String twoWheelerFee = jsonObject.optString("two_wheeler_fee", "Free");
            int availableCarSlots = jsonObject.optInt("available_car_slots", 0);
            int totalCarSlots = jsonObject.optInt("car_capacity", 0);
            int availableTwoWheelerSlots = jsonObject.optInt("available_two_wheeler_slots", 0);
            int totalTwoWheelerSlots = jsonObject.optInt("two_wheeler_capacity", 0);
            String paymentMode = jsonObject.optString("payment_mode", "Free Parking");
            
            // Create ParkingLot object
            ParkingLot parkingLot = new ParkingLot(
                id, name, latitude, longitude, carFee, twoWheelerFee,
                availableCarSlots, totalCarSlots, availableTwoWheelerSlots,
                totalTwoWheelerSlots, paymentMode
            );
            
            // Set additional fields
            parkingLot.setAddress(jsonObject.optString("address", ""));
            parkingLot.setLandmark(jsonObject.optString("landmark", ""));
            parkingLot.setCity(jsonObject.optString("city", ""));
            
            // Set distance if available
            if (jsonObject.has("distance")) {
                parkingLot.setDistance(jsonObject.getDouble("distance"));
            }
            
            // Set hourly rate if available
            if (jsonObject.has("hourly_rate")) {
                parkingLot.setHourlyRate(jsonObject.getDouble("hourly_rate"));
            }
            
            // Set additional backend fields
            parkingLot.setParkingType(jsonObject.optString("parking_type", ""));
            parkingLot.setHasCctv(jsonObject.optBoolean("has_cctv", false));
            parkingLot.setHasBoomBarrier(jsonObject.optBoolean("has_boom_barrier", false));
            parkingLot.setTicketGenerated(jsonObject.optString("ticket_generated", ""));
            parkingLot.setEntryExitGates(jsonObject.optString("entry_exit_gates", ""));
            parkingLot.setWeeklyOff(jsonObject.optString("weekly_off", ""));
            parkingLot.setParkingTiming(jsonObject.optString("parking_timing", ""));
            parkingLot.setVehicleTypes(jsonObject.optString("vehicle_types", ""));
            parkingLot.setAllowsPrepaidPasses(jsonObject.optString("allows_prepaid_passes", ""));
            parkingLot.setProvidesValetServices(jsonObject.optString("provides_valet_services", ""));
            parkingLot.setValueAddedServices(jsonObject.optString("value_added_services", ""));
            parkingLot.setAvailabilityStatus(jsonObject.optString("availability_status", ""));
            parkingLot.setOpen(jsonObject.optBoolean("is_open", true));
            
            return parkingLot;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing parking lot JSON: " + e.getMessage(), e);
            return null;
        }
    }
}
