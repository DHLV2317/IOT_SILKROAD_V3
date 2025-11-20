package com.example.silkroad_iot.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.data.GuideFb;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideProfileBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;

public class GuideProfileActivity extends AppCompatActivity {

    private ActivityGuideProfileBinding b;
    private Uri newImageUri;
    private FirebaseFirestore db;
    private String guideDocId; // doc id en "guias"

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    newImageUri = result.getData().getData();
                    b.imgProfilePhoto.setImageURI(newImageUri);
                    b.btnSaveChanges.setEnabled(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuideProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
        }

        db = FirebaseFirestore.getInstance();
        setupClickListeners();
        setupFormWatchers();
        fetchGuide();
    }

    private void setupFormWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.btnSaveChanges.setEnabled(true);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        b.inputNames.addTextChangedListener(watcher);
        b.inputLastNames.addTextChangedListener(watcher);
        b.inputPhone.addTextChangedListener(watcher);
        b.inputAddress.addTextChangedListener(watcher);
        b.inputLanguages.addTextChangedListener(watcher);
    }

    private void fetchGuide() {
        User u = UserStore.get().getLogged();
        String email = (u != null ? u.getEmail() : null);
        if (email == null || email.isEmpty()) return;

        db.collection("guias")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    DocumentSnapshot d = snap.getDocuments().get(0);
                    guideDocId = d.getId();

                    GuideFb guide = d.toObject(GuideFb.class);
                    if (guide == null) guide = new GuideFb();

                    // ==== Datos base ====
                    String nombre = guide.getNombre();
                    String apellidos = guide.getApellidos();
                    String telefono = guide.getTelefono();
                    String direccion = guide.getDireccion();
                    String langs = guide.getLangs();
                    String foto = guide.getFotoUrl();

                    String fullName = guide.getDisplayName();

                    b.txtProfileName.setText(fullName);
                    b.inputNames.setText(nombre == null ? "" : nombre);
                    b.inputLastNames.setText(apellidos == null ? "" : apellidos);
                    b.inputEmail.setText(email);
                    b.inputPhone.setText(telefono == null ? "" : telefono);
                    b.inputAddress.setText(direccion == null ? "" : direccion);
                    b.inputLanguages.setText(langs == null ? "" : langs);

                    if (foto != null && !foto.isEmpty()) {
                        try {
                            b.imgProfilePhoto.setImageURI(Uri.parse(foto));
                        } catch (Exception ignore) {
                        }
                    }

                    // ==== Estado de aprobación ====
                    boolean aprobadoSafe = guide.isAprobadoSafe();
                    String approvalStatus = guide.getGuideApprovalStatus();
                    if (aprobadoSafe) {
                        b.txtApprovalStatus.setText("Estado: ✅ Aprobado");
                    } else {
                        if (approvalStatus != null && !approvalStatus.trim().isEmpty()) {
                            b.txtApprovalStatus.setText("Estado: " + approvalStatus);
                        } else {
                            b.txtApprovalStatus.setText("Estado: ⏳ En revisión");
                        }
                    }

                    // ==== DNI / Nacimiento (si existen campos) ====
                    String dni = d.getString("dni");
                    String birth = d.getString("birthDate");
                    b.txtDocumentInfo.setText("DNI: " + (dni == null ? "—" : dni));
                    b.txtBirthDate.setText("Nacimiento: " + (birth == null ? "—" : birth));

                    // ==== Cargar estadísticas desde historial ====
                    loadGuideStats();
                });
    }

    private void loadGuideStats() {
        if (guideDocId == null) return;

        db.collection("guias")
                .document(guideDocId)
                .collection("historial")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    int totalTours = 0;
                    double totalEarnings = 0.0;
                    double sumRatings = 0.0;
                    int ratingCount = 0;

                    StringBuilder recent = new StringBuilder();

                    int shown = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        totalTours++;

                        Double payment = doc.getDouble("payment");
                        if (payment != null) {
                            totalEarnings += payment;
                        }

                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            sumRatings += rating;
                            ratingCount++;
                        }

                        if (shown < 3) {
                            String tName = doc.getString("tourName");
                            String estado = doc.getString("estado");
                            Long ts = doc.getLong("timestamp");

                            recent.append("• ")
                                    .append(tName == null ? "Tour" : tName);
                            if (estado != null) {
                                recent.append(" (").append(estado).append(")");
                            }
                            if (ts != null) {
                                // Podrías formatear fecha; aquí solo se usa como info de orden
                            }
                            recent.append("\n");
                            shown++;
                        }
                    }

                    b.txtTotalTours.setText(
                            String.format(Locale.getDefault(),
                                    "Tours realizados: %d", totalTours)
                    );

                    b.txtTotalEarnings.setText(
                            String.format(Locale.getDefault(),
                                    "Ingresos totales: S/ %.2f", totalEarnings)
                    );

                    if (ratingCount > 0) {
                        double avg = sumRatings / ratingCount;
                        b.txtAverageRating.setText(
                                String.format(Locale.getDefault(),
                                        "Calificación: ⭐ %.1f/5.0", avg)
                        );
                    } else {
                        b.txtAverageRating.setText("Calificación: —");
                    }

                    if (shown > 0) {
                        b.txtRecentHistory.setText(recent.toString().trim());
                    } else {
                        b.txtRecentHistory.setText("Aún no tienes tours en tu historial.");
                    }
                });
    }

    private void setupClickListeners() {
        b.btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        b.btnSaveChanges.setOnClickListener(v -> saveProfileChanges());

        b.btnViewFullHistory.setOnClickListener(v ->
                startActivity(new Intent(this, GuideHistoryActivity.class)));

        b.btnLogout.setOnClickListener(v -> {
            UserStore.get().logout();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveProfileChanges() {
        if (guideDocId == null) {
            Snackbar.make(b.getRoot(), "No se encontró el documento del guía", Snackbar.LENGTH_LONG).show();
            return;
        }

        db.collection("guias").document(guideDocId)
                .update(
                        "nombre", b.inputNames.getText() == null ? "" : b.inputNames.getText().toString().trim(),
                        "apellidos", b.inputLastNames.getText() == null ? "" : b.inputLastNames.getText().toString().trim(),
                        "telefono", b.inputPhone.getText() == null ? "" : b.inputPhone.getText().toString().trim(),
                        "direccion", b.inputAddress.getText() == null ? "" : b.inputAddress.getText().toString().trim(),
                        "langs", b.inputLanguages.getText() == null ? "" : b.inputLanguages.getText().toString().trim(),
                        "fotoUrl", (newImageUri != null ? newImageUri.toString() : null)
                )
                .addOnSuccessListener(unused -> {
                    Snackbar.make(b.getRoot(), "✅ Perfil actualizado", Snackbar.LENGTH_LONG).show();
                    b.btnSaveChanges.setEnabled(false);
                })
                .addOnFailureListener(e ->
                        Snackbar.make(b.getRoot(), "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
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