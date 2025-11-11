package com.clinicease.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText etWorkStart, etWorkEnd, etSlotDuration;
    private SwitchCompat swNotifications; // <-- use SwitchCompat
    private Button btnSave, btnSyncNow;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etWorkStart = findViewById(R.id.et_work_start);
        etWorkEnd = findViewById(R.id.et_work_end);
        etSlotDuration = findViewById(R.id.et_slot_duration);
        swNotifications = findViewById(R.id.sw_notifications); // no cast needed with generics
        btnSave = findViewById(R.id.btn_save_settings);
        btnSyncNow = findViewById(R.id.btn_sync_now);

        db = FirebaseFirestore.getInstance();

        // loadSettings can be left as-is; if it touches firestore and crashes,
        // handle errors inside that method. For now, keep it:
        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        btnSyncNow.setOnClickListener(v -> {
            // If SyncManager exists; wrap in try/catch to be safe
            try {
                com.clinicease.app.sync.SyncManager.triggerImmediateSync(this);
                Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Sync failed to start", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void loadSettings() {
        db.collection("doctor").document("main").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String ws = task.getResult().getString("workStart");
                String we = task.getResult().getString("workEnd");
                Long slot = task.getResult().getLong("slotDurationMin");
                etWorkStart.setText(ws == null ? "09:00" : ws);
                etWorkEnd.setText(we == null ? "17:00" : we);
                etSlotDuration.setText(slot == null ? "30" : Long.toString(slot));
                swNotifications.setChecked(true);
            } else {
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
