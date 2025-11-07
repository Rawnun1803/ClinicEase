// /app/src/main/java/com/clinicease/app/model/Appointment.java
package com.clinicease.app.model;

public class Appointment {
    public String id;
    public String patientId;
    public String patientName;
    public long startTs; // epoch millis UTC
    public long endTs;   // epoch millis UTC
    public String status; // BOOKED | COMPLETED | CANCELED
    public String reason;
    public String createdBy;
    public long updatedAt;

    public Appointment() {}

    public Appointment(String id, String patientId, String patientName, long startTs, long endTs, String status, String reason, String createdBy, long updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.startTs = startTs;
        this.endTs = endTs;
        this.status = status;
        this.reason = reason;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
    }
}
