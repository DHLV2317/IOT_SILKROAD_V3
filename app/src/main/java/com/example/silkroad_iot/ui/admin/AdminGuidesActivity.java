package com.example.silkroad_iot.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.GuideFb;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ContentAdminGuidesBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.*;

import java.util.*;

public class AdminGuidesActivity extends BaseDrawerActivity implements OnMapReadyCallback {

    private ContentAdminGuidesBinding b;

    private FirebaseFirestore db;
    private ListenerRegistration guidesReg;

    private final List<GuideFb> guides = new ArrayList<>();
    private final List<TourFB> toursCache = new ArrayList<>();

    private AdminGuidesAdapter adapter;

    private GoogleMap mMap;
    private boolean isMapReady = false;

    private final Map<String, Marker> markers = new HashMap<>();
    private final Map<String, Polyline> guidePolylines = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawer(R.layout.content_admin_guides, R.menu.menu_drawer_admin, "Guías");
        b = ContentAdminGuidesBinding.bind(findViewById(R.id.rootContent));

        db = FirebaseFirestore.getInstance();

        adapter = new AdminGuidesAdapter(guides, new AdminGuidesAdapter.Callbacks() {
            @Override public void onAssignClicked(int pos) { showAssignTourDialog(pos); }
            @Override public void onDetailClicked(int pos) { showGuideDetail(pos); }
        });

        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);

        b.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                adapter.filter(s == null ? "" : s.toString());
            }
        });

        b.progress.setVisibility(View.VISIBLE);
        b.tEmpty.setVisibility(View.GONE);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapAdmin);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        attachGuidesListener();
        preloadTours();
    }

    @Override
    protected int defaultMenuId() { return R.id.m_guides; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (guidesReg != null) guidesReg.remove();
    }

    // ============================================================
    //   MAPA
    // ============================================================

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady = true;

        LatLng lima = new LatLng(-12.0464, -77.0428);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f));

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String) {
                String guideId = (String) tag;
                loadGuidePath(guideId);
            }
            return false;
        });

        refreshMarkers();
    }

    private void refreshMarkers() {
        if (!isMapReady || mMap == null) return;

        markers.clear();

        for (GuideFb g : guides) {
            if (g.getLatActual() == null || g.getLngActual() == null) continue;

            LatLng pos = new LatLng(g.getLatActual(), g.getLngActual());

            Marker mk = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(g.getNombre())
                    .snippet("Estado: " + g.getEstado()));

            if (mk != null) {
                mk.setTag(g.getId());
                markers.put(g.getId(), mk);
            }
        }
    }

    private void loadGuidePath(String guideId) {
        db.collection("guias")
                .document(guideId)
                .collection("ubicaciones")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {

                    Polyline old = guidePolylines.get(guideId);
                    if (old != null) old.remove();

                    List<LatLng> points = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        if (lat != null && lng != null) points.add(new LatLng(lat, lng));
                    }

                    if (points.isEmpty()) {
                        Toast.makeText(this, "Sin recorrido registrado.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Polyline poly = mMap.addPolyline(
                            new PolylineOptions().addAll(points).width(8f)
                    );
                    guidePolylines.put(guideId, poly);

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            points.get(points.size() - 1), 16f));
                });
    }

    // ============================================================
    //   FIRESTORE LISTENER (guias)
    // ============================================================

    private void attachGuidesListener() {
        Query q = db.collection("guias")
                .whereEqualTo("guideApproved", true)
                .whereEqualTo("guideApprovalStatus", "APPROVED");

        guidesReg = q.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null) {
                b.progress.setVisibility(View.GONE);
                Toast.makeText(this, "Error cargando guías.", Toast.LENGTH_LONG).show();
                return;
            }

            guides.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                GuideFb g = d.toObject(GuideFb.class);
                if (g != null) {
                    g.setId(d.getId());
                    guides.add(g);
                }
            }

            adapter.updateData(guides);

            b.progress.setVisibility(View.GONE);
            b.tEmpty.setVisibility(guides.isEmpty() ? View.VISIBLE : View.GONE);

            refreshMarkers();
        });
    }

    // ============================================================
    //   TOURS
    // ============================================================

    private void preloadTours() {
        db.collection("tours")
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("assignedGuideId", null)
                .get()
                .addOnSuccessListener(snap -> {
                    toursCache.clear();
                    for (DocumentSnapshot d : snap) {
                        TourFB t = d.toObject(TourFB.class);
                        if (t != null) {
                            t.setId(d.getId());
                            toursCache.add(t);
                        }
                    }
                });
    }

    // ============================================================
    //   ASIGNAR TOUR
    // ============================================================

    public void showAssignTourDialog(int idx) {
        GuideFb guide = guides.get(idx);

        if (toursCache.isEmpty()) {
            Toast.makeText(this, "No hay tours pendientes para asignar.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> nombres = new ArrayList<>();
        for (TourFB t : toursCache) nombres.add(t.getDisplayName());

        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, nombres);

        new AlertDialog.Builder(this)
                .setTitle("Asignar Tour")
                .setAdapter(ad, (dialog, pos) -> {
                    assignTourToGuide(guide, toursCache.get(pos));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void assignTourToGuide(GuideFb guide, TourFB tour) {

        Map<String, Object> upd = new HashMap<>();
        upd.put("assignedGuideId", guide.getId());
        upd.put("assignedGuideName", guide.getNombre());
        upd.put("status", "EN_CURSO");

        db.collection("tours")
                .document(tour.getId())
                .update(upd)
                .addOnSuccessListener(unused -> {

                    Map<String, Object> hist = new HashMap<>();
                    hist.put("tourId", tour.getId());
                    hist.put("tourName", tour.getDisplayName());
                    hist.put("timestamp", System.currentTimeMillis());
                    hist.put("status", "EN_CURSO");

                    db.collection("guias")
                            .document(guide.getId())
                            .collection("historial")
                            .add(hist);

                    Toast.makeText(this,
                            "Tour asignado a " + guide.getNombre(),
                            Toast.LENGTH_LONG).show();

                    preloadTours();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // ============================================================
    //   DETALLE
    // ============================================================

    public void showGuideDetail(int idx) {
        GuideFb g = guides.get(idx);
        Intent i = new Intent(this, AdminGuideDetailActivity.class);
        i.putExtra("guideId", g.getId());
        startActivity(i);
    }
}