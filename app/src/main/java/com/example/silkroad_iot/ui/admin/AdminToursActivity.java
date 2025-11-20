package com.example.silkroad_iot.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ContentAdminToursBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminToursActivity extends BaseDrawerActivity {

    private ContentAdminToursBinding c;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration reg;

    private final List<TourFB> allTours = new ArrayList<>();
    private AdminToursAdapter adapter;

    // Empresa actual del admin (se obtiene desde usuarios/{uid})
    private String empresaId;

    // Preferencias compartidas para que otros módulos reutilicen empresaId
    private static final String PREFS = "app_prefs";
    private static final String KEY_EMPRESA_ID = "empresa_id";

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);

        // Drawer + toolbar
        setupDrawer(R.layout.content_admin_tours, R.menu.menu_drawer_admin, "Mis Tours");

        FrameLayout container = findViewById(R.id.contentContainer);
        c = ContentAdminToursBinding.bind(container.getChildAt(0));

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Recycler
        c.recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminToursAdapter(new ArrayList<>());
        c.recycler.setAdapter(adapter);

        // Buscador
        c.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c1, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c2) { filter(s); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Añadir nuevo tour (pasando empresaId al wizard)
        c.btnAdd.setOnClickListener(v -> {
            if (empresaId == null || empresaId.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        "Aún no se ha cargado tu empresa. Espera un momento e inténtalo de nuevo.",
                        Toast.LENGTH_SHORT
                ).show();
                startListeningTours(); // reintento suave
                return;
            }

            Intent it = new Intent(this, AdminTourWizardActivity.class);
            it.putExtra("empresaId", empresaId);
            startActivity(it);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListeningTours();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void startListeningTours() {
        if (reg != null) { reg.remove(); reg = null; }

        c.progress.setVisibility(View.VISIBLE);
        c.empty.setVisibility(View.GONE);

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            c.progress.setVisibility(View.GONE);
            showEmpty("Inicia sesión para ver tus tours.");
            return;
        }

        // 1) Obtener empresaId desde usuarios/{uid}
        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(this::onUserLoadedForEmpresa)
                .addOnFailureListener(e -> {
                    c.progress.setVisibility(View.GONE);
                    showEmpty("No se pudo obtener la empresa del usuario.");
                });
    }

    private void onUserLoadedForEmpresa(DocumentSnapshot doc) {
        if (!doc.exists()) {
            c.progress.setVisibility(View.GONE);
            showEmpty("No se encontró información del usuario.");
            return;
        }

        empresaId = doc.getString("empresaId"); // guardamos la empresa para el botón +

        if (empresaId == null || empresaId.isEmpty()) {
            c.progress.setVisibility(View.GONE);
            showEmpty("Tu usuario no tiene empresa asignada.");
            return;
        }

        // Guardamos también en SharedPreferences para que otros módulos lo reutilicen
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMPRESA_ID, empresaId)
                .apply();

        // 2) Cargar tours según empresaId en tiempo real
        if (reg != null) { reg.remove(); }
        reg = db.collection("tours")
                .whereEqualTo("empresaId", empresaId)
                .addSnapshotListener((snap, err) -> {
                    c.progress.setVisibility(View.GONE);
                    if (err != null || snap == null) {
                        showEmpty("No se pudieron cargar los tours.");
                        return;
                    }

                    allTours.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        TourFB t = d.toObject(TourFB.class);
                        if (t != null) {
                            t.setId(d.getId());
                            allTours.add(t);
                        }
                    }

                    applyData();
                });
    }

    private void applyData() {
        if (allTours.isEmpty()) {
            adapter.replace(new ArrayList<>());
            showEmpty("Aún no has creado tours.");
        } else {
            c.empty.setVisibility(View.GONE);
            adapter.replace(new ArrayList<>(allTours));
            filter(c.inputSearch.getText());
        }
    }

    private void filter(CharSequence s) {
        String q = (s == null) ? "" : s.toString().trim().toLowerCase();
        List<TourFB> filtered = new ArrayList<>();
        for (TourFB t : allTours) {
            String name   = safe(t.getDisplayName());
            String city   = safe(t.getCiudad());
            String desc   = safe(t.getDescription());
            String estado = safe(t.getEstado());

            if (q.isEmpty()
                    || name.toLowerCase().contains(q)
                    || city.toLowerCase().contains(q)
                    || desc.toLowerCase().contains(q)
                    || estado.toLowerCase().contains(q)) {
                filtered.add(t);
            }
        }
        adapter.replace(filtered);

        if (filtered.isEmpty()) showEmpty("Sin resultados para \"" + q + "\"");
        else c.empty.setVisibility(View.GONE);
    }

    private void showEmpty(String msg) {
        c.empty.setText(msg);
        c.empty.setVisibility(View.VISIBLE);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    protected int defaultMenuId() { return R.id.m_tours; }
}