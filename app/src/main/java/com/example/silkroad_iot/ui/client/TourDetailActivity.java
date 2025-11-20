package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.data.GuideFb;
import com.example.silkroad_iot.data.ParadaFB;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ActivityTourDetailBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TourDetailActivity extends AppCompatActivity {

    private ActivityTourDetailBinding b;
    private final SimpleDateFormat sdfDateTime =
            new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
    private final SimpleDateFormat sdfDay =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private FirebaseFirestore db;
    private Date selectedDate;  // día elegido por el cliente (opcional)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTourDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Detalles del tour");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        TourFB tour = (TourFB) getIntent().getSerializableExtra("tour");
        if (tour == null) {
            Toast.makeText(this, "No se recibió el tour", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("TOUR_DETAIL", "Mostrando detalles de: " + tour.getDisplayName());

        // ===== Nombre =====
        String displayName = tour.getDisplayName();
        b.tTourName.setText(displayName.isEmpty() ? "Tour sin nombre" : displayName);

        // ===== Imagen =====
        String imgUrl = tour.getDisplayImageUrl();
        Glide.with(this)
                .load(imgUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(b.imgTour);

        // ===== Empresa =====
        b.tTourCompany.setText("Empresa: —");
        String empresaId = tour.getEmpresaId();
        if (empresaId != null && !empresaId.trim().isEmpty()) {
            db.collection("empresas")
                    .document(empresaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            EmpresaFb emp = doc.toObject(EmpresaFb.class);
                            if (emp != null && emp.getNombre() != null && !emp.getNombre().isEmpty()) {
                                b.tTourCompany.setText("Empresa: " + emp.getNombre());
                            }
                        }
                    });
        }

        // ===== Guía asignado (si lo hay) =====
        b.tTourGuide.setText("Guía: —");
        String guideId = tour.getAssignedGuideId();
        if (guideId != null && !guideId.trim().isEmpty()) {
            db.collection("guias")
                    .document(guideId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            GuideFb g = doc.toObject(GuideFb.class);
                            if (g != null) {
                                b.tTourGuide.setText("Guía: " + g.getDisplayName());
                            }
                        }
                    });
        }

        // ===== Idiomas =====
        String langs = tour.getLangs();
        if (langs == null || langs.trim().isEmpty()) {
            b.tTourLangs.setText("Idiomas: —");
        } else {
            b.tTourLangs.setText("Idiomas: " + langs);
        }

        // ===== Capacidad / cupos =====
        int total = tour.getCuposTotalesSafe();
        int disp  = tour.getCuposDisponiblesSafe();
        b.tTourCapacity.setText("Cupos disponibles: " + disp + " / " + total);

        // ===== Descripción =====
        String desc = tour.getDescription();
        b.tTourDescription.setText(desc != null && !desc.isEmpty()
                ? desc
                : "Sin descripción.");

        // ===== Fechas rango =====
        if (tour.getDateFrom() != null) {
            b.tTourDateFrom.setText("Inicio: " + sdfDateTime.format(tour.getDateFrom()));
        } else {
            b.tTourDateFrom.setText("Inicio: -");
        }

        if (tour.getDateTo() != null) {
            b.tTourDateTo.setText("Fin: " + sdfDateTime.format(tour.getDateTo()));
        } else {
            b.tTourDateTo.setText("Fin: -");
        }

        // ===== Selector de día entre dateFrom y dateTo =====
        setupDateSelector(tour);

        // ===== Duración / Paradas =====
        if (tour.getDuration() != null && !tour.getDuration().isEmpty()) {
            b.tTourDuration.setText("Duración: " + tour.getDuration());
        } else if (tour.getIdParadasList() != null && !tour.getIdParadasList().isEmpty()) {
            b.tTourDuration.setText("Paradas: " + tour.getIdParadasList().size());
        } else {
            b.tTourDuration.setText("Duración: -");
        }

        // ===== Lista de paradas =====
        List<String> paradaTexts = new ArrayList<>();
        if (tour.getParadas() != null && !tour.getParadas().isEmpty()) {
            for (ParadaFB p : tour.getParadas()) {
                paradaTexts.add("• " + p.getDisplayName());
            }
        } else if (!tour.getIdParadasList().isEmpty()) {
            for (String idp : tour.getIdParadasList()) {
                paradaTexts.add("• Parada " + idp);
            }
        }

        if (paradaTexts.isEmpty()) {
            b.tParadasTitle.setVisibility(View.GONE);
            b.rvParadas.setVisibility(View.GONE);
        } else {
            b.tParadasTitle.setVisibility(View.VISIBLE);
            b.rvParadas.setVisibility(View.VISIBLE);
            b.rvParadas.setLayoutManager(new LinearLayoutManager(this));
            b.rvParadas.setAdapter(new SimpleTextAdapter(paradaTexts));
        }

        // ===== Botón → Confirmar tour (reservar) =====
        double price = tour.getDisplayPrice();
        b.btnAdd.setText("Reservar S/. " + price);

        b.btnAdd.setOnClickListener(v -> {
            if (tour.getCuposDisponiblesSafe() <= 0) {
                Toast.makeText(this, "No hay cupos disponibles para este tour", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(this, ConfirmTourActivity.class);
            i.putExtra("tour", tour);

            if (selectedDate != null) {
                i.putExtra("selectedDate", selectedDate.getTime());
            }

            startActivity(i);
        });
    }

    private void setupDateSelector(TourFB tour) {
        Date from = tour.getDateFrom();
        Date to   = tour.getDateTo();

        if (from == null || to == null || from.after(to)) {
            // Sin rango válido → Spinner con solo "Sin fecha específica"
            List<String> single = new ArrayList<>();
            single.add("Sin fecha específica");
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, single);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            b.spinnerDates.setAdapter(adapter);

            selectedDate = null;
            b.tTourSelectedDate.setText("Día seleccionado: —");
            return;
        }

        // Generar lista de días entre from y to (inclusive)
        List<Date> days = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(from);

        while (!cal.getTime().after(to)) {
            Date d = cal.getTime();
            days.add(d);
            labels.add(sdfDay.format(d));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (days.isEmpty()) {
            selectedDate = null;
            b.tTourSelectedDate.setText("Día seleccionado: —");
            return;
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerDates.setAdapter(adapter);

        // Por defecto, el primer día
        selectedDate = days.get(0);
        b.tTourSelectedDate.setText("Día seleccionado: " + sdfDay.format(selectedDate));

        b.spinnerDates.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedDate = days.get(position);
                b.tTourSelectedDate.setText("Día seleccionado: " + sdfDay.format(selectedDate));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // no-op
            }
        });
    }
}