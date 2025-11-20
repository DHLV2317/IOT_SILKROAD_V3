package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.data.TourHistorialFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityTourHistoryBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TourHistoryActivity extends AppCompatActivity {

    private ActivityTourHistoryBinding b;
    private final List<TourHistorialFB> historialList = new ArrayList<>();
    private TourHistorialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTourHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // 🧭 Toolbar
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Historial de Tours");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // 🧑 Usuario actual
        User u = UserStore.get().getLogged();
        if (u == null) {
            finish();
            return;
        }

        // 🔄 Botón de filtro
        b.btnFiltrar.setOnClickListener(view -> {
            if (historialList.isEmpty()) return;

            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenu().add("Fecha de inicio del tour");
            popup.getMenu().add("Fecha de reserva");

            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();

                if (selected.equals("Fecha de inicio del tour")) {
                    historialList.sort((a, c) -> compareDates(a.getFechaRealizado(), c.getFechaRealizado()));
                } else if (selected.equals("Fecha de reserva")) {
                    historialList.sort((a, c) -> compareDates(a.getFechaReserva(), c.getFechaReserva()));
                }

                adapter.notifyDataSetChanged();
                return true;
            });

            popup.show();
        });

        // Recycler
        b.rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // 🔄 Cargar historial
        cargarHistorialDesdeFirestore(u.getEmail());
    }

    private int compareDates(Date d1, Date d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;  // null al final
        if (d2 == null) return -1;
        return d1.compareTo(d2);
    }

    private void cargarHistorialDesdeFirestore(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        historialList.clear();

        db.collection("tours_history")
                .whereEqualTo("id_usuario", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TourHistorialFB historial = doc.toObject(TourHistorialFB.class);
                        historial.setId(doc.getId());
                        historialList.add(historial);
                    }

                    if (adapter == null) {
                        adapter = new TourHistorialAdapter(historialList);
                        b.rvHistory.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    b.rvHistory.setVisibility(historialList.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }
}