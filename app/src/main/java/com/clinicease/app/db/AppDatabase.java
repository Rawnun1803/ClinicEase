// /app/src/main/java/com/clinicease/app/db/AppDatabase.java
package com.clinicease.app.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.clinicease.app.db.dao.AppointmentDao;
import com.clinicease.app.db.dao.PatientDao;
import com.clinicease.app.model.Appointment;
import com.clinicease.app.model.Patient;

@Database(entities = {AppointmentEntity.class, PatientEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract AppointmentDao appointmentDao();
    public abstract PatientDao patientDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "clinicease-db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
