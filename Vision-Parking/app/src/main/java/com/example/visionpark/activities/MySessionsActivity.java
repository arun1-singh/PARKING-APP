package com.example.visionpark.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.visionpark.activities.PaymentActivity;
import com.example.visionpark.R;
import com.example.visionpark.adapters.SessionAdapter;
import com.example.visionpark.models.ParkingSession;
import com.example.visionpark.models.PaymentInfo;
import com.example.visionpark.models.SlotLocation;
import com.example.visionpark.network.ApiClient;
import com.example.visionpark.network.ApiService;
import com.example.visionpark.network.SessionCheckoutRequest;
import com.example.visionpark.services.SessionTrackingService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MySessionsActivity extends AppCompatActivity implements SessionAdapter.OnSessionActionListener {
    
    private RecyclerView recyclerViewSessions;
    private SessionAdapter sessionAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutLoading;
    private TextView tvSessionSummary;
    
    private List<ParkingSession> allSessions = new ArrayList<>();
    private List<ParkingSession> activeSessions = new ArrayList<>();
    private List<ParkingSession> completedSessions = new ArrayList<>();
    private int currentTab = 0; // 0 = Active, 1 = Completed
    private SessionTrackingService.SessionBinder sessionBinder;
    private ServiceConnection serviceConnection;
    private boolean isServiceBound = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sessions);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Sessions");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh();
        setupTabs();
        setupBottomNavigation();
        bindToSessionTrackingService();
        
        // Load sessions
        loadSessions();
        
        // Check if we should highlight a new session
        String newSessionId = getIntent().getStringExtra("new_session_id");
        if (newSessionId != null) {
            // TODO: Highlight the new session in the list
            Toast.makeText(this, "New parking session started!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initializeViews() {
        recyclerViewSessions = findViewById(R.id.recyclerViewSessions);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutLoading = findViewById(R.id.layoutLoading);
        tvSessionSummary = findViewById(R.id.tvSessionSummary);
    }
    
    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(this, allSessions);
        sessionAdapter.setOnSessionActionListener(this);
        
        recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSessions.setAdapter(sessionAdapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.green);
        swipeRefreshLayout.setOnRefreshListener(this::loadSessions);
    }
    
    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateSessionInList(List<ParkingSession> list, ParkingSession updated) {
        for (int i = 0; i < list.size(); i++) {
            if (updated.getTicketId() != null && updated.getTicketId().equals(list.get(i).getTicketId())) {
                list.set(i, updated);
                return;
            }
        }
    }

    private void updateTabDisplay() {
        List<ParkingSession> toShow = currentTab == 0 ? activeSessions : completedSessions;
        sessionAdapter.updateSessions(new ArrayList<>(toShow));
        if (toShow.isEmpty()) {
            layoutEmptyState.setVisibility(android.view.View.VISIBLE);
            recyclerViewSessions.setVisibility(android.view.View.GONE);
        } else {
            layoutEmptyState.setVisibility(android.view.View.GONE);
            recyclerViewSessions.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_sessions);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(MySessionsActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_sessions) {
                    // Already on My Sessions
                    return true;
                } else if (itemId == R.id.nav_bookings) {
                    Intent intent = new Intent(MySessionsActivity.this, BookingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(MySessionsActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }
    
    private void bindToSessionTrackingService() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                sessionBinder = (SessionTrackingService.SessionBinder) service;
                isServiceBound = true;
                
                sessionBinder.setSessionUpdateListener(new SessionTrackingService.SessionUpdateListener() {
                    @Override
                    public void onSessionUpdated(ParkingSession session) {
                        runOnUiThread(() -> {
                            // Update backing lists only
                            updateSessionInList(activeSessions, session);
                            updateSessionInList(completedSessions, session);
                            updateSessionInList(allSessions, session);
                            // Update only this specific card — no full list re-render
                            sessionAdapter.updateSession(session);
                            updateSessionSummary();
                        });
                    }
                });
            }
            
            @Override
            public void onServiceDisconnected(ComponentName name) {
                sessionBinder = null;
                isServiceBound = false;
            }
        };
        
        Intent intent = new Intent(this, SessionTrackingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    private void loadSessions() {
        showLoading(true);
        
        // Clear existing sessions to prevent duplicates
        allSessions.clear();
        activeSessions.clear();
        completedSessions.clear();
        
        // Check network connectivity
        if (!isNetworkAvailable()) {
            android.util.Log.w("MySessionsActivity", "No network connection - using mock data");
            Toast.makeText(this, "No internet connection - showing offline data", 
                          Toast.LENGTH_LONG).show();
            
            createMockActiveSessions();
            createMockPastSessions();
            updateTabDisplay();
            updateSessionSummary();
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        android.util.Log.d("MySessionsActivity", "Network available - loading sessions from API");
        
        // Load active sessions first, history loads after active completes
        loadActiveSessions();
    }
    
    private void loadActiveSessions() {
        // Check authentication token first
        String token = com.example.visionpark.utils.TokenManager.getToken(this);
        if (token == null || token.trim().isEmpty()) {
            android.util.Log.e("MySessionsActivity", "No authentication token available");
            Toast.makeText(this, "Please login again", Toast.LENGTH_LONG).show();
            
            // Redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        android.util.Log.d("MySessionsActivity", "Loading active sessions with token present: " + 
                          (token.length() > 10 ? "YES" : "NO"));
        
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        apiService.getActiveSessions().enqueue(new retrofit2.Callback<ApiService.ApiResponse<List<ParkingSession>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ApiResponse<List<ParkingSession>>> call, 
                                 retrofit2.Response<ApiService.ApiResponse<List<ParkingSession>>> response) {
                android.util.Log.d("MySessionsActivity", "Active sessions API response: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("MySessionsActivity", "Response body success: " + response.body().isSuccess());
                    
                    if (response.body().isSuccess()) {
                        List<ParkingSession> fetchedActive = response.body().getData();
                        android.util.Log.d("MySessionsActivity", "Active sessions count: " + 
                                          (fetchedActive != null ? fetchedActive.size() : "null"));
                        
                        if (fetchedActive != null && !fetchedActive.isEmpty()) {
                            for (ParkingSession session : fetchedActive) {
                                allSessions.add(session);
                                activeSessions.add(session);
                                if (isServiceBound) {
                                    sessionBinder.startTrackingSession(session);
                                }
                            }
                            android.util.Log.d("MySessionsActivity", "Loaded " + fetchedActive.size() + " active session(s) from API");
                        } else {
                            android.util.Log.d("MySessionsActivity", "No active sessions found from API");
                        }
                    } else {
                        android.util.Log.w("MySessionsActivity", "API returned success=false: " + response.body().getError());
                        createMockActiveSessions();
                    }
                } else {
                    android.util.Log.e("MySessionsActivity", "API response unsuccessful: " + response.code() + 
                                      " " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("MySessionsActivity", "Error body: " + errorBody);
                        } catch (Exception e) {
                            android.util.Log.e("MySessionsActivity", "Could not read error body", e);
                        }
                    }
                    createMockActiveSessions();
                }
                
                // Update active tab display, then load history
                runOnUiThread(() -> {
                    if (currentTab == 0) updateTabDisplay();
                    updateSessionSummary();
                });
                // Chain: load history only after active is done
                loadSessionHistory();
            }
            
            @Override
            public void onFailure(retrofit2.Call<ApiService.ApiResponse<List<ParkingSession>>> call, Throwable t) {
                android.util.Log.e("MySessionsActivity", "Active sessions API call failed: " + t.getMessage(), t);
                android.util.Log.e("MySessionsActivity", "Request URL: " + call.request().url());
                
                // Check specific error types
                if (t instanceof java.net.ConnectException) {
                    android.util.Log.e("MySessionsActivity", "Connection failed - check if backend is running");
                    Toast.makeText(MySessionsActivity.this, 
                        "Cannot connect to server. Using offline data.", Toast.LENGTH_LONG).show();
                } else if (t instanceof java.net.SocketTimeoutException) {
                    android.util.Log.e("MySessionsActivity", "Request timeout - backend may be slow");
                    Toast.makeText(MySessionsActivity.this, 
                        "Server timeout. Using offline data.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MySessionsActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
                
                // Fallback to mock data
                createMockActiveSessions();
                runOnUiThread(() -> {
                    if (currentTab == 0) updateTabDisplay();
                    updateSessionSummary();
                });
                // Chain: load history even if active failed
                loadSessionHistory();
            }
        });
    }
    
    private void loadSessionHistory() {
        android.util.Log.d("MySessionsActivity", "Loading session history");
        
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        apiService.getSessionHistory().enqueue(new retrofit2.Callback<ApiService.ApiResponse<List<ParkingSession>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ApiResponse<List<ParkingSession>>> call, 
                                 retrofit2.Response<ApiService.ApiResponse<List<ParkingSession>>> response) {
                android.util.Log.d("MySessionsActivity", "Session history API response: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("MySessionsActivity", "History response body success: " + response.body().isSuccess());
                    
                    if (response.body().isSuccess()) {
                        List<ParkingSession> pastSessions = response.body().getData();
                        android.util.Log.d("MySessionsActivity", "Past sessions count: " + 
                                          (pastSessions != null ? pastSessions.size() : "null"));
                        
                        if (pastSessions != null && !pastSessions.isEmpty()) {
                            java.util.Set<String> existingIds = new java.util.HashSet<>();
                            for (ParkingSession s : allSessions) {
                                if (s.getTicketId() != null) existingIds.add(s.getTicketId());
                            }
                            int added = 0;
                            for (ParkingSession s : pastSessions) {
                                if (s.getTicketId() != null && !existingIds.contains(s.getTicketId())) {
                                    allSessions.add(s);
                                    completedSessions.add(s);
                                    added++;
                                }
                            }
                            android.util.Log.d("MySessionsActivity", "Loaded " + added + " past session(s) from API (deduplicated)");
                        } else {
                            android.util.Log.d("MySessionsActivity", "No past sessions found from API");
                        }
                    } else {
                        android.util.Log.w("MySessionsActivity", "History API returned success=false: " + response.body().getError());
                        createMockPastSessions();
                    }
                } else {
                    android.util.Log.e("MySessionsActivity", "History API response unsuccessful: " + response.code());
                    createMockPastSessions();
                }
                
                // Update UI after loading all sessions
                runOnUiThread(() -> {
                    if (currentTab == 1) updateTabDisplay();
                    updateSessionSummary();
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                });
            }
            
            @Override
            public void onFailure(retrofit2.Call<ApiService.ApiResponse<List<ParkingSession>>> call, Throwable t) {
                android.util.Log.e("MySessionsActivity", "Session history API call failed: " + t.getMessage(), t);
                android.util.Log.e("MySessionsActivity", "Request URL: " + call.request().url());
                
                // Fallback to mock data
                createMockPastSessions();
                runOnUiThread(() -> {
                    if (currentTab == 1) updateTabDisplay();
                    updateSessionSummary();
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                });
            }
        });
    }
    
    private void createMockActiveSessions() {
        // Only create mock data if we don't already have sessions
        if (allSessions.isEmpty()) {
            // Mock active session for demonstration when API is not available
            ParkingSession activeSession = new ParkingSession();
            activeSession.setTicketId("TKT123456");
            activeSession.setUserId(1);
            activeSession.setVehicleId(1);
            activeSession.setParkinglotId(1);
            activeSession.setParkingLotName("Central Plaza Parking");
            activeSession.setParkingLotAddress("123 Main Street, Downtown");
            activeSession.setVehicleRegNo("ABC-123");
            activeSession.setVehicleType("car");
            activeSession.setStartTime(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)); // 2 hours ago
            activeSession.setSessionStatus("active");
            activeSession.setPaymentStatus("pending");
            activeSession.setPaymentMethod("card");
            activeSession.setTotalAmount(0.0); // Will be calculated dynamically
            
            SlotLocation slotLocation = new SlotLocation();
            slotLocation.setFloorName("2");
            slotLocation.setRowName("A");
            slotLocation.setSlotName("15");
            activeSession.setSlotLocation(slotLocation);
            
            allSessions.add(activeSession);
            
            // Start tracking active sessions
            if (isServiceBound) {
                sessionBinder.startTrackingSession(activeSession);
            }
        }
    }
    
    private void createMockPastSessions() {
        // Only add past sessions if we don't have too many already
        if (allSessions.size() < 3) {
            // Mock completed session for demonstration when API is not available
            ParkingSession completedSession = new ParkingSession();
            completedSession.setTicketId("TKT123455");
            completedSession.setUserId(1);
            completedSession.setVehicleId(1);
            completedSession.setParkinglotId(2);
            completedSession.setParkingLotName("Mall Parking");
            completedSession.setParkingLotAddress("456 Shopping Ave, City Center");
            completedSession.setVehicleRegNo("ABC-123");
            completedSession.setVehicleType("car");
            completedSession.setStartTime(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // Yesterday
            completedSession.setEndTime(new Date(System.currentTimeMillis() - 22 * 60 * 60 * 1000)); // 22 hours ago
            completedSession.setDurationHrs(2.0);
            completedSession.setTotalAmount(100.0);
            completedSession.setSessionStatus("completed");
            completedSession.setPaymentStatus("completed");
            completedSession.setPaymentMethod("card");
            
            SlotLocation completedSlotLocation = new SlotLocation();
            completedSlotLocation.setFloorName("1");
            completedSlotLocation.setRowName("B");
            completedSlotLocation.setSlotName("8");
            completedSession.setSlotLocation(completedSlotLocation);
            
            allSessions.add(completedSession);
            
            // Add one more past session for variety
            ParkingSession pastSession2 = new ParkingSession();
            pastSession2.setTicketId("TKT123454");
            pastSession2.setUserId(1);
            pastSession2.setVehicleId(1);
            pastSession2.setParkinglotId(3);
            pastSession2.setParkingLotName("Airport Parking");
            pastSession2.setParkingLotAddress("789 Airport Road, Terminal 1");
            pastSession2.setVehicleRegNo("ABC-123");
            pastSession2.setVehicleType("car");
            pastSession2.setStartTime(new Date(System.currentTimeMillis() - 48 * 60 * 60 * 1000)); // 2 days ago
            pastSession2.setEndTime(new Date(System.currentTimeMillis() - 44 * 60 * 60 * 1000)); // 44 hours ago
            pastSession2.setDurationHrs(4.0);
            pastSession2.setTotalAmount(200.0);
            pastSession2.setSessionStatus("completed");
            pastSession2.setPaymentStatus("completed");
            pastSession2.setPaymentMethod("cash");
            
            SlotLocation pastSlotLocation2 = new SlotLocation();
            pastSlotLocation2.setFloorName("Ground");
            pastSlotLocation2.setRowName("C");
            pastSlotLocation2.setSlotName("25");
            pastSession2.setSlotLocation(pastSlotLocation2);
            
            allSessions.add(pastSession2);
        }
    }
    
    private void updateSessionSummary() {
        int activeCount = 0;
        int totalCount = allSessions.size();
        
        for (ParkingSession session : allSessions) {
            if (session.isActive()) {
                activeCount++;
            }
        }
        
        String summary;
        if (activeCount > 0) {
            summary = activeCount + " active session" + (activeCount > 1 ? "s" : "") + 
                     " • " + totalCount + " total";
        } else {
            summary = totalCount + " session" + (totalCount != 1 ? "s" : "") + " in history";
        }
        
        tvSessionSummary.setText(summary);
    }
    
    private void updateEmptyState() {
        if (allSessions.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewSessions.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewSessions.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLoading(boolean show) {
        if (show) {
            layoutLoading.setVisibility(View.VISIBLE);
            recyclerViewSessions.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            layoutLoading.setVisibility(View.GONE);
            updateEmptyState();
        }
    }
    
    // SessionAdapter.OnSessionActionListener implementation
    @Override
    public void onExitVehicle(String sessionId) {
        ParkingSession session = findSessionById(sessionId);
        if (session == null) return;
        
        showCheckoutConfirmation(session);
    }
    
    @Override
    public void onSessionDetails(ParkingSession session) {
        // TODO: Show detailed session information dialog or navigate to details screen
        Toast.makeText(this, "Session details: " + session.getTicketId(), Toast.LENGTH_SHORT).show();
    }
    
    private ParkingSession findSessionById(String sessionId) {
        for (ParkingSession session : allSessions) {
            if (session.getTicketId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }
    
    private void showCheckoutConfirmation(ParkingSession session) {
        String message = "Are you sure you want to exit your vehicle?\n\n" +
                        "Parking Lot: " + session.getParkingLotName() + "\n" +
                        "Duration: " + session.getCurrentDuration() + "\n" +
                        "Estimated Cost: €" + String.format("%.2f", session.getCurrentCost());
        
        new AlertDialog.Builder(this)
            .setTitle("Exit Vehicle")
            .setMessage(message)
            .setPositiveButton("Exit & Pay", (dialog, which) -> processCheckout(session.getTicketId()))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void processCheckout(String sessionId) {
        Toast.makeText(this, "Processing checkout...", Toast.LENGTH_SHORT).show();

        // Find the session to get amount
        ParkingSession session = findSessionById(sessionId);
        double amount = session != null ? session.getCurrentCost() : 0.0;
        String lotName = session != null ? session.getParkingLotName() : "Parking";

        // Launch payment screen instead of direct checkout
        Intent payIntent = new Intent(this, PaymentActivity.class);
        payIntent.putExtra(PaymentActivity.EXTRA_PAYMENT_FOR,  "session");
        payIntent.putExtra(PaymentActivity.EXTRA_REFERENCE_ID, sessionId);
        payIntent.putExtra(PaymentActivity.EXTRA_AMOUNT,       amount);
        payIntent.putExtra(PaymentActivity.EXTRA_DESCRIPTION,  "Parking at " + lotName + " | Ticket: " + sessionId);
        startActivityForResult(payIntent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Payment succeeded — reload sessions
            loadSessions();
        }
    }
    
    private void handleSuccessfulCheckout(String sessionId, PaymentInfo paymentInfo) {
        // Stop tracking this session
        if (isServiceBound) {
            sessionBinder.stopTrackingSession(sessionId);
        }
        
        // Update session status to completed
        ParkingSession session = findSessionById(sessionId);
        if (session != null) {
            session.setSessionStatus("completed");
            session.setEndTime(paymentInfo.getEndTime());
            // Ensure payment status is set to "completed" not just from API
            session.setPaymentStatus(paymentInfo.getPaymentStatus() != null ? 
                paymentInfo.getPaymentStatus() : "completed");
            session.setTotalAmount(paymentInfo.getTotalAmount());
            session.setPaymentMethod(paymentInfo.getPaymentMethod());
            session.setDurationHrs(calculateDurationHours(session.getStartTime(), paymentInfo.getEndTime()));
            
            // Update the adapter with the modified session
            sessionAdapter.updateSession(session);
            updateSessionSummary();
            
            android.util.Log.d("MySessionsActivity", "Session updated after checkout: " + 
                sessionId + ", payment_status: " + session.getPaymentStatus());
        }
        
        // Show payment success dialog
        showPaymentSuccessDialog(paymentInfo);
        
        // Move session from active to completed list directly (no reload flicker)
        runOnUiThread(() -> {
            ParkingSession s = findSessionById(sessionId);
            if (s != null) {
                activeSessions.remove(s);
                if (!completedSessions.contains(s)) completedSessions.add(s);
            }
            updateTabDisplay();
            updateSessionSummary();
        });
    }
    
    private void simulateSuccessfulCheckout(String sessionId) {
        // Stop tracking this session
        if (isServiceBound) {
            sessionBinder.stopTrackingSession(sessionId);
        }
        
        // Update session status to completed
        ParkingSession session = findSessionById(sessionId);
        if (session != null) {
            session.setSessionStatus("completed");
            session.setEndTime(new Date());
            session.setPaymentStatus("completed");
            session.setTotalAmount(session.getCurrentCost());
            session.setPaymentMethod("card");
            
            // Calculate duration
            session.setDurationHrs(calculateDurationHours(session.getStartTime(), session.getEndTime()));
            
            // Update the adapter with the modified session
            sessionAdapter.updateSession(session);
            updateSessionSummary();
            
            android.util.Log.d("MySessionsActivity", "Session simulated checkout: " + 
                sessionId + ", payment_status: " + session.getPaymentStatus());
        }
        
        Toast.makeText(this, "Checkout completed successfully!", Toast.LENGTH_SHORT).show();
        
        // Move session from active to completed list directly (no reload flicker)
        runOnUiThread(() -> {
            ParkingSession s = findSessionById(sessionId);
            if (s != null) {
                activeSessions.remove(s);
                if (!completedSessions.contains(s)) completedSessions.add(s);
            }
            updateTabDisplay();
            updateSessionSummary();
        });
    }
    
    private void showPaymentSuccessDialog(PaymentInfo paymentInfo) {
        String message = "Payment Successful!\n\n" +
                        "Session ID: " + paymentInfo.getTicketId() + "\n" +
                        "Duration: " + paymentInfo.getDuration() + "\n" +
                        "Total Amount: €" + String.format("%.2f", paymentInfo.getTotalAmount()) + "\n" +
                        "Payment Method: " + paymentInfo.getPaymentMethod();
        
        new AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View Receipt", (dialog, which) -> {
                // TODO: Open receipt URL if available
                if (paymentInfo.getReceiptUrl() != null) {
                    // Open receipt in browser or show receipt activity
                    Toast.makeText(this, "Receipt feature coming soon", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }
    
    private double calculateDurationHours(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) return 0.0;
        
        long durationMillis = endTime.getTime() - startTime.getTime();
        return durationMillis / (1000.0 * 60 * 60); // Convert to hours
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound && serviceConnection != null) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 