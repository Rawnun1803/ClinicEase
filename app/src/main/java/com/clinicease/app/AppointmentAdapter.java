// /app/src/main/java/com/clinic ease/app/AppointmentAdapter.java
package com.clinicease.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clinicease.app.model.Appointment;

import java.text.SimpleDateFormat;
import java.util.*;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {

    public interface ActionListener {
        void onAction(String appointmentId, String action);
    }

    public interface PatientClickListener {
        void onPatientClick(String patientId);
    }

    private List<Appointment> items;
    private ActionListener actionListener;
    private PatientClickListener patientClickListener;

    public AppointmentAdapter(List<Appointment> items, ActionListener actionListener, PatientClickListener patientClickListener) {
        this.items = items;
        this.actionListener = actionListener;
        this.patientClickListener = patientClickListener;
    }

    public void update(List<Appointment> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Appointment a = items.get(position);
        holder.tvTime.setText(DateTimeUtils.getTimeString(a.startTs));
        holder.tvPatient.setText(a.patientName);
        holder.tvReason.setText(a.reason);
        holder.btnEdit.setOnClickListener(v -> actionListener.onAction(a.id, "edit"));
        holder.btnExtend.setOnClickListener(v -> actionListener.onAction(a.id, "extend"));
        holder.btnCancel.setOnClickListener(v -> actionListener.onAction(a.id, "cancel"));
        holder.btnDone.setOnClickListener(v -> actionListener.onAction(a.id, "done"));

        holder.itemView.setOnClickListener(v -> patientClickListener.onPatientClick(a.patientId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvPatient, tvReason;
        Button btnEdit, btnExtend, btnCancel, btnDone;

        VH(@NonNull View v) {
            super(v);
            tvTime = v.findViewById(R.id.item_time);
            tvPatient = v.findViewById(R.id.item_patient);
            tvReason = v.findViewById(R.id.item_reason);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnExtend = v.findViewById(R.id.btn_extend);
            btnCancel = v.findViewById(R.id.btn_cancel);
            btnDone = v.findViewById(R.id.btn_done);
        }
    }
}
