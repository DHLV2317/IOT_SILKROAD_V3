package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityClientOnboardingBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ClientOnboardingActivity extends AppCompatActivity {

    private ActivityClientOnboardingBinding b;
    private final UserStore store = UserStore.get();
    private Uri photoUri;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // true = perfil, false = primera vez (onboarding)
    private boolean isProfileMode = false;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            photoUri = uri;
                            b.imgAvatar.setImageURI(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        b = ActivityClientOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        User u = store.getLogged();
        if (u==null){ finish(); return; }

        // Detectar si ya completó el perfil antes
        isProfileMode = u.isClientProfileCompleted();

        if (isProfileMode) {
            b.toolbar.setTitle("Mi perfil");
            b.btnConfirm.setText("Guardar datos");
            b.groupPasswordSection.setVisibility(View.VISIBLE);
        } else {
            b.toolbar.setTitle("Verifica tus datos");
            b.btnConfirm.setText("Confirmar");
            b.groupPasswordSection.setVisibility(View.GONE);
        }

        // precargar datos
        b.inputEmail.setText(u.getEmail());
        b.inputName.setText(nz(u.getName()));
        b.inputLastName.setText(nz(u.getLastName()));
        b.inputPhone.setText(cleanPhoneForInput(u.getPhone())); // sin +51
        b.inputAddress.setText(nz(u.getAddress()));
        b.inputDni.setText(nz(u.getDni()));

        if (!TextUtils.isEmpty(u.getPhotoUri())) {
            photoUri = Uri.parse(u.getPhotoUri());
            b.imgAvatar.setImageURI(photoUri);
        }

        b.btnPickPhoto.setOnClickListener(v -> pickImage.launch("image/*"));

        b.btnConfirm.setOnClickListener(v -> saveProfile(u));

        b.btnChangePassword.setOnClickListener(v -> changePassword(u));
    }

    private void saveProfile(User u) {
        String name  = b.inputName.getText().toString().trim();
        String last  = b.inputLastName.getText().toString().trim();
        String phone = b.inputPhone.getText().toString().trim();
        String dni   = b.inputDni.getText().toString().trim();
        String addr  = b.inputAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)){ b.inputName.setError("Requerido"); return; }
        if (TextUtils.isEmpty(last)){ b.inputLastName.setError("Requerido"); return; }
        if (TextUtils.isEmpty(phone)){ b.inputPhone.setError("Requerido"); return; }
        if (!phone.matches("\\d{6,12}")) {
            b.inputPhone.setError("Solo números (6-12 dígitos)");
            return;
        }
        if (!TextUtils.isEmpty(dni) && !dni.matches("\\d{8}")) {
            b.inputDni.setError("DNI debe tener 8 dígitos");
            return;
        }
        if (TextUtils.isEmpty(addr)){ b.inputAddress.setError("Requerido"); return; }

        // Normalizar teléfono con +51
        String phoneStored = phone;
        if (!phoneStored.startsWith("+")) {
            phoneStored = "+51" + phoneStored;
        }

        // Actualizar objeto User en memoria
        u.setName(name);
        u.setLastName(last);
        u.setPhone(phoneStored);
        u.setAddress(addr);
        u.setDni(dni);
        if (photoUri!=null) u.setPhotoUri(photoUri.toString());
        u.setClientProfileCompleted(true);

        store.updateLogged(u);

        // Actualizar en Firestore (colección "usuarios")
        db.collection("usuarios")
                .whereEqualTo("email", u.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        snap.getDocuments().get(0).getReference()
                                .update(
                                        "nombre", name,
                                        "apellido", last,
                                        "telefono", u.getPhone(),       // 👈 usamos u.getPhone()
                                        "direccion", addr,
                                        "dni", dni,
                                        "photoUri", u.getPhotoUri(),
                                        "clientProfileCompleted", true
                                );
                    }

                    if (isProfileMode) {
                        Snackbar.make(b.getRoot(),"Datos guardados", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(b.getRoot(),"Perfil confirmado", Snackbar.LENGTH_SHORT).show();
                        Intent i = new Intent(this, ClientHomeActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                })
                .addOnFailureListener(e -> Snackbar.make(
                        b.getRoot(),
                        "No se pudieron guardar en servidor: " + e.getMessage(),
                        Snackbar.LENGTH_LONG
                ).show());
    }

    private void changePassword(User u) {
        String current = b.inputCurrentPass.getText().toString();
        String newPass = b.inputNewPass.getText().toString();
        String confirm = b.inputConfirmPass.getText().toString();

        if (TextUtils.isEmpty(current)) {
            b.inputCurrentPass.setError("Requerido"); return;
        }
        if (TextUtils.isEmpty(newPass)) {
            b.inputNewPass.setError("Requerido"); return;
        }
        if (newPass.length() < 6) {
            b.inputNewPass.setError("Mínimo 6 caracteres"); return;
        }
        if (!newPass.equals(confirm)) {
            b.inputConfirmPass.setError("No coincide con la nueva contraseña"); return;
        }

        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            Snackbar.make(b.getRoot(),
                    "No hay usuario autenticado",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(u.getEmail())) {
            Snackbar.make(b.getRoot(),
                    "No se puede cambiar contraseña (email vacío)",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        auth.getCurrentUser()
                .reauthenticate(EmailAuthProvider.getCredential(u.getEmail(), current))
                .addOnSuccessListener(unused -> {
                    fUser.updatePassword(newPass)
                            .addOnSuccessListener(aVoid -> {
                                Snackbar.make(b.getRoot(),
                                        "Contraseña actualizada correctamente",
                                        Snackbar.LENGTH_LONG).show();
                                b.inputCurrentPass.setText("");
                                b.inputNewPass.setText("");
                                b.inputConfirmPass.setText("");
                            })
                            .addOnFailureListener(e -> Snackbar.make(
                                    b.getRoot(),
                                    "Error al actualizar contraseña: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG
                            ).show());
                })
                .addOnFailureListener(e -> Snackbar.make(
                        b.getRoot(),
                        "Contraseña actual incorrecta",
                        Snackbar.LENGTH_LONG
                ).show());
    }

    private static String nz(String s){ return s == null ? "" : s.trim(); }

    /** Quita posible prefijo +51 para mostrar solo los dígitos en el input */
    private static String cleanPhoneForInput(String phoneStored) {
        if (phoneStored == null) return "";
        String p = phoneStored.trim();
        if (p.startsWith("+51")) {
            p = p.substring(3);
        }
        return p;
    }
}