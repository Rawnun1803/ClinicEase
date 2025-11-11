// /app/src/main/java/com/clinicease/app/LoginActivity.java
package com.clinicease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

/**
 * LoginActivity: handles login/signup for the selected role (passed via Intent).
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inputEmail, inputPassword;
    private Button btnLogin, btnBack;
    private TextView tvSelectedRole;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        btnBack = findViewById(R.id.btn_back);
        tvSelectedRole = findViewById(R.id.tv_selected_role);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // read role from intent
        selectedRole = getIntent().getStringExtra(RoleSelectionActivity.EXTRA_ROLE);
        if (selectedRole == null) selectedRole = "patient";
        tvSelectedRole.setText(String.format("Logging in as: %s", selectedRole));

        btnBack.setOnClickListener(v -> finish()); // go back to role selection

        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText() == null ? "" : inputEmail.getText().toString().trim();
            String pass = inputPassword.getText() == null ? "" : inputPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            signInOrSignUp(email, pass);
        });
    }

    private void signInOrSignUp(String email, String password) {
        AlertDialog pd = new AlertDialog.Builder(this).setMessage("Signing in...").setCancelable(false).create();
        pd.show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            verifyRoleThenProceed(user.getUid(), pd);
                        } else {
                            pd.dismiss();
                            Toast.makeText(this, "Auth succeeded but user is null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthInvalidUserException || (ex != null && ex.getMessage() != null && ex.getMessage().contains("There is no user"))) {
                            // create account then write role
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    FirebaseUser newUser = auth.getCurrentUser();
                                    if (newUser != null) {
                                        Map<String, Object> udoc = new HashMap<>();
                                        udoc.put("email", email);
                                        udoc.put("role", selectedRole);
                                        udoc.put("createdAt", System.currentTimeMillis());
                                        db.collection("users").document(newUser.getUid()).set(udoc)
                                                .addOnCompleteListener(setTask -> {
                                                    pd.dismiss();
                                                    navigateAfterLogin(selectedRole);
                                                });
                                    } else {
                                        pd.dismiss();
                                        Toast.makeText(this, "Account created but user is null", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    pd.dismiss();
                                    Toast.makeText(this, "Sign-up failed: " + (createTask.getException() == null ? "unknown" : createTask.getException().getMessage()), Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            pd.dismiss();
                            Toast.makeText(this, "Sign-in failed: " + (ex == null ? "unknown" : ex.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void verifyRoleThenProceed(String uid, AlertDialog pd) {
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                DocumentSnapshot s = task.getResult();
                if (s != null && s.exists()) {
                    String role = s.contains("role") ? s.getString("role") : null;
                    if (role == null) role = selectedRole;
                    if (!role.equals(selectedRole)) {
                        String finalRole = role;
                        new AlertDialog.Builder(this)
                                .setTitle("Role mismatch")
                                .setMessage("Your account role is '" + role + "' but you selected '" + selectedRole + "'. Proceed as '" + role + "' or cancel?")
                                .setPositiveButton("Proceed", (d, w) -> navigateAfterLogin(finalRole))
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        navigateAfterLogin(role);
                    }
                } else {
                    // user doc not set; ensure it's written
                    Map<String, Object> udoc = new HashMap<>();
                    udoc.put("role", selectedRole);
                    udoc.put("updatedAt", System.currentTimeMillis());
                    db.collection("users").document(uid).set(udoc).addOnCompleteListener(setTask -> {
                        navigateAfterLogin(selectedRole);
                    });
                }
            } else {
                Toast.makeText(this, "Failed to read user role, proceeding to app", Toast.LENGTH_SHORT).show();
                navigateAfterLogin(selectedRole);
            }
        });
    }

    private void navigateAfterLogin(String role) {
        Intent i;
        if ("doctor".equals(role)) {
            i = new Intent(this, DoctorHomeActivity.class);
        } else if ("receptionist".equals(role)) {
            i = new Intent(this, ReceptionistQuickBookActivity.class);
        } else {
            i = new Intent(this, PatientHomeActivity.class);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
