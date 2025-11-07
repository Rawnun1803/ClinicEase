// /app/src/main/java/com/clinicease/app/PatientHomeActivity.java
package com.clinicease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clinicease.app.repository.FirestoreRepository;
import com.clinicease.app.model.Appointment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.*;

public class PatientHomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnRequest;
    private RecyclerView rvUpcoming;
    private UpcomingAdapter adapter;
    private FirestoreRepository repo;
    private String patientName = "You";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_home);

        tvWelcome = findViewById(R.id.tv_welcome);
        btnRequest = findViewById(R.id.btn_request_appointment);
        rvUpcoming = findViewById(R.id.rv_upcoming);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UpcomingAdapter(new ArrayList<>());
        rvUpcoming.setAdapter(adapter);

        repo = FirestoreRepository.getInstance();

        // If you have user profile data, show name; else "Welcome"
        // For demo: static "John"
        patientName = "John";
        tvWelcome.setText("Welcome, " + patientName);

        btnRequest.setOnClickListener(v -> {
            startActivity(new Intent(this, RequestAppointmentActivity.class));
        });

        loadUpcomingAppointments();
    }

    private void loadUpcomingAppointments() {
        long now = System.currentTimeMillis();
        long future = now + 1000L * 60 * 60 * 24 * 365; // 1 year; or limit to 30 days
        // For demo query all upcoming where startTs >= now
        repo.getAppointmentsForDate(DateTimeUtils.getDayStartMillis(now), DateTimeUtils.getDayEndMillis(now + 7L * 24 * 3600 * 1000))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        List<Appointment> items = new ArrayList<>();
                        for (DocumentSnapshot d : docs) {
                            Appointment a = d.toObject(Appointment.class);
                            if (a != null) {
                                a.id = d.getId();
                                // Filter to only show appointments for this patient if needed.
                                // For demo, add all
                                items.add(a);
                            }
                        }
                        // Sort by startTs
                        items.sort(Comparator.comparingLong(ap -> ap.startTs));
                        adapter.update(items);
                    } else {
                        Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Adapter to render upcoming appointments
    static class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.VH> {
        private List<Appointment> items;
        UpcomingAdapter(List<Appointment> items) { this.items = items; }
        void update(List<Appointment> newItems) { this.items = newItems; notifyDataSetChanged(); }

        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upcoming_appointment, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH holder, int position) {
            Appointment a = items.get(position);
            holder.tvDate.setText(DateTimeUtils.getFriendlyDate(a.startTs));
            holder.tvTime.setText(DateTimeUtils.getTimeString(a.startTs));
            holder.tvDoctor.setText(a.patientName == null ? "Dr. Sarah Johnson" : a.patientName); // use patientName for demo; ideally doctorName field
            holder.tvReason.setText(a.reason);
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvTime, tvDoctor, tvReason;
            VH(android.view.View v) {
                super(v);
                tvDate = v.findViewById(R.id.up_date);
                tvTime = v.findViewById(R.id.up_time);
                tvDoctor = v.findViewById(R.id.up_doctor);
                tvReason = v.findViewById(R.id.up_reason);
            }
        }
    }
}
