package com.clinicease.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private TextView resultText;
    private Button btnAddData;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        resultText = findViewById(R.id.resultText);
        btnAddData = findViewById(R.id.btnAddData);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Button click listener
        btnAddData.setOnClickListener(v -> addTestData());
    }

    private void addTestData() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("message", "Hello Firestore!");
        data.put("timestamp", System.currentTimeMillis());

        db.collection("test")
                .add(data)
                .addOnSuccessListener(ref -> {
                    String msg = "✅ Document added with ID: " + ref.getId();
                    resultText.setText(msg);
                    Log.d("FirestoreTest", msg);
                })
                .addOnFailureListener(e -> {
                    String msg = "❌ Error adding document: " + e.getMessage();
                    resultText.setText(msg);
                    Log.e("FirestoreTest", msg, e);
                });
    }
}
