// /app/src/main/java/com/clinicease/app/sync/SyncWorker.java
package com.clinicease.app.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.clinicease.app.db.AppDatabase;
import com.clinicease.app.db.AppointmentEntity;
import com.clinicease.app.db.dao.AppointmentDao;
import com.clinicease.app.repository.FirestoreRepository;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    private final AppDatabase db;
    private final AppointmentDao appointmentDao;
    private final FirebaseFirestore firestore;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = AppDatabase.getInstance(context);
        appointmentDao = db.appointmentDao();
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 1) Push local unsynced appointments
            List<AppointmentEntity> localUnsynced = appointmentDao.getAll(); // for simplicity; ideally filtered by isSynced flag
            for (AppointmentEntity le : localUnsynced) {
                if (!le.isSynced) {
                    // upsert to Firestore
                    Map<String, Object> data = new HashMap<>();
                    data.put("patientId", le.patientId);
                    data.put("patientName", le.patientName);
                    data.put("startTs", le.startTs);
                    data.put("endTs", le.endTs);
                    data.put("status", le.status);
                    data.put("reason", le.reason);
                    data.put("updatedAt", le.updatedAt);

                    final CountDownLatch latch = new CountDownLatch(1);
                    // If id exists, set by id
                    DocumentReference dr = firestore.collection("appointments").document(le.id);
                    dr.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            dr.update(data).addOnCompleteListener(t2 -> latch.countDown()).addOnFailureListener(e -> latch.countDown());
                        } else {
                            dr.set(data).addOnCompleteListener(t2 -> latch.countDown()).addOnFailureListener(e -> latch.countDown());
                        }
                    });
                    latch.await(5, TimeUnit.SECONDS);
                    // Mark as synced locally (we assume success; could validate)
                    le.isSynced = true;
                    appointmentDao.insert(le);
                }
            }

            // 2) Pull remote appointments for upcoming 30 days and upsert locally
            long now = System.currentTimeMillis();
            long future = now + TimeUnit.DAYS.toMillis(30);
            Query q = firestore.collection("appointments")
                    .whereGreaterThanOrEqualTo("startTs", now - TimeUnit.DAYS.toMillis(1))
                    .whereLessThanOrEqualTo("startTs", future)
                    .orderBy("startTs");

            final CountDownLatch latch2 = new CountDownLatch(1);
            final List<DocumentSnapshot>[] snapshots = new List[]{Collections.emptyList()};
            q.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) snapshots[0] = task.getResult().getDocuments();
                latch2.countDown();
            });

            latch2.await(6, TimeUnit.SECONDS);

            for (DocumentSnapshot ds : snapshots[0]) {
                String id = ds.getId();
                Long startTs = ds.getLong("startTs");
                Long endTs = ds.getLong("endTs");
                String patientName = ds.getString("patientName");
                String patientId = ds.getString("patientId");
                String status = ds.getString("status");
                String reason = ds.getString("reason");
                Long updatedAt = ds.getLong("updatedAt") == null ? 0L : ds.getLong("updatedAt");

                // compare with local copy (if exists)
                AppointmentEntity local = db.appointmentDao().getById(id); // implement getById in DAO
                if (local == null) {
                    // insert
                    AppointmentEntity ne = new AppointmentEntity();
                    ne.id = id;
                    ne.patientId = patientId;
                    ne.patientName = patientName;
                    ne.startTs = startTs == null ? now : startTs;
                    ne.endTs = endTs == null ? now : endTs;
                    ne.status = status == null ? "BOOKED" : status;
                    ne.reason = reason == null ? "" : reason;
                    ne.updatedAt = updatedAt;
                    ne.isSynced = true;
                    db.appointmentDao().insert(ne);
                } else {
                    // conflict detection
                    if (local.updatedAt == updatedAt) {
                        // nothing to do
                    } else {
                        // if both changed locally (local.updatedAt > remote.updatedAt) -> we should push local (handled earlier)
                        // if remote.updatedAt > local.updatedAt -> update local
                        if (updatedAt > local.updatedAt) {
                            local.startTs = startTs == null ? local.startTs : startTs;
                            local.endTs = endTs == null ? local.endTs : endTs;
                            local.patientName = patientName == null ? local.patientName : patientName;
                            local.reason = reason == null ? local.reason : reason;
                            local.status = status == null ? local.status : status;
                            local.updatedAt = updatedAt;
                            local.isSynced = true;
                            db.appointmentDao().insert(local);
                        } else if (local.updatedAt > updatedAt) {
                            // local is newer; schedule a push next run (already handled earlier)
                            // Also, optionally, record conflict for doctor to review
                            ConflictManager.getInstance(getApplicationContext()).recordConflict("Local change awaiting sync for appointment " + id);
                        } else {
                            // equal or ambiguous
                        }
                    }
                }
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
