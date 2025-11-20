package com.example.silkroad_iot.ui.admin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.UserStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminChangePasswordActivity extends AppCompatActivity {

    private static final String PREFS = "app_prefs";
    private static final String KEY_PROFILE_DONE = "admin_profile_completed";

    private TextInputEditText inputCurrentPassword;
    private TextInputEditText inputNewPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton btnSavePassword;
    private MaterialButton btnCancel;

    private FirebaseAuth auth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_change_password);

        auth  = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        setupToolbar();
        bindViews();
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Cambiar contraseña");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void bindViews() {
        inputCurrentPassword = findViewById(R.id.inputCurrentPassword);
        inputNewPassword     = findViewById(R.id.inputNewPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnSavePassword      = findViewById(R.id.btnSavePassword);
        btnCancel            = findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        btnSavePassword.setOnClickListener(v -> changePassword());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void changePassword() {
        String current = safeText(inputCurrentPassword);
        String newer   = safeText(inputNewPassword);
        String confirm = safeText(inputConfirmPassword);

        if (current.isEmpty() || newer.isEmpty() || confirm.isEmpty()) {
            showSnack("Completa todos los campos");
            return;
        }

        if (!newer.equals(confirm)) {
            showSnack("La nueva contraseña y la confirmación no coinciden");
            return;
        }

        if (newer.length() < 6) {
            showSnack("La nueva contraseña debe tener al menos 6 caracteres");
            return;
        }

        if (newer.equals(current)) {
            showSnack("La nueva contraseña debe ser diferente a la actual");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showSnack("Sesión expirada. Vuelve a iniciar sesión.");
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            // Intentamos tomarla desde UserStore como respaldo
            var u = UserStore.get().getLogged();
            if (u != null && u.getEmail() != null && !u.getEmail().isEmpty()) {
                email = u.getEmail();
            }
        }

        if (email == null || email.isEmpty()) {
            showSnack("No se pudo obtener el email del usuario.");
            return;
        }

        btnSavePassword.setEnabled(false);

        AuthCredential credential = EmailAuthProvider.getCredential(email, current);

        // 1) Reautenticar
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    // 2) Actualizar contraseña
                    user.updatePassword(newer)
                            .addOnSuccessListener(unused2 -> {
                                // Marcamos perfil como completado (para el flujo de primera vez)
                                prefs.edit()
                                        .putBoolean(KEY_PROFILE_DONE, true)
                                        .apply();

                                showSnack("Contraseña actualizada correctamente");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSavePassword.setEnabled(true);
                                showSnack("Error al actualizar contraseña: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    btnSavePassword.setEnabled(true);
                    showSnack("La contraseña actual no es correcta");
                });
    }

    private String safeText(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    private void showSnack(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 👇 Esta es la forma correcta en Java con AppCompatActivity
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}