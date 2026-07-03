package com.example.visionpark.network;

import android.content.Context;
import android.util.Log;
import com.example.visionpark.models.UserVehicle;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service class for handling vehicle and session management API calls
 */
public class VehicleApiService {
    private static final String TAG = "VehicleApiService";
    private static final String BASE_URL = "http://10.0.2.2:5000/api/v1"; // Use nginx proxy
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Context context;
    
    public VehicleApiService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    // Callback interfaces
    public interface VehicleListCallback {
        void onSuccess(List<UserVehicle> vehicles);
        void onError(String error);
    }
    
    public interface VehicleCallback {
        void onSuccess(UserVehicle vehicle);
        void onError(String error);
    }
    
    public interface SessionCallback {
        void onSuccess(String ticketId, String message);
        void onError(String error);
    }
    
    /**
     * Get all vehicles for the authenticated user
     */
    public void getUserVehicles(VehicleListCallback callback) {
        String token = TokenManager.getToken(context);
        if (token == null) {
            callback.onError("Authentication token not found. Please login again.");
            return;
        }
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/user/vehicles")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        
        Log.d(TAG, "Getting user vehicles...");
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get user vehicles", e);
                callback.onError("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Get vehicles response: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        callback.onError("Server error: " + response.code());
                        return;
                    }
                    
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (!jsonResponse.getBoolean("success")) {
                        String error = jsonResponse.optString("error", "Unknown error");
                        callback.onError(error);
                        return;
                    }
                    
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    List<UserVehicle> vehicles = parseVehicles(dataArray);
                    
                    Log.d(TAG, "Successfully parsed " + vehicles.size() + " vehicles");
                    callback.onSuccess(vehicles);
                    
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    callback.onError("Data parsing error: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                    callback.onError("Unexpected error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Create a new vehicle for the user
     */
    public void createVehicle(UserVehicle vehicle, VehicleCallback callback) {
        String token = TokenManager.getToken(context);
        if (token == null) {
            callback.onError("Authentication token not found. Please login again.");
            return;
        }
        
        try {
            JSONObject vehicleJson = new JSONObject();
            vehicleJson.put("registration_number", vehicle.getRegistrationNumber());
            vehicleJson.put("vehicle_name", vehicle.getVehicleName());
            if (vehicle.getMake() != null) vehicleJson.put("make", vehicle.getMake());
            if (vehicle.getModel() != null) vehicleJson.put("model", vehicle.getModel());
            if (vehicle.getYear() != null) vehicleJson.put("year", vehicle.getYear());
            if (vehicle.getVehicleType() != null) vehicleJson.put("vehicle_type", vehicle.getVehicleType());
            if (vehicle.getColor() != null) vehicleJson.put("color", vehicle.getColor());
            
            RequestBody body = RequestBody.create(vehicleJson.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/user/vehicles")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            Log.d(TAG, "Creating vehicle: " + vehicle.getRegistrationNumber());
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to create vehicle", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Create vehicle response: " + response.code());
                        
                        if (!response.isSuccessful()) {
                            if (response.code() == 409) {
                                callback.onError("Vehicle with this registration number already exists");
                            } else {
                                callback.onError("Server error: " + response.code());
                            }
                            return;
                        }
                        
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        if (!jsonResponse.getBoolean("success")) {
                            String error = jsonResponse.optString("error", "Unknown error");
                            callback.onError(error);
                            return;
                        }
                        
                        JSONObject vehicleData = jsonResponse.getJSONObject("data");
                        UserVehicle createdVehicle = parseVehicle(vehicleData);
                        
                        Log.d(TAG, "Vehicle created successfully: " + createdVehicle.getVehicleId());
                        callback.onSuccess(createdVehicle);
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        callback.onError("Data parsing error: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error", e);
                        callback.onError("Unexpected error: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating vehicle JSON", e);
            callback.onError("Data formatting error: " + e.getMessage());
        }
    }
    
    /**
     * Start a parking session
     */
    public void startParkingSession(int vehicleId, int parkingLotId, SessionCallback callback) {
        String token = TokenManager.getToken(context);
        if (token == null) {
            callback.onError("Authentication token not found. Please login again.");
            return;
        }
        
        try {
            JSONObject sessionJson = new JSONObject();
            sessionJson.put("vehicle_id", vehicleId);
            sessionJson.put("parkinglot_id", parkingLotId);
            
            RequestBody body = RequestBody.create(sessionJson.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/user/sessions/check-in")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            Log.d(TAG, "Starting parking session for vehicle " + vehicleId + " at lot " + parkingLotId);
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to start parking session", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Start session response: " + response.code());
                        
                        if (!response.isSuccessful()) {
                            if (response.code() == 409) {
                                // Parse error message for specific conflict reason
                                try {
                                    JSONObject errorJson = new JSONObject(responseBody);
                                    String error = errorJson.optString("error", "Conflict error");
                                    callback.onError(error);
                                } catch (JSONException e) {
                                    callback.onError("No available parking slots or vehicle already has active session");
                                }
                            } else {
                                callback.onError("Server error: " + response.code());
                            }
                            return;
                        }
                        
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        if (!jsonResponse.getBoolean("success")) {
                            String error = jsonResponse.optString("error", "Unknown error");
                            callback.onError(error);
                            return;
                        }
                        
                        JSONObject sessionData = jsonResponse.getJSONObject("data");
                        // API now uses snake_case (ticket_id) instead of camelCase (ticketId)
                        String ticketId = sessionData.getString("ticket_id");
                        String message = jsonResponse.optString("message", "Session started successfully");
                        
                        Log.d(TAG, "Parking session started: " + ticketId);
                        callback.onSuccess(ticketId, message);
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        callback.onError("Data parsing error: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error", e);
                        callback.onError("Unexpected error: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating session JSON", e);
            callback.onError("Data formatting error: " + e.getMessage());
        }
    }
    
    private List<UserVehicle> parseVehicles(JSONArray jsonArray) throws JSONException {
        List<UserVehicle> vehicles = new ArrayList<>();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject vehicleJson = jsonArray.getJSONObject(i);
            UserVehicle vehicle = parseVehicle(vehicleJson);
            if (vehicle != null) {
                vehicles.add(vehicle);
            }
        }
        
        return vehicles;
    }
    
    private UserVehicle parseVehicle(JSONObject jsonObject) throws JSONException {
        UserVehicle vehicle = new UserVehicle();
        
        vehicle.setVehicleId(jsonObject.getInt("vehicle_id"));
        vehicle.setUserId(jsonObject.optInt("user_id", 0));
        vehicle.setRegistrationNumber(jsonObject.getString("registration_number"));
        vehicle.setVehicleName(jsonObject.optString("vehicle_name", null));
        vehicle.setMake(jsonObject.optString("make", null));
        vehicle.setModel(jsonObject.optString("model", null));
        
        if (jsonObject.has("year") && !jsonObject.isNull("year")) {
            vehicle.setYear(jsonObject.getInt("year"));
        }
        
        vehicle.setVehicleType(jsonObject.optString("vehicle_type", "car"));
        vehicle.setColor(jsonObject.optString("color", null));
        vehicle.setActive(jsonObject.optBoolean("is_active", true));
        vehicle.setCreatedAt(jsonObject.optString("created_at", null));
        vehicle.setUpdatedAt(jsonObject.optString("updated_at", null));
        
        return vehicle;
    }
}