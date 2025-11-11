// /app/src/main/java/com/clinicease/app/RoleSelectionActivity.java
package com.clinicease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * RoleSelectionActivity: only responsible for selecting a role and launching LoginActivity.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private Button btnDoctor, btnReceptionist, btnPatient;

    public static final String EXTRA_ROLE = "extra_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        btnDoctor = findViewById(R.id.btn_doctor);
        btnReceptionist = findViewById(R.id.btn_receptionist);
        btnPatient = findViewById(R.id.btn_patient);

        btnDoctor.setOnClickListener(v -> openLoginForRole("doctor"));
        btnReceptionist.setOnClickListener(v -> openLoginForRole("receptionist"));
        btnPatient.setOnClickListener(v -> openLoginForRole("patient"));
    }

    private void openLoginForRole(String role) {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(EXTRA_ROLE, role);
        startActivity(i);
    }
}
