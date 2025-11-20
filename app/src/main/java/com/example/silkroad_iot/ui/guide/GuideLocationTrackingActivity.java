package com.example.silkroad_iot.ui.guide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideLocationTrackingBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuideLocationTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityGuideLocationTrackingBinding b;
    private GoogleMap mMap;

    private FirebaseFirestore db;
    private String guideDocId;
    private String assignedTourId = null;

    private FusedLocationProviderClient fused;
    private LocationCallback callback;
    private LocationRequest request;

    private static final int REQ = 778;

    // Estado del tour
    private boolean tourStarted = false;
    private boolean tourFinished = false;
    private int stopCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuideLocationTrackingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Toolbar
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Seguimiento en Tiempo Real");
        }

        db = FirebaseFirestore.getInstance();
        fused = LocationServices.getFusedLocationProviderClient(this);

        request = LocationRequest.create()
                .setInterval(4000)
                .setFastestInterval(2500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    updateLocation(loc);
                }
            }
        };

        // Resolver guía logueado y tour asignado
        resolveGuideDocId();

        // Mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapGuide);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Estado inicial UI
        updateStopCounter();
        b.txtGpsStatus.setText("GPS: verificando...");
        b.btnRegisterLocation.setEnabled(false);
        b.btnNextStop.setEnabled(true);
        b.btnNextStop.setText("▶ Iniciar tour");
        b.btnFinishTour.setVisibility(android.view.View.GONE);

        // Listeners
        b.btnNextStop.setOnClickListener(v -> onNextStopClicked());
        b.btnRegisterLocation.setOnClickListener(v -> onRegisterLocationClicked());
        b.btnFinishTour.setOnClickListener(v -> onFinishTourClicked());
    }

    // Obtiene el documento del guía por email y el tour asignado
    private void resolveGuideDocId() {
        User u = UserStore.get().getLogged();
        if (u == null) return;

        db.collection("guias")
                .whereEqualTo("email", u.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        guideDocId = snap.getDocuments().get(0).getId();
                        assignedTourId = snap.getDocuments().get(0).getString("tourIdAsignado");
                    }
                });
    }

    // Actualiza ubicación en pantalla y en Firestore (latActual/lngActual)
    private void updateLocation(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        b.txtCurrentStop.setText("Ubicación Actual");
        b.txtDemoCoordinates.setText(
                String.format(Locale.getDefault(), "Lat: %.6f\nLng: %.6f", lat, lng));
        b.txtGpsStatus.setText("GPS: activo ✓");

        if (mMap != null) {
            LatLng p = new LatLng(lat, lng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(p).title("Mi ubicación"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 17f));
        }

        if (guideDocId != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("latActual", lat);
            map.put("lngActual", lng);
            map.put("lastUpdate", System.currentTimeMillis());

            db.collection("guias").document(guideDocId).update(map);
        }
    }

    // Click en "Iniciar tour" / "Siguiente parada"
    private void onNextStopClicked() {
        if (!tourStarted) {
            if (assignedTourId == null) {
                Snackbar.make(b.getRoot(), "No tienes un tour asignado actualmente.", Snackbar.LENGTH_LONG).show();
                return;
            }
            tourStarted = true;
            tourFinished = false;
            stopCount = 0;
            updateStopCounter();

            b.btnRegisterLocation.setEnabled(true);
            b.btnNextStop.setText("➡️ Siguiente parada");
            b.btnFinishTour.setVisibility(android.view.View.GONE);

            Snackbar.make(b.getRoot(), "Tour iniciado. Registra la primera parada.", Snackbar.LENGTH_LONG).show();

            if (guideDocId != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("tourTrackingStatus", "STARTED");
                db.collection("guias").document(guideDocId).update(map);
            }

        } else if (!tourFinished) {
            Snackbar.make(b.getRoot(),
                    "Muévete hacia la siguiente parada y luego pulsa \"Registrar ubicación actual\".",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // Click en "Registrar ubicación actual"
    private void onRegisterLocationClicked() {
        if (!tourStarted) {
            Snackbar.make(b.getRoot(), "Primero inicia el tour.", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (tourFinished) {
            Snackbar.make(b.getRoot(), "El tour ya fue finalizado.", Snackbar.LENGTH_LONG).show();
            return;
        }

        String coordsText = b.txtDemoCoordinates.getText().toString();
        if (!coordsText.contains("Lat")) {
            Snackbar.make(b.getRoot(), "Aún no se obtiene la ubicación GPS.", Snackbar.LENGTH_LONG).show();
            return;
        }

        double lat, lng;
        try {
            String[] lines = coordsText.split("\n");
            lat = Double.parseDouble(lines[0].replace("Lat:", "").trim());
            lng = Double.parseDouble(lines[1].replace("Lng:", "").trim());
        } catch (Exception e) {
            Snackbar.make(b.getRoot(), "Error al leer las coordenadas.", Snackbar.LENGTH_LONG).show();
            return;
        }

        registerStop(lat, lng);
    }

    // Registra parada en subcolección /guias/{id}/ubicaciones
    private void registerStop(double lat, double lng) {
        if (guideDocId == null || assignedTourId == null) {
            Snackbar.make(b.getRoot(), "No se pudo asociar la parada al guía/tour.", Snackbar.LENGTH_LONG).show();
            return;
        }

        long now = System.currentTimeMillis();

        Map<String, Object> stop = new HashMap<>();
        stop.put("lat", lat);
        stop.put("lng", lng);
        stop.put("timestamp", now);
        stop.put("tourId", assignedTourId);

        db.collection("guias")
                .document(guideDocId)
                .collection("ubicaciones")
                .add(stop);

        stopCount++;
        updateStopCounter();

        // Mostrar en historial local
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());
        String line = String.format(
                Locale.getDefault(),
                "Parada #%d [%s]\n  Lat: %.6f, Lng: %.6f\n\n",
                stopCount, sdf.format(new Date(now)), lat, lng
        );
        b.txtLocationHistory.append(line);

        if (stopCount > 0) {
            b.btnFinishTour.setVisibility(android.view.View.VISIBLE);
        }

        Snackbar.make(b.getRoot(), "Parada registrada ✓", Snackbar.LENGTH_SHORT).show();
    }

    // Click en "Finalizar tour"
    private void onFinishTourClicked() {
        if (!tourStarted) {
            Snackbar.make(b.getRoot(), "No has iniciado el tour.", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (stopCount == 0) {
            Snackbar.make(b.getRoot(), "Registra al menos una parada antes de finalizar.", Snackbar.LENGTH_LONG).show();
            return;
        }

        tourFinished = true;
        b.btnRegisterLocation.setEnabled(false);
        b.btnNextStop.setEnabled(false);
        b.btnFinishTour.setEnabled(false);

        if (guideDocId != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("tourTrackingStatus", "FINISHED");
            map.put("lastTourStops", stopCount);
            map.put("lastTourFinishedAt", System.currentTimeMillis());
            db.collection("guias").document(guideDocId).update(map);
        }

        Snackbar.make(b.getRoot(), "Tour finalizado. ¡Buen trabajo!", Snackbar.LENGTH_LONG).show();
    }

    private void updateStopCounter() {
        b.txtStopCounter.setText("Paradas registradas: " + stopCount);
    }

    // ===================================
    //   PERMISOS + CICLO DE VIDA
    // ===================================

    private void startUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fused.requestLocationUpdates(request, callback, getMainLooper());
    }

    private void stopUpdates() {
        fused.removeLocationUpdates(callback);
    }

    private void checkPerm() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
        } else {
            startUpdates();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkPerm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPerm();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpdates();
            } else {
                Snackbar.make(b.getRoot(),
                        "Se requiere el permiso de ubicación para el seguimiento.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}