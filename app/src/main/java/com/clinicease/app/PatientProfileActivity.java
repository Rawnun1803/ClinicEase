// /app/src/main/java/com/clinicease/app/PatientProfileActivity.java
package com.clinicease.app;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textview.MaterialTextView;

import java.util.*;

public class PatientProfileActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvLastVisit;
    private LinearLayout historyContainer;
    private FirebaseFirestore db;
    private String patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_profile);

        tvName = findViewById(R.id.pp_name);
        tvPhone = findViewById(R.id.pp_phone);
        tvLastVisit = findViewById(R.id.pp_last_visit);
        historyContainer = findViewById(R.id.pp_history_container);

        db = FirebaseFirestore.getInstance();

        patientId = getIntent().getStringExtra("patientId");
        if (patientId == null) {
            Toast.makeText(this, "No patient selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPatientData();
    }

    private void loadPatientData() {
        db.collection("patients").document(patientId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot ds = task.getResult();
                if (ds != null && ds.exists()) {
                    String name = ds.getString("name");
                    String phone = ds.getString("phone");
                    Long lastVisit = ds.getLong("lastVisit");
                    tvName.setText(name == null ? "Unknown" : name);
                    tvPhone.setText(phone == null ? "-" : phone);
                    tvLastVisit.setText(lastVisit == null ? "-" : DateTimeUtils.getFriendlyDate(lastVisit));
                } else {
                    tvName.setText("Unknown");
                    tvPhone.setText("-");
                    tvLastVisit.setText("-");
                }
            } else {
                Toast.makeText(this, "Failed to load patient", Toast.LENGTH_SHORT).show();
            }
        });

        // Load visit history — for demo we pull appointments for this patient
        long start = DateTimeUtils.getDayStartMillis(System.currentTimeMillis() - 365L * 24 * 3600 * 1000); // last year
        long end = DateTimeUtils.getDayEndMillis(System.currentTimeMillis());
        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .whereGreaterThanOrEqualTo("startTs", start)
                .whereLessThanOrEqualTo("startTs", end)
                .orderBy("startTs", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyContainer.removeAllViews();
                    for (DocumentSnapshot ds : queryDocumentSnapshots.getDocuments()) {
                        Long ts = ds.getLong("startTs");
                        String reason = ds.getString("reason");
                        View item = getLayoutInflater().inflate(R.layout.item_visit_history, historyContainer, false);
                        TextView tDate = item.findViewById(R.id.visit_date);
                        TextView tReason = item.findViewById(R.id.visit_reason);
                        tDate.setText(ts == null ? "-" : DateTimeUtils.getFriendlyDate(ts));
                        tReason.setText(reason == null ? "-" : reason);
                        historyContainer.addView(item);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
                });
    }
}
