// /app/src/main/java/com/clinicease/app/db/dao/AppointmentDao.java
package com.clinicease.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.clinicease.app.db.AppointmentEntity;

import java.util.List;

@Dao
public interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY startTs")
    List<AppointmentEntity> getAll();

    @Query("SELECT * FROM appointments WHERE date(startTs/1000, 'unixepoch') = :dateStr ORDER BY startTs")
    List<AppointmentEntity> getForDate(String dateStr); // dateStr = 'YYYY-MM-DD' string; we can simplify if needed

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppointmentEntity appointment);

    @Update
    void update(AppointmentEntity appointment);

    @Query("DELETE FROM appointments WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    AppointmentEntity getById(String id);

}
