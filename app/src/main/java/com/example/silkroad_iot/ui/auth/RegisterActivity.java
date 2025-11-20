package com.example.silkroad_iot.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.databinding.ActivityRegisterBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding b;

    // ⚙️ CONFIG EMAILJS – TUS DATOS
    private static final String EMAILJS_SERVICE_ID  = "service_g8jcbib";      // service ID
    private static final String EMAILJS_TEMPLATE_ID = "template_k9uzb48";     // template ID real
    private static final String EMAILJS_PUBLIC_KEY  = "Z0GwI2IxoW77f78QX";    // public key
    private static final String EMAILJS_URL         = "https://api.emailjs.com/api/v1.0/email/send";
    // Origin que debe estar permitido en EmailJS /Account/Security
    private static final String EMAILJS_ORIGIN      = "http://localhost";

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    // Regla: mínimo 8 caracteres, al menos 1 número y 1 carácter especial
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-={}|\\[\\]:;\"'<>,.?/]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Registro Cliente");

        // botón atrás del toolbar
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Continuar registro Cliente
        b.btnContinue.setOnClickListener(v -> {
            String name = Objects.requireNonNull(b.inputName.getText()).toString().trim();
            String mail = Objects.requireNonNull(b.inputEmail.getText()).toString().trim();
            String pas1 = Objects.requireNonNull(b.inputPass1.getText()).toString().trim();
            String pas2 = Objects.requireNonNull(b.inputPass2.getText()).toString().trim();
            boolean term = b.ckTerms.isChecked();

            // Validaciones
            if (TextUtils.isEmpty(name)) {
                b.inputName.setError("Requerido");
                return;
            }

            if (TextUtils.isEmpty(mail)) {
                b.inputEmail.setError("Requerido");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                b.inputEmail.setError("Correo no válido");
                return;
            }

            if (TextUtils.isEmpty(pas1)) {
                b.inputPass1.setError("Requerido");
                return;
            }

            if (!PASSWORD_PATTERN.matcher(pas1).matches()) {
                b.inputPass1.setError("Mín. 8 caracteres, 1 número y 1 símbolo");
                return;
            }

            if (TextUtils.isEmpty(pas2)) {
                b.inputPass2.setError("Requerido");
                return;
            }

            if (!pas1.equals(pas2)) {
                b.inputPass2.setError("Las contraseñas no coinciden");
                return;
            }

            if (!term) {
                Toast.makeText(this,
                        "Debes aceptar los términos y la política de privacidad",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Generar código de 6 dígitos
            String code = String.format(Locale.US, "%06d", new Random().nextInt(1_000_000));

            // Enviar email con EmailJS
            setLoading(true);
            sendVerificationEmail(name, mail, code, new EmailCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this,
                            "Te enviamos un código a " + mail,
                            Toast.LENGTH_LONG).show();

                    // Ir a pantalla de verificación
                    Intent i = new Intent(RegisterActivity.this, RegisterVerifyActivity.class);
                    i.putExtra("NAME", name);
                    i.putExtra("EMAIL", mail);
                    i.putExtra("PASS", pas1);
                    i.putExtra("CODE", code);
                    startActivity(i);
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this,
                            "No se pudo enviar el código: " + message,
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        // Ir a Registro Guía
        b.btnRegisterGuide.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuideRegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --------------------------------------------------------------------
    // EMAILJS
    // --------------------------------------------------------------------
    private interface EmailCallback {
        void onSuccess();
        void onError(String message);
    }

    private void sendVerificationEmail(String name, String mail, String code, EmailCallback cb) {
        new Thread(() -> {
            try {
                // variables que usas en el template: {{to_name}}, {{to_email}}, {{verification_code}}
                JSONObject params = new JSONObject();
                params.put("to_name", name);
                params.put("to_email", mail);
                params.put("verification_code", code);

                JSONObject root = new JSONObject();
                root.put("service_id", EMAILJS_SERVICE_ID);
                root.put("template_id", EMAILJS_TEMPLATE_ID);
                root.put("user_id", EMAILJS_PUBLIC_KEY);
                root.put("template_params", params);

                RequestBody body = RequestBody.create(root.toString(), JSON);
                Request request = new Request.Builder()
                        .url(EMAILJS_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("origin", EMAILJS_ORIGIN)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    boolean ok = response.isSuccessful();
                    String msg = response.message();

                    runOnUiThread(() -> {
                        if (ok) {
                            cb.onSuccess();
                        } else {
                            cb.onError("HTTP " + response.code() + " " + msg);
                        }
                    });
                }
            } catch (IOException ioe) {
                runOnUiThread(() -> cb.onError(ioe.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    // --------------------------------------------------------------------
    // UI helper
    // --------------------------------------------------------------------
    private void setLoading(boolean loading) {
        b.progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        b.btnContinue.setEnabled(!loading);
        b.btnRegisterGuide.setEnabled(!loading);
        b.inputName.setEnabled(!loading);
        b.inputEmail.setEnabled(!loading);
        b.inputPass1.setEnabled(!loading);
        b.inputPass2.setEnabled(!loading);
        b.ckTerms.setEnabled(!loading);
    }
}