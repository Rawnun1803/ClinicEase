// /app/src/main/java/com/clinicease/app/RequestAppointmentActivity.java
package com.clinicease.app;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.clinicease.app.repository.FirestoreRepository;

import java.util.*;

public class RequestAppointmentActivity extends AppCompatActivity {

    private Button btnFindSlots;
    private LinearLayout slotsContainer;
    private FirestoreRepository repo;
    private long selectedDateMillis = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_appointment);
        btnFindSlots = findViewById(R.id.btn_find_slots);
        slotsContainer = findViewById(R.id.slots_container);
        repo = FirestoreRepository.getInstance();

        btnFindSlots.setOnClickListener(v -> findSlots());
    }

    private void findSlots() {
        slotsContainer.removeAllViews();
        // For demo, create 3 mock slots
        List<String[]> mock = Arrays.asList(new String[]{"Nov 8, 2025", "09:00 AM"}, new String[]{"Nov 8, 2025", "14:00 PM"}, new String[]{"Nov 9, 2025", "10:30 AM"});
        for (String[] s : mock) {
            View item = getLayoutInflater().inflate(R.layout.item_slot_card, slotsContainer, false);
            TextView tvDate = item.findViewById(R.id.slot_date);
            TextView tvTime = item.findViewById(R.id.slot_time);
            Button btnSelect = item.findViewById(R.id.btn_select_slot);
            tvDate.setText(s[0]);
            tvTime.setText(s[1]);
            btnSelect.setOnClickListener(v -> {
                // Confirm - for demo just show toast and finish
                Toast.makeText(this, "Slot selected: " + s[0] + " " + s[1], Toast.LENGTH_SHORT).show();
                finish();
            });
            slotsContainer.addView(item);
        }
    }
}
