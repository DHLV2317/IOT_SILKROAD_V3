package com.example.silkroad_iot.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ReservaWithTour;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.TourHistorialFB;
import com.example.silkroad_iot.databinding.ContentAdminReservationsBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.example.silkroad_iot.ui.util.PdfReportUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReservationsActivity extends BaseDrawerActivity {

    private ContentAdminReservationsBinding b;
    private FirebaseFirestore db;

    private final List<ReservaWithTour> fullList = new ArrayList<>();
    private AdminReservationsAdapter adapter;

    private static final String PREFS = "app_prefs";
    private static final String KEY_EMPRESA_ID = "empresa_id";

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);

        // Inserta el contenido dentro del drawer
        setupDrawer(R.layout.content_admin_reservations, R.menu.menu_drawer_admin, "Reservas");

        b = ContentAdminReservationsBinding.bind(findViewById(R.id.rootContent));
        db = FirebaseFirestore.getInstance();

        // Recycler + adapter
        b.list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReservationsAdapter(new ArrayList<>());
        b.list.setAdapter(adapter);

        // Búsqueda por texto
        b.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b2, int c2) {}
            @Override public void afterTextChanged(Editable s) {
                String q = (s == null) ? "" : s.toString();
                adapter.filter(q, adapter.getStatusFilter());
            }
        });

        // ===== Filtro por estado =====
        String[] estados = new String[]{
                "Todos",
                "pendiente",
                "aceptado",
                "rechazado",
                "check-in",
                "check-out",
                "finalizada",
                "cancelado"
        };

        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, estados);

        b.inputStatus.setAdapter(statusAdapter);
        b.inputStatus.setText("Todos", false);

        b.inputStatus.setOnItemClickListener((p, v, pos, id) -> {
            String sel = estados[pos];
            adapter.setStatusFilter(sel);
            String q = b.inputSearch.getText() == null
                    ? "" : b.inputSearch.getText().toString();
            adapter.filter(q, sel);
        });

        // Botón generar PDF
        b.btnReport.setOnClickListener(v -> {
            List<Object> items = new ArrayList<>();
            items.addAll(fullList);     // usamos todas las reservas de la empresa (o de todo)
            PdfReportUtil.createReservationsPdf(this, items);
        });

        // Carga inicial
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // por si hubo nuevas reservas
        loadData();
    }

    @Override
    protected int defaultMenuId() { return R.id.m_reservations; }

    // =========================================================
    // CARGA DE DATOS
    // =========================================================
    private void loadData() {
        String empresaId = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_EMPRESA_ID, null);

        if (empresaId == null || empresaId.isEmpty()) {
            // 🔁 SIN empresa asociada → mostrar TODAS las reservas (todas las empresas)
            loadAllReservations();
        } else {
            // 🔒 Con empresa asociada → solo reservas de esa empresa
            loadReservationsByEmpresa(empresaId);
        }
    }

    /**
     * Carga TODAS las reservas de tours_history, usando todos los tours existentes.
     */
    private void loadAllReservations() {
        // 1) Traer TODOS los tours
        db.collection("tours")
                .get()
                .addOnSuccessListener(tourSnap -> {
                    Map<String, TourFB> tourMap = new HashMap<>();

                    for (QueryDocumentSnapshot d : tourSnap) {
                        TourFB t = d.toObject(TourFB.class);
                        t.setId(d.getId());
                        tourMap.put(d.getId(), t);
                    }

                    // 2) Traer todas las reservas
                    db.collection("tours_history")
                            .get()
                            .addOnSuccessListener(hSnap -> {
                                fullList.clear();

                                for (QueryDocumentSnapshot h : hSnap) {
                                    TourHistorialFB r = h.toObject(TourHistorialFB.class);
                                    r.setId(h.getId());

                                    TourFB tour = tourMap.get(r.getIdTour());
                                    if (tour == null) {
                                        // Si el tour se borró o no existe, igual podrías decidir
                                        // mostrarlo con un "Tour eliminado", pero por ahora lo omitimos.
                                        continue;
                                    }

                                    fullList.add(new ReservaWithTour(r, tour));
                                }

                                adapter.replace(fullList);

                                String q = b.inputSearch.getText() == null
                                        ? "" : b.inputSearch.getText().toString();
                                adapter.filter(q, adapter.getStatusFilter());
                            });
                });
    }

    /**
     * Carga solo las reservas de tours pertenecientes a una empresa específica.
     */
    private void loadReservationsByEmpresa(String empresaId) {

        // 1) Obtener tours de la empresa
        db.collection("tours")
                .whereEqualTo("empresaId", empresaId)
                .get()
                .addOnSuccessListener(tourSnap -> {

                    Map<String, TourFB> tourMap = new HashMap<>();

                    for (QueryDocumentSnapshot d : tourSnap) {
                        TourFB t = d.toObject(TourFB.class);
                        t.setId(d.getId());
                        tourMap.put(d.getId(), t);
                    }

                    if (tourMap.isEmpty()) {
                        adapter.replace(new ArrayList<>());
                        fullList.clear();
                        return;
                    }

                    // 2) Obtener TODAS las reservas y filtrar por id_tour
                    db.collection("tours_history")
                            .get()
                            .addOnSuccessListener(hSnap -> {

                                fullList.clear();

                                for (QueryDocumentSnapshot h : hSnap) {
                                    TourHistorialFB r = h.toObject(TourHistorialFB.class);
                                    r.setId(h.getId());

                                    TourFB tour = tourMap.get(r.getIdTour());
                                    if (tour == null) {
                                        // reserva de otro tour/empresa
                                        continue;
                                    }

                                    fullList.add(new ReservaWithTour(r, tour));
                                }

                                adapter.replace(fullList);

                                // aplicar filtros actuales
                                String q = b.inputSearch.getText() == null
                                        ? "" : b.inputSearch.getText().toString();
                                adapter.filter(q, adapter.getStatusFilter());
                            });
                });
    }
}