package com.example.silkroad_iot.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ParadaFB;
import com.example.silkroad_iot.data.ServiceFB;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ActivityAdminTourDetailViewBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminTourDetailViewActivity extends AppCompatActivity {

    private ActivityAdminTourDetailViewBinding b;
    private FirebaseFirestore db;
    private TourFB tour;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAdminTourDetailViewBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        String tourId = getIntent().getStringExtra("tourId");

        if (TextUtils.isEmpty(tourId)) {
            tour = (TourFB) getIntent().getSerializableExtra("tour");
            if (tour == null || TextUtils.isEmpty(tour.getId())) { finish(); return; }
            bindTour();
            loadParadas(tour.getId());
        } else {
            showStopsLoading(true, null);
            db.collection("tours").document(tourId).get()
                    .addOnSuccessListener(d -> {
                        tour = d.toObject(TourFB.class);
                        if (tour == null) { finish(); return; }
                        if (TextUtils.isEmpty(tour.getId())) tour.setId(d.getId());
                        bindTour();
                        loadParadas(tour.getId());
                    })
                    .addOnFailureListener(e -> {
                        showStopsLoading(false, getString(R.string.error_loading_tour));
                        finish();
                    });
        }

        b.btnEdit.setOnClickListener(v -> {
            if (tour == null) return;
            Intent it = new Intent(this, AdminTourWizardActivity.class);
            it.putExtra("docId", tour.getId());
            it.putExtra("empresaId", tour.getEmpresaId());
            startActivity(it);
        });

        b.btnDelete.setOnClickListener(v -> finalizeTour());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tour != null && !TextUtils.isEmpty(tour.getId())) {
            db.collection("tours").document(tour.getId())
                    .get()
                    .addOnSuccessListener(d -> {
                        TourFB tNew = d.toObject(TourFB.class);
                        if (tNew != null) {
                            if (TextUtils.isEmpty(tNew.getId())) tNew.setId(d.getId());
                            tour = tNew;
                        }
                        bindTour();
                        loadParadas(tour.getId());
                    });
        }
    }

    private void bindTour() {

        b.tName.setText(nz(tour.getDisplayName()));
        b.tDesc.setText(nz(tour.getDescription()));
        b.tCity.setText("Ciudad: " + (TextUtils.isEmpty(tour.getCiudad()) ? "—" : tour.getCiudad()));

        b.tDuration.setText("Duración: " + nz(tour.getDuration()));
        b.tLangs.setText("Idiomas: " + nz(tour.getLangs()));

        String fechas = "—";
        if (tour.getDateFrom() != null && tour.getDateTo() != null) {
            fechas = sdf.format(tour.getDateFrom()) + " - " + sdf.format(tour.getDateTo());
        }
        b.tDates.setText("Fechas: " + fechas);

        b.tPrice.setText(String.format(Locale.getDefault(),"S/ %.2f", tour.getDisplayPrice()));
        b.tPeople.setText(String.valueOf(tour.getDisplayPeople()));

        // *** USAMOS SOLO "estado" ***
        String estado = nz(tour.getEstado());
        b.tStatus.setText("Estado: " + (estado.isEmpty() ? "—" : estado));

        boolean isFinalizado = estado.equalsIgnoreCase("finalizado");
        b.btnDelete.setEnabled(!isFinalizado);

        // Imagen
        String img = tour.getDisplayImageUrl();
        Glide.with(this)
                .load(TextUtils.isEmpty(img) ? R.drawable.ic_menu_24 : img)
                .into(b.img);

        // Guía
        b.tGuide.setText(TextUtils.isEmpty(tour.getAssignedGuideName()) ? "—" : tour.getAssignedGuideName());

        // Pago
        b.tPayment.setText(tour.getPaymentProposal() != null ?
                String.format(Locale.getDefault(),"S/ %.2f", tour.getPaymentProposal()) : "—");

        // Servicios
        b.boxServices.removeAllViews();
        List<ServiceFB> services = tour.getServices();
        if (services != null) {
            for (ServiceFB sv : services) {
                Chip c = new Chip(this);
                String name = nz(sv.getName());
                Boolean incl = sv.getIncluded();
                Double price = sv.getPrice();
                c.setText(name + (incl!=null && incl ? " · Incluido" : " · S/ " + price));
                c.setChipBackgroundColorResource(R.color.pill_gray);
                b.boxServices.addView(c);
            }
        }
    }

    private void loadParadas(String tourId) {
        b.boxStops.removeAllViews();
        showStopsLoading(true, null);

        db.collection("tours")
                .document(tourId)
                .collection("paradas")
                .orderBy("orden")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        showStopsLoading(false, getString(R.string.empty_stops));
                        return;
                    }
                    showStopsLoading(false, null);

                    for (QueryDocumentSnapshot d : snap) {
                        ParadaFB p = d.toObject(ParadaFB.class);

                        String label = (!isEmpty(p.getAddress()) ? p.getAddress() : nz(p.getNombre())) +
                                " · " + (p.getMinutes()==null ? "0" : p.getMinutes()) + " min";

                        Chip chip = new Chip(this);
                        chip.setText(label);
                        chip.setChipBackgroundColorResource(R.color.pill_gray);
                        b.boxStops.addView(chip);
                    }
                })
                .addOnFailureListener(e -> showStopsLoading(false, getString(R.string.error_loading_stops)));
    }

    private void showStopsLoading(boolean loading, String msg) {
        b.progressStops.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.tEmptyStops.setVisibility(msg != null && !loading ? View.VISIBLE : View.GONE);
        if (msg != null) b.tEmptyStops.setText(msg);
    }

    // ======================================================
    // FINALIZAR TOUR
    // ======================================================
    private void finalizeTour() {

        if (tour == null || TextUtils.isEmpty(tour.getId())) return;

        b.btnDelete.setEnabled(false);
        b.btnEdit.setEnabled(false);

        final String guideId = tour.getAssignedGuideId();

        if (!TextUtils.isEmpty(guideId)) {

            // Liberar guía
            db.collection("guias").document(guideId)
                    .update("ocupado", false, "tourActualId", null)
                    .addOnCompleteListener(t -> updateGuideHistoryAndTour(guideId))
                    .addOnFailureListener(e -> updateGuideHistoryAndTour(guideId));

        } else {
            updateTourStatus();
        }
    }

    private void updateGuideHistoryAndTour(String guideId) {

        db.collection("guias")
                .document(guideId)
                .collection("historial")
                .whereEqualTo("tourId", tour.getId())
                .whereEqualTo("estado", "asignado")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        String histId = snap.getDocuments().get(0).getId();

                        Map<String,Object> upd = new HashMap<>();
                        upd.put("estado", "finalizado");
                        upd.put("timestamp", System.currentTimeMillis());

                        db.collection("guias")
                                .document(guideId)
                                .collection("historial")
                                .document(histId)
                                .update(upd)
                                .addOnCompleteListener(t -> updateTourStatus());

                    } else {
                        updateTourStatus();
                    }
                })
                .addOnFailureListener(e -> updateTourStatus());
    }

    private void updateTourStatus() {

        db.collection("tours")
                .document(tour.getId())
                .update(
                        "estado","finalizado",
                        "publicado", false,
                        "assignedGuideId", null,
                        "assignedGuideName", null
                )
                .addOnSuccessListener(unused -> {
                    Snackbar.make(b.getRoot(),"Tour finalizado",Snackbar.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    b.btnDelete.setEnabled(true);
                    b.btnEdit.setEnabled(true);
                });
    }

    private static String nz(String s){ return s==null?"":s; }
    private static boolean isEmpty(String s){ return s==null || s.trim().isEmpty(); }
}