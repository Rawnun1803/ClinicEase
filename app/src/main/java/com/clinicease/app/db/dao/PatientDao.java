// /app/src/main/java/com/clinicease/app/db/dao/PatientDao.java
package com.clinicease.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.clinicease.app.db.PatientEntity;

import java.util.List;

@Dao
public interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name")
    List<PatientEntity> getAll();

    @Query("SELECT * FROM patients WHERE name LIKE :prefix || '%' LIMIT 10")
    List<PatientEntity> searchByPrefix(String prefix);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PatientEntity patient);
}
