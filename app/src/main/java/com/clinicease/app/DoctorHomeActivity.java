package com.clinicease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clinicease.app.repository.FirestoreRepository;
import com.clinicease.app.model.Appointment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DoctorHomeActivity - shows today's appointments
 */
public class DoctorHomeActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private FirestoreRepository repo;
    private TextView tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        rvAppointments = findViewById(R.id.rv_appointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(new ArrayList<>(), this::onAppointmentAction, this::onPatientClick);
        rvAppointments.setAdapter(adapter);

        tvDate = findViewById(R.id.tv_date);

        repo = FirestoreRepository.getInstance();

        // Conflict banner (uses simple ConflictManager helper)
        try {
            List<String> conflicts = ConflictManager.getInstance(this).getConflicts();
            if (conflicts != null && !conflicts.isEmpty()) {
                final TextView tvBanner = findViewById(R.id.tv_conflict_banner);
                if (tvBanner != null) {
                    tvBanner.setVisibility(View.VISIBLE);
                    tvBanner.setOnClickListener(v -> {
                        // show dialog listing conflicts
                        new AlertDialog.Builder(DoctorHomeActivity.this)
                                .setTitle("Sync Conflicts")
                                .setItems(conflicts.toArray(new String[0]), null)
                                .setPositiveButton("Clear", (d, w) -> {
                                    ConflictManager.getInstance(DoctorHomeActivity.this).clearConflicts();
                                    tvBanner.setVisibility(View.GONE);
                                }).show();
                    });
                }
            }
        } catch (Exception e) {
            // If ConflictManager isn't available or something goes wrong, ignore and continue.
            e.printStackTrace();
        }

        findViewById(R.id.btn_settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v -> finish());

        tvDate.setText("Today, " + DateTimeUtils.getFriendlyDate(System.currentTimeMillis()));

        loadTodayAppointments();
    }

    private void loadTodayAppointments() {
        long dayStart = DateTimeUtils.getDayStartMillis(System.currentTimeMillis());
        long dayEnd = DateTimeUtils.getDayEndMillis(System.currentTimeMillis());

        // repo.getAppointmentsForDate should return a Task<QuerySnapshot>
        repo.getAppointmentsForDate(dayStart, dayEnd).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot qs = task.getResult();
                    if (qs != null) {
                        List<DocumentSnapshot> docs = qs.getDocuments();
                        List<Appointment> items = new ArrayList<>();
                        if (docs != null) {
                            for (DocumentSnapshot d : docs) {
                                if (d == null) continue;
                                Appointment a = d.toObject(Appointment.class);
                                if (a != null) {
                                    // ensure the id is copied from the document snapshot
                                    a.id = d.getId();
                                    items.add(a);
                                }
                            }
                        }
                        // sort by startTs (use Collections.sort for API 21 compatibility)
                        Collections.sort(items, new Comparator<Appointment>() {
                            @Override
                            public int compare(Appointment a1, Appointment a2) {
                                long diff = a1.startTs - a2.startTs;
                                if (diff < 0) return -1;
                                if (diff > 0) return 1;
                                return 0;
                            }
                        });
                        adapter.update(items);
                    } else {
                        Toast.makeText(DoctorHomeActivity.this, "No appointments found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DoctorHomeActivity.this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onAppointmentAction(String appointmentId, String action) {
        // action: edit | extend | cancel | done
        Toast.makeText(this, "Action " + action + " on " + appointmentId, Toast.LENGTH_SHORT).show();
        // TODO: implement extend, cancel and batch update logic with Firestore
    }

    private void onPatientClick(String patientId) {
        Intent i = new Intent(this, PatientProfileActivity.class);
        i.putExtra("patientId", patientId);
        startActivity(i);
    }
}
