// /app/src/main/java/com/clinicease/app/DateTimeUtils.java
package com.clinicease.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateTimeUtils {

    // parse date YYYY-MM-DD and time HH:mm to epoch millis (local timezone)
    public static long parseDateTimeToMillis(String dateYmd, String timeHm) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date d = sdf.parse(dateYmd + " " + timeHm);
            if (d != null) return d.getTime();
        } catch (ParseException ignored) {}
        return System.currentTimeMillis();
    }

    public static long getDayStartMillis(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long getDayEndMillis(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    public static String getTimeString(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(ts));
    }

    public static String getFriendlyDate(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(ts));
    }
}
