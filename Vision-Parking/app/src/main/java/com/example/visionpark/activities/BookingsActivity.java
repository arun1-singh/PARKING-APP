package com.example.visionpark.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.visionpark.R;
import com.example.visionpark.adapters.BookingAdapter;
import com.example.visionpark.models.ParkingBooking;
import com.example.visionpark.models.UserVehicle;
import com.example.visionpark.network.ApiClient;
import com.example.visionpark.network.ApiService;
import com.example.visionpark.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BookingsActivity extends AppCompatActivity implements BookingAdapter.OnBookingActionListener {

    private static final String TAG = "BookingsActivity";
    private static final String BASE_URL = "http://10.0.2.2:5000/api/v1";

    // Views
    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutLoading, layoutEmpty;
    private TextView tvSummary, tvEmptyMessage;
    private TabLayout tabLayout;

    // Data
    private List<ParkingBooking> allBookings = new ArrayList<>();
    private List<ParkingBooking> upcomingBookings = new ArrayList<>();
    private List<ParkingBooking> pastBookings = new ArrayList<>();
    private List<UserVehicle> userVehicles = new ArrayList<>();
    private int currentTab = 0; // 0=Upcoming, 1=Past

    // HTTP
    private OkHttpClient httpClient;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Bookings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        authToken = TokenManager.getToken(this);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        initViews();
        setupTabs();
        setupBottomNav();
        loadVehicles();
        loadBookings();
    }

    private void initViews() {
        recyclerView    = findViewById(R.id.recyclerViewBookings);
        swipeRefresh    = findViewById(R.id.swipeRefreshLayout);
        layoutLoading   = findViewById(R.id.layoutLoading);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        tvSummary       = findViewById(R.id.tvBookingSummary);
        tvEmptyMessage  = findViewById(R.id.tvEmptyMessage);
        tabLayout       = findViewById(R.id.tabLayout);

        adapter = new BookingAdapter(this, new ArrayList<>());
        adapter.setListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.green);
        swipeRefresh.setOnRefreshListener(this::loadBookings);

        MaterialButton btnBook = findViewById(R.id.btnBookNewSlot);
        btnBook.setOnClickListener(v -> showCreateBookingDialog());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        tabLayout.addTab(tabLayout.newTab().setText("Past"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setSelectedItemId(R.id.nav_bookings);
        nav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish(); return true;
            } else if (id == R.id.nav_sessions) {
                startActivity(new Intent(this, MySessionsActivity.class)); return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class)); return true;
            }
            return id == R.id.nav_bookings;
        });
    }

    // ─── Load Data ────────────────────────────────────────────────────────────

    private void loadVehicles() {
        if (authToken == null) return;
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(BASE_URL + "/user/vehicles")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .build();
                Response resp = httpClient.newCall(req).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    JSONObject json = new JSONObject(resp.body().string());
                    JSONArray data = json.getJSONArray("data");
                    userVehicles.clear();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject v = data.getJSONObject(i);
                        UserVehicle uv = new UserVehicle();
                        uv.setVehicleId(v.getInt("vehicle_id"));
                        uv.setRegistrationNumber(v.optString("registration_number", ""));
                        uv.setVehicleName(v.optString("vehicle_name", ""));
                        uv.setVehicleType(v.optString("vehicle_type", "car"));
                        userVehicles.add(uv);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "loadVehicles error: " + e.getMessage());
            }
        }).start();
    }

    private void loadBookings() {
        if (authToken == null) { showEmpty("Please login to view bookings"); return; }
        showLoading(true);

        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(BASE_URL + "/user/bookings")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .build();
                Response resp = httpClient.newCall(req).execute();
                if (!resp.isSuccessful() || resp.body() == null) {
                    runOnUiThread(() -> { showLoading(false); showEmpty("Failed to load bookings"); });
                    return;
                }
                JSONObject json = new JSONObject(resp.body().string());
                JSONArray data = json.getJSONArray("data");

                allBookings.clear(); upcomingBookings.clear(); pastBookings.clear();

                for (int i = 0; i < data.length(); i++) {
                    JSONObject b = data.getJSONObject(i);
                    ParkingBooking booking = parseBooking(b);
                    allBookings.add(booking);
                    if (booking.isConfirmed() || booking.isCheckedIn()) {
                        upcomingBookings.add(booking);
                    } else {
                        pastBookings.add(booking);
                    }
                }

                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    updateDisplay();
                    updateSummary();
                });
            } catch (Exception e) {
                Log.e(TAG, "loadBookings error: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    showEmpty("Error loading bookings");
                });
            }
        }).start();
    }

    private ParkingBooking parseBooking(JSONObject b) throws Exception {
        ParkingBooking booking = new ParkingBooking();
        // Use reflection-free approach via Gson
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(b.toString(), ParkingBooking.class);
    }

    private void updateDisplay() {
        List<ParkingBooking> toShow = currentTab == 0 ? upcomingBookings : pastBookings;
        if (toShow.isEmpty()) {
            String msg = currentTab == 0 ? "No upcoming bookings\nTap '+ Book a Parking Slot' to reserve" : "No past bookings";
            showEmpty(msg);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateBookings(new ArrayList<>(toShow));
        }
    }

    private void updateSummary() {
        tvSummary.setText(upcomingBookings.size() + " upcoming • " + allBookings.size() + " total");
    }

    // ─── Create Booking Dialog ────────────────────────────────────────────────

    private void showCreateBookingDialog() {
        if (userVehicles.isEmpty()) {
            Toast.makeText(this, "Please add a vehicle first", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Vehicle spinner
        Spinner spinnerVehicle = dialogView.findViewById(R.id.spinnerVehicle);
        List<String> vehicleLabels = new ArrayList<>();
        for (UserVehicle v : userVehicles) {
            String label = v.getRegistrationNumber();
            if (v.getVehicleName() != null && !v.getVehicleName().isEmpty())
                label = v.getVehicleName() + " (" + label + ")";
            vehicleLabels.add(label);
        }
        spinnerVehicle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicleLabels));

        // Parking lot spinner (use nearby lots or a fixed list)
        Spinner spinnerLot = dialogView.findViewById(R.id.spinnerParkingLot);
        List<String> lotLabels = new ArrayList<>();
        List<Integer> lotIds = new ArrayList<>();
        // Populate with some known lots — in production you'd fetch from API
        lotLabels.add("Jahangirpuri - Metro Parking (ID:1)"); lotIds.add(1);
        lotLabels.add("Azadpur - Commercial Complex (ID:2)"); lotIds.add(2);
        lotLabels.add("ISBT Kashmere Gate (ID:3)"); lotIds.add(3);
        lotLabels.add("Madhuban Chowk (ID:4)"); lotIds.add(4);
        lotLabels.add("Kohat Enclave Metro (ID:5)"); lotIds.add(5);
        spinnerLot.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lotLabels));

        // Date/time state
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1); // default: tomorrow
        final int[] selectedHourStart = {10}, selectedMinStart = {0};
        final int[] selectedHourEnd   = {13}, selectedMinEnd   = {0};
        final int[] selectedYear  = {cal.get(Calendar.YEAR)};
        final int[] selectedMonth = {cal.get(Calendar.MONTH)};
        final int[] selectedDay   = {cal.get(Calendar.DAY_OF_MONTH)};

        MaterialButton btnDate      = dialogView.findViewById(R.id.btnPickDate);
        MaterialButton btnStartTime = dialogView.findViewById(R.id.btnPickStartTime);
        MaterialButton btnEndTime   = dialogView.findViewById(R.id.btnPickEndTime);
        MaterialButton btnCheckMain = dialogView.findViewById(R.id.btnCheckAvailabilityMain);
        MaterialButton btnCheckInCard = dialogView.findViewById(R.id.btnCheckAvailability); // green button inside card
        MaterialButton btnConfirm   = dialogView.findViewById(R.id.btnConfirmBooking);
        View cardAvail              = dialogView.findViewById(R.id.cardAvailability);
        TextView tvAvailSlots       = dialogView.findViewById(R.id.tvAvailableSlots);
        TextView tvEstCost          = dialogView.findViewById(R.id.tvEstimatedCost);

        // Update button labels
        Runnable updateLabels = () -> {
            btnDate.setText(String.format(Locale.getDefault(), "%d/%02d/%02d",
                    selectedYear[0], selectedMonth[0]+1, selectedDay[0]));
            btnStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s",
                    selectedHourStart[0] % 12 == 0 ? 12 : selectedHourStart[0] % 12,
                    selectedMinStart[0], selectedHourStart[0] < 12 ? "AM" : "PM"));
            btnEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s",
                    selectedHourEnd[0] % 12 == 0 ? 12 : selectedHourEnd[0] % 12,
                    selectedMinEnd[0], selectedHourEnd[0] < 12 ? "AM" : "PM"));
        };
        updateLabels.run();

        btnDate.setOnClickListener(v -> new DatePickerDialog(this,
                (dp, y, m, d) -> { selectedYear[0]=y; selectedMonth[0]=m; selectedDay[0]=d; updateLabels.run(); cardAvail.setVisibility(View.GONE); btnConfirm.setEnabled(false); btnConfirm.setAlpha(0.6f); btnCheckMain.setText("Check Availability"); },
                selectedYear[0], selectedMonth[0], selectedDay[0]).show());

        btnStartTime.setOnClickListener(v -> new TimePickerDialog(this,
                (tp, h, m) -> { selectedHourStart[0]=h; selectedMinStart[0]=m; updateLabels.run(); cardAvail.setVisibility(View.GONE); btnConfirm.setEnabled(false); btnConfirm.setAlpha(0.6f); btnCheckMain.setText("Check Availability"); },
                selectedHourStart[0], selectedMinStart[0], true).show());

        btnEndTime.setOnClickListener(v -> new TimePickerDialog(this,
                (tp, h, m) -> { selectedHourEnd[0]=h; selectedMinEnd[0]=m; updateLabels.run(); cardAvail.setVisibility(View.GONE); btnConfirm.setEnabled(false); btnConfirm.setAlpha(0.6f); btnCheckMain.setText("Check Availability"); },
                selectedHourEnd[0], selectedMinEnd[0], true).show());

        // ── Check availability logic (shared by both buttons) ──────────────
        Runnable doCheckAvailability = () -> {
            int lotIdx = spinnerLot.getSelectedItemPosition();
            int lotId  = lotIds.get(lotIdx);
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", selectedYear[0], selectedMonth[0]+1, selectedDay[0]);
            String st   = String.format(Locale.getDefault(), "%02d:%02d", selectedHourStart[0], selectedMinStart[0]);
            String et   = String.format(Locale.getDefault(), "%02d:%02d", selectedHourEnd[0], selectedMinEnd[0]);

            btnCheckMain.setText("Checking...");
            btnCheckMain.setEnabled(false);
            btnCheckInCard.setText("...");
            btnCheckInCard.setEnabled(false);

            new Thread(() -> {
                try {
                    String url = BASE_URL + "/parking/lots/" + lotId + "/availability?date=" + date + "&start_time=" + st + "&end_time=" + et;
                    Request req = new Request.Builder().url(url)
                            .addHeader("Authorization", "Bearer " + authToken).build();
                    Response resp = httpClient.newCall(req).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        JSONObject json = new JSONObject(resp.body().string());
                        JSONObject data = json.getJSONObject("data");
                        int avail = data.getInt("available_slots");
                        double cost = data.getDouble("estimated_cost");
                        boolean isAvail = data.getBoolean("is_available");

                        runOnUiThread(() -> {
                            cardAvail.setVisibility(View.VISIBLE);
                            tvAvailSlots.setText(avail + " slots available");
                            tvEstCost.setText("Estimated: Rs." + (int)cost);
                            btnConfirm.setEnabled(isAvail);
                            btnConfirm.setAlpha(isAvail ? 1.0f : 0.6f);
                            btnCheckMain.setText("Check Availability");
                            btnCheckMain.setEnabled(true);
                            btnCheckInCard.setText("Check");
                            btnCheckInCard.setEnabled(true);
                            if (!isAvail) Toast.makeText(this, "No slots available for this time", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            btnCheckMain.setText("Check Availability"); btnCheckMain.setEnabled(true);
                            btnCheckInCard.setText("Check"); btnCheckInCard.setEnabled(true);
                            Toast.makeText(this, "Server error checking availability", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnCheckMain.setText("Check Availability"); btnCheckMain.setEnabled(true);
                        btnCheckInCard.setText("Check"); btnCheckInCard.setEnabled(true);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        };

        // Wire BOTH buttons to the same check logic
        btnCheckMain.setOnClickListener(v -> doCheckAvailability.run());
        btnCheckInCard.setOnClickListener(v -> doCheckAvailability.run());

        // Confirm booking
        btnConfirm.setOnClickListener(v -> {
            int lotIdx = spinnerLot.getSelectedItemPosition();
            int vehIdx = spinnerVehicle.getSelectedItemPosition();
            int lotId  = lotIds.get(lotIdx);
            int vehicleId = userVehicles.get(vehIdx).getVehicleId();

            String scheduledStart = String.format(Locale.getDefault(), "%d-%02d-%02dT%02d:%02d:00",
                    selectedYear[0], selectedMonth[0]+1, selectedDay[0], selectedHourStart[0], selectedMinStart[0]);
            String scheduledEnd = String.format(Locale.getDefault(), "%d-%02d-%02dT%02d:%02d:00",
                    selectedYear[0], selectedMonth[0]+1, selectedDay[0], selectedHourEnd[0], selectedMinEnd[0]);

            btnConfirm.setText("Booking...");
            btnConfirm.setEnabled(false);

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("parkinglot_id", lotId);
                    body.put("vehicle_id", vehicleId);
                    body.put("scheduled_start", scheduledStart);
                    body.put("scheduled_end", scheduledEnd);
                    body.put("payment_method", "card");

                    Request req = new Request.Builder()
                            .url(BASE_URL + "/user/bookings")
                            .addHeader("Authorization", "Bearer " + authToken)
                            .addHeader("Content-Type", "application/json")
                            .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                            .build();

                    Response resp = httpClient.newCall(req).execute();
                    String respBody = resp.body() != null ? resp.body().string() : "{}";
                    JSONObject json = new JSONObject(respBody);

                    runOnUiThread(() -> {
                        if (resp.isSuccessful() && json.optBoolean("success", false)) {
                            dialog.dismiss();
                            Toast.makeText(this, "Booking confirmed! 🎉", Toast.LENGTH_LONG).show();
                            loadBookings();
                        } else {
                            String err = json.optString("error", "Booking failed");
                            btnConfirm.setText("Confirm Booking");
                            btnConfirm.setEnabled(true);
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> { btnConfirm.setText("Confirm Booking"); btnConfirm.setEnabled(true); Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
                }
            }).start();
        });

        dialog.show();
    }

    // ─── Booking Actions ──────────────────────────────────────────────────────

    @Override
    public void onCancelBooking(ParkingBooking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Cancel booking " + booking.getBookingId() + " at " + booking.getParkingLotName() + "?")
                .setPositiveButton("Yes, Cancel", (d, w) -> cancelBooking(booking))
                .setNegativeButton("Keep Booking", null)
                .show();
    }

    @Override
    public void onCheckinBooking(ParkingBooking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Check In")
                .setMessage("Check in now at " + booking.getParkingLotName() + "?\n\nYou can pay after checkout.")
                .setPositiveButton("Check In", (d, w) -> checkinBooking(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelBooking(ParkingBooking booking) {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(BASE_URL + "/user/bookings/" + booking.getBookingId() + "?reason=Cancelled+by+user")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .delete()
                        .build();
                Response resp = httpClient.newCall(req).execute();
                String body = resp.body() != null ? resp.body().string() : "{}";
                Log.d(TAG, "Cancel response " + resp.code() + ": " + body);
                runOnUiThread(() -> {
                    if (resp.isSuccessful()) {
                        Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                        loadBookings();
                    } else {
                        try {
                            JSONObject json = new JSONObject(body);
                            String err = json.optString("error", json.optString("msg", "Failed to cancel"));
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Cancel error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void checkinBooking(ParkingBooking booking) {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(BASE_URL + "/user/bookings/" + booking.getBookingId() + "/checkin")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create("{}", MediaType.parse("application/json")))
                        .build();
                Response resp = httpClient.newCall(req).execute();
                String body = resp.body() != null ? resp.body().string() : "{}";
                Log.d(TAG, "Check-in response " + resp.code() + ": " + body);

                JSONObject json = new JSONObject(body);

                // Extract error from either {"detail":{"error":"..."}} or {"error":"..."} or {"msg":"..."}
                String errorMsg = null;
                if (!resp.isSuccessful()) {
                    if (json.has("detail")) {
                        Object detail = json.get("detail");
                        if (detail instanceof JSONObject) {
                            errorMsg = ((JSONObject) detail).optString("error", null);
                        } else {
                            errorMsg = detail.toString();
                        }
                    }
                    if (errorMsg == null) errorMsg = json.optString("error", json.optString("msg", "Check-in failed (HTTP " + resp.code() + ")"));
                }

                final String finalError = errorMsg;
                runOnUiThread(() -> {
                    if (resp.isSuccessful() && json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        String ticketId = data != null ? data.optString("ticket_id", "") : "";
                        String slot = data != null ? data.optString("slot_location", "") : "";
                        Toast.makeText(this,
                            "Checked in! 🎉\nTicket: " + ticketId + "\n" + slot,
                            Toast.LENGTH_LONG).show();
                        loadBookings();
                    } else {
                        Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Check-in error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        tvEmptyMessage.setText(message);
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
