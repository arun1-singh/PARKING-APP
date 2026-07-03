package com.example.visionpark.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.visionpark.R;
import com.google.android.material.button.MaterialButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        double amount       = getIntent().getDoubleExtra("amount", 0.0);
        String txnId        = getIntent().getStringExtra("txn_id");
        String method       = getIntent().getStringExtra("payment_method");
        String lotName      = getIntent().getStringExtra("parking_lot_name");
        String paidAt       = getIntent().getStringExtra("paid_at");
        String paymentFor   = getIntent().getStringExtra("payment_for");

        // Amount
        TextView tvAmount = findViewById(R.id.tvPaidAmount);
        tvAmount.setText(amount == (long) amount
                ? "Rs. " + (long) amount
                : String.format(Locale.getDefault(), "Rs. %.2f", amount));

        // TXN ID
        ((TextView) findViewById(R.id.tvTxnId)).setText(txnId != null ? txnId : "—");

        // Method
        String methodDisplay = method != null ? method.toUpperCase() : "—";
        ((TextView) findViewById(R.id.tvPayMethod)).setText(methodDisplay);

        // Parking lot
        ((TextView) findViewById(R.id.tvParkingLot)).setText(lotName != null ? lotName : "—");

        // Paid at
        ((TextView) findViewById(R.id.tvPaidAt)).setText(formatDateTime(paidAt));

        // Done button — go back to appropriate screen
        MaterialButton btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            Intent intent;
            if ("booking".equals(paymentFor)) {
                intent = new Intent(this, BookingsActivity.class);
            } else {
                intent = new Intent(this, MySessionsActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String formatDateTime(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso;
        } catch (ParseException e) {
            return iso;
        }
    }
}
