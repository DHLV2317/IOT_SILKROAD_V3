package com.example.silkroad_iot.ui.guide;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideTourOffersBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuideTourOffersActivity extends AppCompatActivity {

    private ActivityGuideTourOffersBinding binding;
    private TourOfferAdapter tourOfferAdapter;
    private final List<TourFB> tourOfferList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuideTourOffersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ofertas de Tours Disponibles");

        db = FirebaseFirestore.getInstance();

        binding.recyclerViewTourOffers.setLayoutManager(new LinearLayoutManager(this));
        tourOfferAdapter = new TourOfferAdapter(tourOfferList);
        binding.recyclerViewTourOffers.setAdapter(tourOfferAdapter);

        loadTourOffers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTourOffers();
    }

    private void loadTourOffers() {
        User u = UserStore.get().getLogged();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        if (u == null) {
            tourOfferList.clear();
            tourOfferAdapter.notifyDataSetChanged();
            binding.progressBar.setVisibility(View.GONE);
            binding.textEmpty.setText("Inicia sesión para ver ofertas.");
            binding.textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // 🔹 Tomamos TODOS los tours y filtramos en cliente usando TourFB.isAvailableForOffers()
        db.collection("tours")
                .get()
                .addOnSuccessListener(snap -> {
                    tourOfferList.clear();

                    for (QueryDocumentSnapshot d : snap) {
                        TourFB tour = d.toObject(TourFB.class);
                        // IMPORTANTE: guardar el id del documento
                        tour.setId(d.getId());

                        // Solo mostrar tours:
                        //  - status/estado = pendiente
                        //  - publicado = true
                        //  - sin guía asignado
                        if (tour.isAvailableForOffers()) {
                            tourOfferList.add(tour);
                        }
                    }

                    tourOfferAdapter.notifyDataSetChanged();

                    binding.progressBar.setVisibility(View.GONE);
                    if (tourOfferList.isEmpty()) {
                        binding.textEmpty.setText("No tienes ofertas pendientes.");
                        binding.textEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.textEmpty.setText("Error cargando ofertas.");
                    binding.textEmpty.setVisibility(View.VISIBLE);
                });
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