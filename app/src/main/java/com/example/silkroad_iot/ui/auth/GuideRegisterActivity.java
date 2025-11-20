package com.example.silkroad_iot.ui.auth;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.databinding.ActivityGuideRegisterBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuideRegisterActivity extends AppCompatActivity {

    private ActivityGuideRegisterBinding binding;
    private Uri imageUri;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // ---- Idiomas: lista + códigos ----
    private static final String[] LANGUAGE_NAMES = {
            "Español", "Inglés", "Francés", "Portugués", "Alemán", "Italiano"
    };
    private static final String[] LANGUAGE_CODES = {
            "ES", "EN", "FR", "PT", "DE", "IT"
    };
    private final boolean[] selectedLanguages = new boolean[LANGUAGE_NAMES.length];
    private String selectedLanguageCodes = ""; // guardado en BD ej: "EN,ES"

    // Lanzador para elegir imagen (opcional)
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null &&
                        result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    binding.imageProfile.setImageURI(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuideRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registro de Guía");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Tipo de documento
        String[] documentTypes = {"DNI", "Carnet de Extranjería", "Pasaporte"};
        androidx.appcompat.widget.AppCompatAutoCompleteTextView docSpinner =
                (androidx.appcompat.widget.AppCompatAutoCompleteTextView) binding.spinnerDocumentType;
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                documentTypes
        );
        docSpinner.setAdapter(adapter);

        // Fecha nacimiento
        binding.inputBirthDate.setOnClickListener(v -> showDatePickerDialog());

        // Idiomas → abre diálogo multi selección
        binding.inputLanguages.setFocusable(false);
        binding.inputLanguages.setClickable(true);
        binding.inputLanguages.setOnClickListener(v -> showLanguagesDialog());

        // Seleccionar imagen (opcional)
        binding.btnSelectImage.setOnClickListener(v -> openImagePicker());

        // Registrar guía
        binding.btnRegisterGuide.setOnClickListener(v -> registerGuide());
    }

    // ---------------- FECHA ----------------
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day   = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(
                this,
                (view, year1, month1, day1) -> {
                    String date = String.format(Locale.US, "%02d/%02d/%d",
                            day1, month1 + 1, year1);
                    binding.inputBirthDate.setText(date);
                },
                year, month, day
        ).show();
    }

    // ---------------- IDIOMAS ----------------
    private void showLanguagesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Selecciona los idiomas que hablas")
                .setMultiChoiceItems(LANGUAGE_NAMES, selectedLanguages,
                        (dialog, i, checked) -> selectedLanguages[i] = checked)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    StringBuilder names = new StringBuilder();
                    StringBuilder codes = new StringBuilder();

                    for (int i = 0; i < LANGUAGE_NAMES.length; i++) {
                        if (selectedLanguages[i]) {
                            if (names.length() > 0) names.append(", ");
                            if (codes.length() > 0) codes.append(",");
                            names.append(LANGUAGE_NAMES[i]);
                            codes.append(LANGUAGE_CODES[i]);
                        }
                    }

                    binding.inputLanguages.setText(names.toString());
                    selectedLanguageCodes = codes.toString();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------------- IMAGEN (OPCIONAL) ----------------
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    // ---------------- REGISTRO COMPLETO (Auth + Firestore) ----------------
    private void registerGuide() {
        String names          = binding.inputNames.getText().toString().trim();
        String lastNames      = binding.inputLastNames.getText().toString().trim();
        String documentType   = binding.spinnerDocumentType.getText().toString().trim();
        String documentNumber = binding.inputDocumentNumber.getText().toString().trim();
        String birthDate      = binding.inputBirthDate.getText().toString().trim();
        String email          = binding.inputEmail.getText().toString().trim();
        String phone          = binding.inputPhone.getText().toString().trim();
        String address        = binding.inputAddress.getText().toString().trim();
        String pass1          = binding.inputPass1.getText().toString().trim();
        String pass2          = binding.inputPass2.getText().toString().trim();

        // Validaciones básicas
        if (TextUtils.isEmpty(names)) {
            binding.inputNames.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(lastNames)) {
            binding.inputLastNames.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(documentType)) {
            binding.spinnerDocumentType.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(documentNumber)) {
            binding.inputDocumentNumber.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(birthDate)) {
            binding.inputBirthDate.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("Correo inválido");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.inputPhone.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            binding.inputAddress.setError("Requerido");
            return;
        }
        // Foto YA NO ES obligatoria -> no validamos imageUri

        if (TextUtils.isEmpty(pass1)) {
            binding.inputPass1.setError("Requerido");
            return;
        }
        if (pass1.length() < 6) {
            binding.inputPass1.setError("Mínimo 6 caracteres");
            return;
        }
        if (TextUtils.isEmpty(pass2)) {
            binding.inputPass2.setError("Requerido");
            return;
        }
        if (!pass1.equals(pass2)) {
            binding.inputPass2.setError("Las contraseñas no coinciden");
            return;
        }

        if (selectedLanguageCodes.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Selecciona al menos un idioma", Snackbar.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        // 1) Crear usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, pass1)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null
                            ? authResult.getUser().getUid()
                            : null;

                    if (uid == null) {
                        setLoading(false);
                        Snackbar.make(binding.getRoot(),
                                "Error: UID nulo al crear usuario",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // 2) Crear documento en "guias"
                    crearGuiaEnFirestore(uid, names, lastNames, documentType,
                            documentNumber, birthDate, email, phone, address);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(binding.getRoot(),
                            "Error creando usuario: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void crearGuiaEnFirestore(String uid,
                                      String names,
                                      String lastNames,
                                      String documentType,
                                      String documentNumber,
                                      String birthDate,
                                      String email,
                                      String phone,
                                      String address) {

        DocumentReference guiaRef = db.collection("guias").document();
        String guiaId = guiaRef.getId();

        Map<String, Object> guideData = new HashMap<>();
        guideData.put("uid", uid);
        guideData.put("email", email);
        guideData.put("fotoUrl", imageUri != null ? imageUri.toString() : "");
        guideData.put("langs", selectedLanguageCodes);   // EN,ES
        guideData.put("nombre", names + " " + lastNames);
        guideData.put("telefono", phone);
        guideData.put("direccion", address);
        guideData.put("birthDate", birthDate);
        guideData.put("documentType", documentType);
        guideData.put("documentNumber", documentNumber);
        guideData.put("historial", new ArrayList<String>());
        guideData.put("tourActual", "");
        guideData.put("estado", "Libre");
        guideData.put("guideApproved", false);
        guideData.put("guideApprovalStatus", "PENDING");

        guiaRef.set(guideData)
                .addOnSuccessListener(done -> actualizarUsuarioComoGuia(uid, guiaId, names, lastNames))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(binding.getRoot(),
                            "Error guardando guía: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void actualizarUsuarioComoGuia(String uid,
                                           String guiaId,
                                           String names,
                                           String lastNames) {

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("email", auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");
        userData.put("nombre", names + " " + lastNames);
        userData.put("rol", "GUIDE");
        userData.put("guiaId", guiaId);
        userData.put("guideApproved", false);
        userData.put("guideApprovalStatus", "PENDING");

        db.collection("usuarios")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Snackbar.make(binding.getRoot(),
                            "Solicitud de guía enviada. Espera la aprobación del superadmin.",
                            Snackbar.LENGTH_LONG).show();

                    // Cerrar activity después de mostrar mensaje
                    new android.os.Handler().postDelayed(this::finish, 2500);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(binding.getRoot(),
                            "Error guardando usuario/guía: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // ---------------- UI helper ----------------
    private void setLoading(boolean loading) {
        binding.btnRegisterGuide.setEnabled(!loading);
        binding.btnSelectImage.setEnabled(!loading);
        binding.inputNames.setEnabled(!loading);
        binding.inputLastNames.setEnabled(!loading);
        binding.spinnerDocumentType.setEnabled(!loading);
        binding.inputDocumentNumber.setEnabled(!loading);
        binding.inputBirthDate.setEnabled(!loading);
        binding.inputEmail.setEnabled(!loading);
        binding.inputPhone.setEnabled(!loading);
        binding.inputAddress.setEnabled(!loading);
        binding.inputLanguages.setEnabled(!loading);
        binding.inputPass1.setEnabled(!loading);
        binding.inputPass2.setEnabled(!loading);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}