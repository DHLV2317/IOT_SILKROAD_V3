package com.example.silkroad_iot.ui.guide;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideHomeBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class GuideHomeActivity extends AppCompatActivity {
    private ActivityGuideHomeBinding b;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        b = ActivityGuideHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar()!=null) getSupportActionBar().setTitle("Guía - Panel de Control");

        db = FirebaseFirestore.getInstance();
        setupClickListeners();
        loadGuideFromFirestore();
    }

    private void loadGuideFromFirestore() {
        User local = UserStore.get().getLogged();
        final String email = (local != null ? local.getEmail() : null);
        if (email == null || email.trim().isEmpty()) {
            b.txtWelcomeGuide.setText("Bienvenido");
            b.txtGuideStatus.setText("Estado: —");
            return;
        }

        db.collection("guias")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        bindGuideDoc(snap.getDocuments().get(0));
                    } else {
                        // Fallback si la colección guarda 'correo'
                        db.collection("guias")
                                .whereEqualTo("correo", email)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    if (!snap2.isEmpty()) bindGuideDoc(snap2.getDocuments().get(0));
                                    else {
                                        b.txtWelcomeGuide.setText("Bienvenido");
                                        b.txtGuideStatus.setText("Estado: (no encontrado)");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    b.txtWelcomeGuide.setText("Bienvenido");
                                    b.txtGuideStatus.setText("Estado: error");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    b.txtWelcomeGuide.setText("Bienvenido");
                    b.txtGuideStatus.setText("Estado: error");
                });
    }

    private void bindGuideDoc(DocumentSnapshot d) {
        String nombre = d.getString("nombre");
        if (nombre == null) nombre = d.getString("nombres"); // por si usaste 'nombres'
        String estado = d.getString("estado");

        b.txtWelcomeGuide.setText("¡Bienvenido, " + (nombre == null ? "Guía" : nombre) + "!");
        b.txtGuideStatus.setText("Estado: " + (estado == null ? "—" : estado));
    }

    private void setupClickListeners() {
        b.cardTourOffers.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, GuideTourOffersActivity.class)));
        b.cardLocationTracking.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, GuideLocationTrackingActivity.class)));
        b.cardQRScanner.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, GuideQRScannerActivity.class)));
        b.cardProfile.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, GuideProfileActivity.class)));
    }
}