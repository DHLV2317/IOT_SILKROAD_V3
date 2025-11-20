package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.databinding.ActivitySuperadminDetallesAdministradorBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DetallesAdministradorActivity extends AppCompatActivity {

    private ActivitySuperadminDetallesAdministradorBinding binding;
    private String docId; // correo actual
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetallesAdministradorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        Intent i = getIntent();
        // Espera un User por intent con campos name/email/phone/address/companyId/password
        User admin = (User) i.getSerializableExtra("admin");
        if (admin == null) { Toast.makeText(this, "Admin no encontrado", Toast.LENGTH_SHORT).show(); finish(); return; }

        docId = admin.getEmail();

        binding.textInputLayout.getEditText().setText(admin.getName());
        binding.textInputLayout2.getEditText().setText(admin.getCompanyId()); // empresa
        binding.textInputLayout3.getEditText().setText(admin.getEmail());
        binding.textInputLayout4.getEditText().setText(admin.getPhone());
        binding.textInputLayout5.getEditText().setText(admin.getAddress());
        binding.textInputLayout6.getEditText().setText(admin.getPassword());
        binding.textInputLayout7.getEditText().setText(admin.getPassword());

        /*if(admin.getActive()) {
            binding.en.setBackgroundColor("@color/green");
            binding.di.setBackgroundColor("@color/base");
        }else {
            binding.en.setBackgroundColor("@color/base");
            binding.di.setBackgroundColor("@color/red");
        }*/

        if(admin.isActive()){
            binding.en.setBackgroundColor(getResources().getColor(R.color.green, null));
            binding.di.setBackgroundColor(getResources().getColor(R.color.base, null));
        }else{
            binding.en.setBackgroundColor(getResources().getColor(R.color.base, null));
            binding.di.setBackgroundColor(getResources().getColor(R.color.red, null));
        }

        // Botones estado opcional (campo "active")
        binding.en.setOnClickListener(v -> setActive(true));
        binding.di.setOnClickListener(v -> setActive(false));
        binding.btnGuardar.setOnClickListener(this::guardar);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setActive(boolean active){
        //db.collection("users").document(docId)
        db.collection("usuarios").document(docId)
                .update("active", active)
                .addOnSuccessListener(v -> Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        if(active){
            binding.en.setBackgroundColor(getResources().getColor(R.color.green, null));
            binding.di.setBackgroundColor(getResources().getColor(R.color.base, null));
        }else{
            binding.en.setBackgroundColor(getResources().getColor(R.color.base, null));
            binding.di.setBackgroundColor(getResources().getColor(R.color.red, null));
        }
    }

    private void guardar(View view){
        String nombre   = binding.textInputLayout.getEditText().getText().toString().trim();
        String empresa  = binding.textInputLayout2.getEditText().getText().toString().trim();
        String correo   = binding.textInputLayout3.getEditText().getText().toString().trim();
        String telefono = binding.textInputLayout4.getEditText().getText().toString().trim();
        String address  = binding.textInputLayout5.getEditText().getText().toString().trim();
        String pass     = binding.textInputLayout6.getEditText().getText().toString().trim();
        String passRep  = binding.textInputLayout7.getEditText().getText().toString().trim();

        if (nombre.isEmpty() || empresa.isEmpty() || correo.isEmpty() || telefono.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(passRep)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> up = new HashMap<>();
        up.put("name", nombre);
        up.put("companyId", empresa);
        up.put("email", correo);
        up.put("phone", telefono);
        up.put("address", address);
        up.put("password", pass);
        up.put("role", User.Role.ADMIN.name());

        boolean emailChange = !correo.equals(docId);
        if (emailChange) {
            //db.collection("users").document(docId).delete()
            db.collection("usuarios").document(docId).delete()
                    //.addOnSuccessListener(v1 -> db.collection("users").document(correo).set(up)
                    .addOnSuccessListener(v1 -> db.collection("usuarios").document(correo).set(up)
                            .addOnSuccessListener(v2 -> { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show(); finish(); })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            //db.collection("users").document(docId).update(up)
            db.collection("usuarios").document(docId).update(up)
                    .addOnSuccessListener(v -> { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show(); finish(); })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
        startActivity(new Intent(this, AdministradoresActivity.class));
        finish();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}