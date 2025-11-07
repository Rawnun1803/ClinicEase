// /app/src/main/java/com/clinicease/app/sync/ConflictManager.java
package com.clinicease.app.sync;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConflictManager {
    private static final String PREF = "clinicease_conflicts";
    private static final String KEY_LIST = "conflicts";
    private static ConflictManager instance;
    private final SharedPreferences prefs;

    private ConflictManager(@NonNull Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized ConflictManager getInstance(Context ctx) {
        if (instance == null) instance = new ConflictManager(ctx.getApplicationContext());
        return instance;
    }

    public void recordConflict(String msg) {
        String existing = prefs.getString(KEY_LIST, "");
        String combined = existing.isEmpty() ? msg : existing + "||" + msg;
        prefs.edit().putString(KEY_LIST, combined).apply();
    }

    public List<String> getConflicts() {
        String raw = prefs.getString(KEY_LIST, "");
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\\|\\|")));
    }

    public void clearConflicts() {
        prefs.edit().remove(KEY_LIST).apply();
    }
}
