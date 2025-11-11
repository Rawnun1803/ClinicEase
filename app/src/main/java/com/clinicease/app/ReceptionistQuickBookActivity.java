// /app/src/main/java/com/clinicease/app/ReceptionistQuickBookActivity.java
package com.clinicease.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.clinicease.app.repository.FirestoreRepository;
import com.google.android.material.textfield.TextInputEditText;

import java.util.*;

public class ReceptionistQuickBookActivity extends AppCompatActivity {
    private TextInputEditText etPatientName, etPhone, etReason;
    private EditText etDate, etTime;
    private Button btnCheck, btnBook;
    private TextView tvConflict;
    private FirestoreRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_book);

        etPatientName = findViewById(R.id.input_patient_name);
        etPhone = findViewById(R.id.input_phone);
        etReason = findViewById(R.id.input_reason);
        etDate = findViewById(R.id.input_date);
        etTime = findViewById(R.id.input_time);
        btnCheck = findViewById(R.id.btn_check);
        btnBook = findViewById(R.id.btn_book);
        tvConflict = findViewById(R.id.tv_conflict);
        repo = FirestoreRepository.getInstance();

        btnCheck.setOnClickListener(v -> checkAvailability());
        btnBook.setOnClickListener(v -> bookAppointment());
    }

    private void checkAvailability() {
        tvConflict.setVisibility(View.GONE);
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();
        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Select date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        // For demo: simulate conflict if time equals 14:00
        if (time.equals("14:00")) {
            tvConflict.setText("Time slot already booked. Please choose another time.");
            tvConflict.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Slot available", Toast.LENGTH_SHORT).show();
        }
    }

    private void bookAppointment() {
        String name = etPatientName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || date.isEmpty() || time.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tvConflict.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Fix conflict before booking", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert date+time to epoch millis (simple parse; expects YYYY-MM-DD and HH:mm)
        long startTs = DateTimeUtils.parseDateTimeToMillis(date, time);
        long slotMs = 30 * 60 * 1000L;
        long endTs = startTs + slotMs;

        Map<String, Object> data = new HashMap<>();
        data.put("patientName", name);
        data.put("patientId", UUID.randomUUID().toString());
        data.put("startTs", startTs);
        data.put("endTs", endTs);
        data.put("status", "BOOKED");
        data.put("reason", reason);
        data.put("createdBy", "receptionist");
        data.put("updatedAt", System.currentTimeMillis());

        repo.createAppointment(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Appointment booked successfully", Toast.LENGTH_SHORT).show();
                // clear form
                etPatientName.setText("");
                etPhone.setText("");
                etDate.setText("");
                etTime.setText("");
                etReason.setText("");
            } else {
                Toast.makeText(this, "Booking failed: " + (task.getException() == null ? "unknown" : task.getException().getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }
}