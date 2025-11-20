package com.example.silkroad_iot.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.databinding.ActivityForgotPasswordVerifyBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordVerifyActivity extends AppCompatActivity {

    private ActivityForgotPasswordVerifyBinding b;

    private String email;
    private String expectedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityForgotPasswordVerifyBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Verificar código");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Recibir datos
        email = getIntent().getStringExtra("EMAIL");
        expectedCode = getIntent().getStringExtra("CODE");

        b.tvInfo.setText("Se envió un código de 6 dígitos a " + email);

        b.btnContinue.setOnClickListener(v -> {
            String code = b.inputCode.getText().toString().trim();

            if (TextUtils.isEmpty(code)) {
                b.inputCode.setError("Requerido");
                return;
            }

            if (code.length() != 6) {
                b.inputCode.setError("Debe tener 6 dígitos");
                return;
            }

            if (!code.equals(expectedCode)) {
                b.inputCode.setError("Código incorrecto");
                return;
            }

            // Código correcto → enviamos enlace real de Firebase
            FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                "Te enviamos un enlace para restablecer tu contraseña",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        });
    }
}