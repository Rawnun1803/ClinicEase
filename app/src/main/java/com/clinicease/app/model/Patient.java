// /app/src/main/java/com/clinicease/app/model/Patient.java
package com.clinicease.app.model;

public class Patient {
    public String id;
    public String name;
    public String phone;
    public long lastVisit; // epoch millis

    public Patient() {}

    public Patient(String id, String name, String phone, long lastVisit) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.lastVisit = lastVisit;
    }
}
