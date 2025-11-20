package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.databinding.ActivitySuperadminDetallesGuiaBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DetallesGuiaActivity extends AppCompatActivity {

    private ActivitySuperadminDetallesGuiaBinding binding;
    private FirebaseFirestore db;
    private String docId;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetallesGuiaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        Intent i = getIntent();
        User guia = (User) i.getSerializableExtra("guia");
        if (guia == null) { Toast.makeText(this, "Guía no encontrado", Toast.LENGTH_SHORT).show(); finish(); return; }

        docId = guia.getEmail();

        binding.inputName.setText(guia.getName());
        binding.inputLastName.setText(guia.getLastName());
        binding.inputDocumentType.setText(guia.getDocumentType());
        binding.inputDocumentNumber.setText(guia.getDocumentNumber());
        binding.inputBirthDate.setText(guia.getBirthDate() == null ? "" : guia.getBirthDate());
        binding.inputEmail.setText(guia.getEmail());
        binding.inputPhone.setText(guia.getPhone());
        binding.inputAddress.setText(guia.getAddress());
        binding.inputLanguages.setText(guia.getLanguages());
        binding.inputPassword.setText(guia.getPassword());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        updateEstadoUI(true);
        binding.en.setOnClickListener(v -> setActive(true));
        binding.di.setOnClickListener(v -> setActive(false));
        binding.button.setOnClickListener(this::guardar);
    }

    private void updateEstadoUI(boolean active) {
        int verde = ContextCompat.getColor(this, R.color.brand_verde);
        int rojo  = ContextCompat.getColor(this, R.color.red);
        int base  = ContextCompat.getColor(this, R.color.base2);
        binding.en.setBackgroundColor(active ? verde : base);
        binding.di.setBackgroundColor(active ? base  : rojo);
    }

    private void setActive(boolean active){
        //db.collection("users").document(docId)
        db.collection("usuarios").document(docId)
                .update("active", active)
                .addOnSuccessListener(v -> { updateEstadoUI(active); Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void guardar(View view) {
        String nombres  = binding.inputName.getText().toString().trim();
        String apellidos= binding.inputLastName.getText().toString().trim();
        String docType  = binding.inputDocumentType.getText().toString().trim();
        String docNum   = binding.inputDocumentNumber.getText().toString().trim();
        String birth    = binding.inputBirthDate.getText().toString().trim();
        String email    = binding.inputEmail.getText().toString().trim();
        String phone    = binding.inputPhone.getText().toString().trim();
        String address  = binding.inputAddress.getText().toString().trim();
        String langs    = binding.inputLanguages.getText().toString().trim();
        String pass     = binding.inputPassword.getText().toString().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || docType.isEmpty() || docNum.isEmpty()
                || email.isEmpty() || phone.isEmpty() || address.isEmpty() || langs.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> up = new HashMap<>();
        up.put("name", nombres);
        up.put("lastName", apellidos);
        up.put("documentType", docType);
        up.put("documentNumber", docNum);
        up.put("birthDate", birth);
        up.put("email", email);
        up.put("phone", phone);
        up.put("address", address);
        up.put("languages", langs);
        up.put("password", pass);
        up.put("role", User.Role.GUIDE.name());
        // Mantener estado de aprobación si existe
        up.put("guideApprovalStatus", "APPROVED"); // si esta pantalla es solo para aprobados
        up.put("guideApproved", true);

        boolean emailChange = !email.equals(docId);
        if (emailChange) {
            //db.collection("users").document(docId).delete()
            db.collection("usuarios").document(docId).delete()
                    //.addOnSuccessListener(v1 -> db.collection("users").document(email).set(up)
                    .addOnSuccessListener(v1 -> db.collection("usuarios").document(email).set(up)
                            .addOnSuccessListener(v2 -> { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show(); finish(); })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            //db.collection("users").document(docId).update(up)
            db.collection("usuarios").document(docId).update(up)
                    .addOnSuccessListener(v -> { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show(); finish(); })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}