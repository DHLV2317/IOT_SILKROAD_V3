package com.example.silkroad_iot.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.databinding.ActivityRegisterVerifyBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterVerifyActivity extends AppCompatActivity {

    private ActivityRegisterVerifyBinding b;

    private String name;
    private String email;
    private String password;
    private String expectedCode;   // código actual válido

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // ⚙️ EmailJS – mismos valores que en RegisterActivity
    private static final String EMAILJS_SERVICE_ID  = "service_g8jcbib";
    private static final String EMAILJS_TEMPLATE_ID = "template_k9uzb48";
    private static final String EMAILJS_PUBLIC_KEY  = "Z0GwI2IxoW77f78QX";
    private static final String EMAILJS_URL         = "https://api.emailjs.com/api/v1.0/email/send";
    private static final String EMAILJS_ORIGIN      = "http://localhost";

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterVerifyBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Toolbar
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Verificar registro");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Datos desde el intent
        name         = getIntent().getStringExtra("NAME");
        email        = getIntent().getStringExtra("EMAIL");
        password     = getIntent().getStringExtra("PASS");
        expectedCode = getIntent().getStringExtra("CODE");

        if (email == null) email = "";
        if (name == null) name = "";
        if (password == null) password = "";
        if (expectedCode == null) expectedCode = "";

        b.tvInfo.setText("Se envió un código de 6 dígitos a " + email);

        // Reenviar código por correo
        b.tvResend.setOnClickListener(v -> {
            expectedCode = String.format(Locale.US, "%06d", new Random().nextInt(1_000_000));
            setLoading(true);
            sendVerificationEmail(name, email, expectedCode, new EmailCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Nuevo código enviado a " + email,
                            Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Error reenviando código: " + message,
                            Snackbar.LENGTH_LONG).show();
                }
            });
        });

        // Confirmar código
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

            // Código correcto → crear usuario en Firebase
            createUserInFirebase();
        });
    }

    // --------------------------------------------------------------------
    // UI helper
    // --------------------------------------------------------------------
    private void setLoading(boolean loading) {
        b.btnContinue.setEnabled(!loading);
        b.tvResend.setEnabled(!loading);
        b.inputCode.setEnabled(!loading);
        b.progressBar.setVisibility(loading ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    // --------------------------------------------------------------------
    // EmailJS
    // --------------------------------------------------------------------
    private interface EmailCallback {
        void onSuccess();
        void onError(String message);
    }

    private void sendVerificationEmail(String name, String mail, String code, EmailCallback cb) {
        new Thread(() -> {
            try {
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
    // Creación de usuario en Firebase
    // --------------------------------------------------------------------
    private void createUserInFirebase() {
        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null
                            ? authResult.getUser().getUid()
                            : null;

                    if (uid == null) {
                        setLoading(false);
                        Snackbar.make(b.getRoot(),
                                "Error creando usuario (UID nulo)",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // Guardar datos adicionales en Firestore
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("email", email);
                    data.put("nombre", name);
                    data.put("rol", "CLIENT");
                    data.put("clientProfileCompleted", false);

                    db.collection("usuarios")
                            .document(uid)
                            .set(data)
                            .addOnSuccessListener(done -> {
                                setLoading(false);
                                Snackbar.make(b.getRoot(),
                                        "Registro verificado. Ahora puedes iniciar sesión.",
                                        Snackbar.LENGTH_LONG).show();

                                Intent backToLogin =
                                        new Intent(this, MainActivity.class);
                                backToLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(backToLogin);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Snackbar.make(b.getRoot(),
                                        "Error guardando datos: " + e.getMessage(),
                                        Snackbar.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Error creando usuario: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }
}