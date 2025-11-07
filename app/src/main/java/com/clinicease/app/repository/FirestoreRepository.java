package com.clinicease.app.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.clinicease.app.model.Appointment;
import com.clinicease.app.model.Patient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * FirestoreRepository: central Firestore access for appointments/patients.
 *
 * NOTE: This implementation uses a read-then-transaction pattern for shifting appointments.
 * It validates `updatedAt` on each document inside the transaction to detect concurrent changes.
 * This DOES NOT detect new documents inserted after the initial query; consider server-side or
 * day-lock patterns if your app needs stronger guarantees.
 */
public class FirestoreRepository {
    private static final String TAG = "FirestoreRepo";
    private static FirestoreRepository instance;
    private final FirebaseFirestore db;

    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreRepository getInstance() {
        if (instance == null) instance = new FirestoreRepository();
        return instance;
    }

    // Create appointment
    public Task<DocumentReference> createAppointment(Map<String, Object> data) {
        return db.collection("appointments").add(data);
    }

    public Task<Void> updateAppointment(String appointmentId, Map<String, Object> updates) {
        return db.collection("appointments").document(appointmentId).update(updates);
    }

    public Task<Void> setAppointment(String appointmentId, Map<String, Object> data) {
        return db.collection("appointments").document(appointmentId).set(data);
    }

    public Task<QuerySnapshot> getAppointmentsForDate(long dayStartTs, long dayEndTs) {
        // Query appointments whose startTs is between dayStartTs and dayEndTs
        return db.collection("appointments")
                .whereGreaterThanOrEqualTo("startTs", dayStartTs)
                .whereLessThanOrEqualTo("startTs", dayEndTs)
                .get();
    }

    public Task<DocumentSnapshot> getPatientById(String patientId) {
        return db.collection("patients").document(patientId).get();
    }

    public Task<DocumentReference> createOrUpdatePatient(Map<String, Object> data) {
        // If caller provides id, use set; else add
        if (data.containsKey("id")) {
            String id = (String) data.get("id");
            // `set` returns Task<Void>, so return a Task<DocumentReference> by chaining
            return db.collection("patients").document(id).set(data)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return Tasks.forResult(db.collection("patients").document(id));
                    });
        } else {
            return db.collection("patients").add(data);
        }
    }

    // Batch updates placeholder (callers can create WriteBatch and commit themselves)
    public Task<Void> performBatchUpdates(List<com.google.firebase.firestore.WriteBatch> batches) {
        // For now, simply return successful completed task. Implement as needed.
        return Tasks.forResult(null);
    }

    /**
     * Extend an appointment by extraMinutes and shift subsequent appointments forward.
     * This method:
     *  - Reads the target appointment to get its startTs and the day range
     *  - Queries the day's appointments with startTs >= target.startTs
     *  - Re-reads each doc in a transaction and compares 'updatedAt' to detect concurrent changes
     *  - Extends the target appointment's endTs and shifts subsequent appointments by extraMs
     *
     * Important: this approach does NOT detect new appointments inserted between the initial query
     * and the transaction. If you need stronger guarantees, implement a server-side operation or a
     * day-lock pattern where all writers also update a per-day lock document.
     *
     * @param appointmentId id of appointment to extend (must exist)
     * @param extraMinutes minutes to extend (must be > 0)
     * @return Task<Void> completing when transaction commits
     */
    public Task<Void> extendAppointmentSafe(final String appointmentId, final int extraMinutes) {
        if (appointmentId == null || appointmentId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("appointmentId is required"));
        }
        if (extraMinutes <= 0) {
            return Tasks.forException(new IllegalArgumentException("extraMinutes must be > 0"));
        }

        final long extraMs = extraMinutes * 60L * 1000L;
        final DocumentReference apptRef = db.collection("appointments").document(appointmentId);

        // Step 1: read the target appointment to determine day range and startTs
        return apptRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            DocumentSnapshot apptSnap = task.getResult();
            if (apptSnap == null || !apptSnap.exists()) {
                throw new FirebaseFirestoreException("Appointment not found", FirebaseFirestoreException.Code.ABORTED);
            }

            Long startTs = apptSnap.getLong("startTs");
            if (startTs == null) {
                throw new FirebaseFirestoreException("Invalid appointment startTs", FirebaseFirestoreException.Code.ABORTED);
            }

            long dayStart = getDayStartMillis(startTs);
            long dayEnd = getDayEndMillis(startTs);

            // Query all appointments for that day with startTs >= target startTs (including target)
            Query q = db.collection("appointments")
                    .whereGreaterThanOrEqualTo("startTs", startTs)
                    .whereLessThanOrEqualTo("startTs", dayEnd)
                    .orderBy("startTs");

            return q.get();
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            QuerySnapshot qsnap = task.getResult();
            if (qsnap == null) {
                throw new FirebaseFirestoreException("Query returned null", FirebaseFirestoreException.Code.ABORTED);
            }

            // Build ordered lists of refs and previously-read updatedAt values and ids
            final List<com.google.firebase.firestore.DocumentReference> refs = new ArrayList<>();
            final List<Long> prevUpdatedAtList = new ArrayList<>();
            final List<String> ids = new ArrayList<>();

            for (DocumentSnapshot ds : qsnap.getDocuments()) {
                refs.add(ds.getReference());
                Long u = ds.getLong("updatedAt");
                prevUpdatedAtList.add(u == null ? 0L : u);
                ids.add(ds.getId());
            }

            // ensure target is in the returned set
            int targetIndex = ids.indexOf(appointmentId);
            if (targetIndex == -1) {
                throw new FirebaseFirestoreException("Target appointment not found in query result", FirebaseFirestoreException.Code.ABORTED);
            }

            if (refs.isEmpty()) {
                return Tasks.forResult(null);
            }

            // run transaction to re-read each doc and update
            return db.runTransaction((Transaction.Function<Void>) transaction -> {
                // Validate no one changed these docs (by comparing updatedAt)
                for (int i = 0; i < refs.size(); i++) {
                    com.google.firebase.firestore.DocumentReference r = refs.get(i);
                    DocumentSnapshot remote = transaction.get(r);
                    if (!remote.exists()) {
                        throw new FirebaseFirestoreException("Appointment disappeared during operation", FirebaseFirestoreException.Code.ABORTED);
                    }
                    Long remoteUpdatedAt = remote.getLong("updatedAt");
                    long prev = prevUpdatedAtList.get(i) == null ? 0L : prevUpdatedAtList.get(i);
                    long remoteVal = remoteUpdatedAt == null ? 0L : remoteUpdatedAt;
                    if (remoteVal != prev) {
                        throw new FirebaseFirestoreException("Appointment changed during operation", FirebaseFirestoreException.Code.ABORTED);
                    }
                }

                // Perform updates: extend target endTs and shift subsequent appointments
                for (int i = targetIndex; i < refs.size(); i++) {
                    com.google.firebase.firestore.DocumentReference r = refs.get(i);
                    DocumentSnapshot snap = transaction.get(r);
                    Long s = snap.getLong("startTs");
                    Long e = snap.getLong("endTs");
                    if (s == null || e == null) {
                        throw new FirebaseFirestoreException("Invalid timestamps in appointment", FirebaseFirestoreException.Code.ABORTED);
                    }

                    Map<String, Object> upd = new HashMap<>();
                    if (i == targetIndex) {
                        // extend only endTs of the target appointment
                        upd.put("endTs", e + extraMs);
                        upd.put("updatedAt", System.currentTimeMillis());
                        transaction.update(r, upd);
                    } else {
                        // shift whole appointment forward by extraMs
                        upd.put("startTs", s + extraMs);
                        upd.put("endTs", e + extraMs);
                        upd.put("updatedAt", System.currentTimeMillis());
                        transaction.update(r, upd);
                    }
                }

                return null;
            });
        });
    }

    // Helper: getDayStartMillis/getDayEndMillis — using Asia/Kolkata timezone
    private long getDayStartMillis(long ts) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getDayEndMillis(long ts) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
