package com.clinicease.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal ConflictManager used for showing sync conflicts in the UI.
 * Replace with your real implementation that persists conflict state.
 */
public class ConflictManager {

    private static ConflictManager INSTANCE;
    private final List<String> conflicts = new ArrayList<>();

    private ConflictManager(Context ctx) {
        // sample entries for demo (remove in production)
        // conflicts.add("Appointment 123 overlapped with 456");
    }

    public static synchronized ConflictManager getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new ConflictManager(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    public List<String> getConflicts() {
        return new ArrayList<>(conflicts);
    }

    public void addConflict(String text) {
        conflicts.add(text);
    }

    public void clearConflicts() {
        conflicts.clear();
    }
}
