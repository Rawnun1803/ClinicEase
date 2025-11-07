// /app/src/main/java/com/clinicease/app/sync/SyncManager.java
package com.clinicease.app.sync;

import android.content.Context;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class SyncManager {

    private static final String WORK_TAG = "clinicease_sync";

    public static void schedulePeriodicSync(Context ctx) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    public static void triggerImmediateSync(Context ctx) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(ctx).enqueue(req);
    }
}
