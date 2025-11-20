package com.example.silkroad_iot.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.databinding.ActivityForgotPasswordBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding b;

    // EmailJS CONFIG (los mismos que en Register, pero con el template de reset)
    private static final String EMAILJS_SERVICE_ID = "service_g8jcbib";
    private static final String EMAILJS_TEMPLATE_ID = "template_8mewgaj";   // <-- ESTE es el correcto
    private static final String EMAILJS_PUBLIC_KEY  = "Z0GwI2IxoW77f78QX";
    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Olvidé mi contraseña");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // --- Botón enviar código ---
        b.btnSendCode.setOnClickListener(v -> {
            String email = b.inputEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                b.inputEmail.setError("Requerido");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                b.inputEmail.setError("Correo no válido");
                return;
            }

            // Generamos código de 6 dígitos
            String code = String.format(Locale.US, "%06d", new Random().nextInt(1_000_000));

            setLoading(true);

            sendResetCode(email, code, new Callback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Código enviado a " + email,
                            Toast.LENGTH_LONG).show();

                    Intent i = new Intent(ForgotPasswordActivity.this,
                            ForgotPasswordVerifyActivity.class);
                    i.putExtra("EMAIL", email);
                    i.putExtra("CODE", code);
                    startActivity(i);
                }

                @Override
                public void onError(String error) {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Error: " + error,
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // --------------------------------------------------------------------
    // Callback para la petición HTTP
    // --------------------------------------------------------------------
    private interface Callback {
        void onSuccess();
        void onError(String error);
    }

    private void sendResetCode(String email, String code, Callback cb) {

        new Thread(() -> {
            try {
                // Estos nombres deben coincidir con lo que pusiste en el template:
                // {{to_email}} y {{verification_code}}
                JSONObject params = new JSONObject();
                params.put("to_email", email);
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
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    boolean ok = response.isSuccessful();

                    runOnUiThread(() -> {
                        if (ok) cb.onSuccess();
                        else cb.onError("HTTP " + response.code());
                    });
                }

            } catch (IOException e) {
                runOnUiThread(() -> cb.onError(e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        b.btnSendCode.setEnabled(!loading);
        b.inputEmail.setEnabled(!loading);
        // si quieres, puedes añadir un ProgressBar en el layout y mostrarlo aquí
    }
}