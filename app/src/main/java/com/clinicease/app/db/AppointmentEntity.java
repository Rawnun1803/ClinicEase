// /app/src/main/java/com/clinicease/app/db/AppointmentEntity.java
package com.clinicease.app.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "appointments")
public class AppointmentEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    public String patientId;
    public String patientName;
    public long startTs;
    public long endTs;
    public String status;
    public String reason;
    public long updatedAt;
    public boolean isSynced;
}
