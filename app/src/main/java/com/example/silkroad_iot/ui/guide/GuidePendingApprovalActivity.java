package com.example.silkroad_iot.ui.guide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuidePendingApprovalBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.Date;

public class GuidePendingApprovalActivity extends AppCompatActivity {

    private ActivityGuidePendingApprovalBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final UserStore store = UserStore.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuidePendingApprovalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Cuenta Pendiente");

        // Si ya está aprobado en memoria → ir directo al panel del guía
        User u = store.getLogged();
        if (u != null && u.isGuideApproved() && u.isGuide()) {
            goHomeAndFinish();
            return;
        }

        setupUserInfo();
        setupClickListeners();

        // Chequeo automático al abrir
        refreshFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Revalida por si cambió mientras la app estaba en background
        refreshFromFirestore();
    }

    private void setupUserInfo() {
        User guide = store.getLogged();
        if (guide == null) return;

        String ln = guide.getLastName() == null ? "" : (" " + guide.getLastName());
        binding.txtGuideName.setText((guide.getName() == null ? "" : guide.getName()) + ln);
        binding.txtGuideEmail.setText(guide.getEmail());

        if (guide.getDocumentNumber() != null) {
            String dt = guide.getDocumentType() == null ? "Doc" : guide.getDocumentType();
            binding.txtDocumentInfo.setText(dt + ": " + guide.getDocumentNumber());
        }
        if (guide.getPhone() != null) binding.txtPhoneInfo.setText("Teléfono: " + guide.getPhone());
        if (guide.getLanguages() != null) binding.txtLanguagesInfo.setText("Idiomas: " + guide.getLanguages());

        showStatus(guide.getGuideApprovalStatus());
    }

    private void setupClickListeners() {
        binding.btnBackToLogin.setOnClickListener(v -> {
            store.logout();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });

        binding.btnRefreshStatus.setOnClickListener(v -> refreshFromFirestore());
    }

    private void setLoading(boolean loading) {
        binding.btnRefreshStatus.setEnabled(!loading);
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String status) {
        if (status == null) status = "PENDING";
        switch (status) {
            case "PENDING":
                binding.txtStatusInfo.setText("⏳ Pendiente de Aprobación");
                binding.txtStatusDescription.setText("Tu solicitud está siendo revisada por nuestro equipo de administradores.");
                break;
            case "REJECTED":
                binding.txtStatusInfo.setText("❌ Solicitud Rechazada");
                binding.txtStatusDescription.setText("Lo sentimos, tu solicitud ha sido rechazada. Contacta con soporte para más información.");
                break;
            case "APPROVED":
                binding.txtStatusInfo.setText("✅ Aprobado");
                binding.txtStatusDescription.setText("¡Listo! Puedes ingresar a tu panel de guía.");
                break;
            default:
                binding.txtStatusInfo.setText("⏳ En Revisión");
                binding.txtStatusDescription.setText("Tu solicitud está siendo procesada.");
        }
    }

    /** Consulta Firestore en colección 'usuarios' (donde está el rol GUIDE). */
    private void refreshFromFirestore() {
        User u = store.getLogged();
        if (u == null || u.getEmail() == null || u.getEmail().isEmpty()) {
            Snackbar.make(binding.getRoot(), "No hay sesión de guía cargada.", Snackbar.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        db.collection("usuarios")
                .whereEqualTo("email", u.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    setLoading(false);
                    if (snap.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "No se encontró tu perfil en Firestore.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    handleUserDoc(u, snap.getDocuments().get(0));
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(binding.getRoot(), "Error consultando estado: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void handleUserDoc(User u, DocumentSnapshot d) {
        // ---- lectura tolerante de campos ----
        String rol = d.getString("rol");                       // "GUIDE"
        Boolean aprobadoBool = d.getBoolean("aprobado");       // true/false (opcional)
        String status = d.getString("guideApprovalStatus");    // "APPROVED"/"PENDING"/"REJECTED" (opcional)
        String estado = d.getString("estado");                 // "Aprobado"/"Pendiente" (opcional)

        boolean isApproved = false;
        if (aprobadoBool != null && aprobadoBool) isApproved = true;
        if ("APPROVED".equalsIgnoreCase(status)) isApproved = true;
        if ("Aprobado".equalsIgnoreCase(estado)) isApproved = true;

        if (status == null) status = isApproved ? "APPROVED" : "PENDING";

        // ---- actualiza cache local ----
        if (rol != null && rol.equalsIgnoreCase("GUIDE")) {
            u.setRole(User.Role.GUIDE); // fuerza el rol correcto para el ruteo
        }
        u.setGuideApproved(isApproved);
        u.setGuideApprovalStatus(status);

        store.updateLogged(u);

        // ---- UI ----
        showStatus(status);
        binding.txtLastCheck.setText("Última verificación: " +
                DateFormat.getDateTimeInstance().format(new Date()));

        // ---- navegación ----
        if (u.isGuideApproved() && u.getRole() == User.Role.GUIDE) {
            Toast.makeText(this, "¡Tu cuenta de guía fue aprobada!", Toast.LENGTH_SHORT).show();
            goHomeAndFinish();
        }
    }

    private void goHomeAndFinish() {
        startActivity(new Intent(this, GuideHomeActivity.class));
        finish();
    }
}