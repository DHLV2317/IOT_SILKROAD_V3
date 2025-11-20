package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.databinding.ActivitySuperadminCrearAdministradorBinding;
import com.google.common.hash.Hashing;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.widget.Toolbar;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CrearAdministradorActivity extends AppCompatActivity {

    private ActivitySuperadminCrearAdministradorBinding binding;
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminCrearAdministradorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        binding.btnCrear.setOnClickListener(v -> crearAdmin());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    private void crearAdmin() {
        String nombre     = binding.textInputLayout.getEditText().getText().toString().trim();
        String empresa    = binding.textInputLayout2.getEditText().getText().toString().trim();
        String correo     = binding.textInputLayout3.getEditText().getText().toString().trim();
        String telefono   = binding.textInputLayout4.getEditText().getText().toString().trim();
        String ubicacion  = binding.textInputLayout5.getEditText().getText().toString().trim();
        String direccion = "Dirección por definir";
        String emailEmpresa = "emailEmpresa por definir";
        String pass       = binding.textInputLayout6.getEditText().getText().toString().trim();
        String passRepeat = binding.textInputLayout7.getEditText().getText().toString().trim();

        if (nombre.isEmpty() || empresa.isEmpty() || correo.isEmpty() || telefono.isEmpty() ||
                ubicacion.isEmpty() || pass.isEmpty() || passRepeat.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(passRepeat)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        //String hashedPass = Hashing.sha256().hashString(pass, StandardCharsets.UTF_8).toString();


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(correo, pass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        Toast.makeText(this, "No se pudo obtener el usuario recién creado.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String nuevoAdminUID = firebaseUser.getUid();

                    Map<String, Object> dataUsuario = new HashMap<>();
                    dataUsuario.put("name", nombre);
                    dataUsuario.put("email", correo);
                    dataUsuario.put("password", pass); // hashear
                    dataUsuario.put("role", User.Role.ADMIN.name());
                    dataUsuario.put("phone", telefono);
                    dataUsuario.put("address", ubicacion);
                    dataUsuario.put("companyId", nuevoAdminUID); // Usuario vinculado con el ID de la empresa
                    dataUsuario.put("active", true);         // campo opcional de estado

                    // Datos para la colección 'empresas'
                    Map<String, Object> dataEmpresa = new HashMap<>();
                    dataEmpresa.put("id", nuevoAdminUID); // UID como ID del documento
                    dataEmpresa.put("nombre", empresa);

                    //dataEmpresa.put("correo_contacto", correo); // 'correo_contacto' para evitar confusión
                    dataEmpresa.put("correo", correo); //del administrador
                    dataEmpresa.put("direccion", "Dirección por definir");
                    dataEmpresa.put("email", "emailEmpresa por definir");
                    dataEmpresa.put("imagen", "porDefinir");
                    dataEmpresa.put("latitud", "porDefinir");
                    dataEmpresa.put("longitud", "porDefinir");
                    dataEmpresa.put("telefono", telefono);
                    //dataEmpresa.put("administrador_uid", nuevoAdminUID); // Guardamos el UID del admin a cargo

                    dataEmpresa.put("administrador_nombre", nombre);     // Y su nombre para fácil acceso

                    // Datos para el log
                    Map<String, Object> logData = new HashMap<>();
                    logData.put("tipo", "Creación");
                    logData.put("tipoUsuario", "Administrador");
                    logData.put("nombre", "De SuperAdministrador");
                    logData.put("usuario", nombre);
                    logData.put("descripcion", "Se ha creado el administrador de nombre " + nombre + " con el correo " + correo + " y se asignó a la empresa " + empresa);
                    logData.put("fecha", System.currentTimeMillis());

                    db.collection("empresas").document(nuevoAdminUID) // Usamos el UID como ID del documento
                            .set(dataEmpresa)
                            .addOnSuccessListener(aVoid -> {
                                db.collection("usuarios").document(nuevoAdminUID)//con correo...
                                        .set(dataUsuario)
                                        .addOnSuccessListener(aVoid2 -> {
                                            db.collection("logs").document()
                                                    .set(logData)
                                                    .addOnSuccessListener(aVoid3 -> {
                                                        Toast.makeText(CrearAdministradorActivity.this, "Administrador y empresa creados con éxito", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(CrearAdministradorActivity.this, AdministradoresActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, "Creado, pero falló el log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Falló la creación del usuario en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Falló la creación de la empresa en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            });

                })
                .addOnFailureListener(e -> {
                    // Falló la creación del usuario en Firebase Auth
                    Toast.makeText(CrearAdministradorActivity.this, "Error al crear administrador en Auth: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

    }
}