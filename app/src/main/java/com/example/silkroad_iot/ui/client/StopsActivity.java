package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.data.ParadaFB;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ActivityStopsBinding;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StopsActivity extends AppCompatActivity {

    private ActivityStopsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityStopsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Toolbar
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lugares a visitar");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Tour del Intent
        TourFB tour = (TourFB) getIntent().getSerializableExtra("tour");
        if (tour == null) { finish(); return; }

        String name = (tour.getNombre() == null || tour.getNombre().trim().isEmpty())
                ? "(Sin nombre)" : tour.getNombre();
        b.tvStopsTitle.setText("Lugares a visitar - " + name);

        // Recycler
        b.rvStops.setLayoutManager(new LinearLayoutManager(this));

        // Lista de paradas (ParadaFB)
        List<ParadaFB> paradas = tour.getParadas();

        // Ordenar por "orden" (nulos al final)
        if (paradas != null) {
            Collections.sort(paradas, Comparator.comparingInt(p ->
                    p.getOrden() == 0 ? Integer.MAX_VALUE : p.getOrden()));
        }

        // Mostrar lista o mensaje vacío
        if (paradas == null || paradas.isEmpty()) {
            b.rvStops.setVisibility(View.GONE);
            b.tvEmptyMessage.setVisibility(View.VISIBLE);
            b.tvEmptyMessage.setText("Este tour aún no tiene paradas registradas.");
        } else {
            b.tvEmptyMessage.setVisibility(View.GONE);
            b.rvStops.setVisibility(View.VISIBLE);
            b.rvStops.setAdapter(new StopAdapter(paradas));
        }
    }
}