// /app/src/main/java/com/clinicease/app/SettingsActivity.java
package com.clinicease.app;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.clinicease.app.model.DoctorSettings;
import com.clinicease.app.sync.SyncManager;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText etWorkStart, etWorkEnd, etSlotDuration;
    private Switch swNotifications;
    private Button btnSave, btnSyncNow;
    private FirebaseFirestore db;
    private DoctorSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etWorkStart = findViewById(R.id.et_work_start);
        etWorkEnd = findViewById(R.id.et_work_end);
        etSlotDuration = findViewById(R.id.et_slot_duration);
        swNotifications = findViewById(R.id.sw_notifications);
        btnSave = findViewById(R.id.btn_save_settings);
        btnSyncNow = findViewById(R.id.btn_sync_now);

        db = FirebaseFirestore.getInstance();

        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        btnSyncNow.setOnClickListener(v -> {
            SyncManager.triggerImmediateSync(this);
            Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        db.collection("doctor").document("main").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentReference dr = task.getResult().getReference();
                String ws = task.getResult().getString("workStart");
                String we = task.getResult().getString("workEnd");
                Long slot = task.getResult().getLong("slotDurationMin");
                etWorkStart.setText(ws == null ? "09:00" : ws);
                etWorkEnd.setText(we == null ? "17:00" : we);
                etSlotDuration.setText(slot == null ? "30" : Long.toString(slot));
                // notifications default true
                swNotifications.setChecked(true);
            } else {
                // defaults
                etWorkStart.setText("09:00");
                etWorkEnd.setText("17:00");
                etSlotDuration.setText("30");
                swNotifications.setChecked(true);
            }
        });
    }

    private void saveSettings() {
        String ws = etWorkStart.getText().toString().trim();
        String we = etWorkEnd.getText().toString().trim();
        String slot = etSlotDuration.getText().toString().trim();
        boolean notify = swNotifications.isChecked();
        if (ws.isEmpty() || we.isEmpty() || slot.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        int slotInt = 30;
        try { slotInt = Integer.parseInt(slot); } catch (Exception ignored) {}

        Map<String, Object> data = new HashMap<>();
        data.put("workStart", ws);
        data.put("workEnd", we);
        data.put("slotDurationMin", slotInt);
        data.put("notifications", notify);
        data.put("updatedAt", System.currentTimeMillis());

        db.collection("doctor").document("main").set(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
