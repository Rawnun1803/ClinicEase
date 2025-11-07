// /app/src/main/java/com/clinicease/app/utils/SlotFinder.java
package com.clinicease.app.utils;

import java.util.*;

public class SlotFinder {

    /**
     * Find next N available slots (startMillis) between workStart/workEnd on same day/date, given existing appointments.
     *
     * existingAppointments: list of long[2] pairs {startMillis, endMillis}, must be sorted ascending by start.
     */
    public static List<Long> findAvailableSlots(long searchFromMillis,
                                                long dayStartMillis,
                                                long dayEndMillis,
                                                int slotMinutes,
                                                List<long[]> existingAppointments,
                                                int count) {
        List<Long> results = new ArrayList<>();
        long slotMs = slotMinutes * 60L * 1000L;

        long cursor = Math.max(searchFromMillis, dayStartMillis);

        // align cursor to next minute boundary
        cursor = (cursor / 60000L) * 60000L;

        // iterate slots until end of day or until found count
        while (cursor + slotMs <= dayEndMillis && results.size() < count) {
            long slotStart = cursor;
            long slotEnd = cursor + slotMs;
            boolean overlaps = false;
            for (long[] a : existingAppointments) {
                long s = a[0];
                long e = a[1];
                // overlap if not (slotEnd <= s or slotStart >= e)
                if (!(slotEnd <= s || slotStart >= e)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps && slotStart >= searchFromMillis) {
                results.add(slotStart);
            }
            cursor += slotMs;
        }
        return results;
    }
}
