// /app/src/main/java/com/clinicease/app/db/PatientEntity.java
package com.clinicease.app.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "patients")
public class PatientEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String name;
    public String phone;
    public long lastVisit;
}
