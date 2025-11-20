package com.example.silkroad_iot.ui.guide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideQrScannerBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Escáner REAL de QR para reservas.
 * No dependemos de reservaId en el texto.
 * Buscamos directamente en la colección "tours_history" por el campo "qrData".
 *
 * Además:
 *  - El guía solo puede escanear reservas del tour que tiene asignado (tourIdAsignado).
 */
public class GuideQRScannerActivity extends AppCompatActivity {

    private ActivityGuideQrScannerBinding b;
    private static final int CAM_REQ = 1002;

    private FirebaseFirestore db;
    private String guideDocId;

    // 🔹 Tour asignado al guía
    private String guideTourId;     // ID real del tour
    private String guideTourName;   // nombre del tour actual

    // CameraX + ML Kit
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean isProcessingFrame = false;
    private long lastScanTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuideQrScannerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Escanear QR de reserva");
        }

        db = FirebaseFirestore.getInstance();

        // Configuración ML Kit: solo QR
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

        resolveGuideDocId();
        checkCameraPermission();
    }

    // -------------------------------------------------------------
    //  Permisos de cámara
    // -------------------------------------------------------------

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAM_REQ);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAM_REQ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                b.txtInstructions.setText("Permiso de cámara denegado. No se puede escanear QR.");
                Snackbar.make(b.getRoot(), "Permiso de cámara necesario", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    // -------------------------------------------------------------
    //  CameraX + análisis de frames
    // -------------------------------------------------------------

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(b.cameraPreview.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Snackbar.make(b.getRoot(),
                        "Error iniciando cámara: " + e.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        if (isProcessingFrame) {
            imageProxy.close();
            return;
        }
        isProcessingFrame = true;

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes != null && !barcodes.isEmpty()) {
                        long now = System.currentTimeMillis();
                        // Evita procesar demasiadas veces el mismo código
                        if (now - lastScanTime > 2000) {
                            lastScanTime = now;
                            Barcode barcode = barcodes.get(0);
                            String raw = barcode.getRawValue();
                            if (raw != null) {
                                runOnUiThread(() -> onQrDecoded(raw));
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Snackbar.make(b.getRoot(), "Error leyendo QR", Snackbar.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    isProcessingFrame = false;
                    imageProxy.close();
                });
    }

    // -------------------------------------------------------------
    //  Lógica de negocio del QR
    // -------------------------------------------------------------

    private void onQrDecoded(String text) {
        // 🔹 Validar que el guía tenga un tour asignado
        if (guideTourId == null || guideTourId.trim().isEmpty()) {
            showMessage("No tienes un tour asignado. Pide al administrador que te asigne uno.");
            return;
        }

        if (text == null) text = "";
        text = text.trim();

        // Mostrar SIEMPRE el texto crudo para depurar
        b.txtScanResult.setText("Texto QR leído:\n" + text);

        if (text.isEmpty()) {
            b.txtInstructions.setText("QR inválido");
            Snackbar.make(b.getRoot(), "QR inválido", Snackbar.LENGTH_LONG).show();
            return;
        }

        // Buscar en tours_history por el campo qrData
        db.collection("tours_history")
                .whereEqualTo("qrData", text)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        b.txtInstructions.setText("QR inválido o reserva no encontrada.");
                        Snackbar.make(b.getRoot(), "QR inválido o reserva no encontrada", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);

                    String reservaDocId = doc.getId(); // ID real de la reserva
                    String estadoActual = doc.getString("estado");

                    // 🔹 id_tour / idTour (por compatibilidad)
                    String tourId = doc.contains("id_tour")
                            ? doc.getString("id_tour")
                            : doc.getString("idTour");

                    String userId = doc.contains("id_usuario")
                            ? doc.getString("id_usuario")
                            : doc.getString("idUsuario");

                    Long paxLong = doc.getLong("pax");
                    int pax = (paxLong == null ? 1 : paxLong.intValue());

                    // Mostrar info de la reserva encontrada
                    b.txtScanResult.append(
                            "\n\nReserva encontrada:" +
                                    "\nDocId: " + reservaDocId +
                                    "\nTour: " + (tourId == null ? "-" : tourId) +
                                    "\nCliente: " + (userId == null ? "-" : userId) +
                                    "\nPAX: " + pax
                    );

                    updateReservationState(reservaDocId, estadoActual, tourId, userId, pax);
                })
                .addOnFailureListener(e -> {
                    b.txtInstructions.setText("Error buscando la reserva: " + e.getMessage());
                    Snackbar.make(b.getRoot(), "Error buscando la reserva", Snackbar.LENGTH_LONG).show();
                });
    }

    /**
     * Flujo B:
     *   pendiente  -> NO cambia (debe aceptar admin)
     *   aceptado   -> check-in
     *   check-in   -> check-out
     *   check-out  -> finalizada
     *   finalizada / cancelado / rechazado -> NO cambia
     *
     * Además:
     *   - Solo permite cambiar estado si la reserva corresponde al tour asignado al guía.
     */
    private void updateReservationState(String reservaDocId,
                                        String estadoActual,
                                        String tourId,
                                        String userId,
                                        int pax) {

        // 🔹 Validar que el QR sea del tour asignado al guía
        if (tourId == null || !tourId.equals(guideTourId)) {
            String nombreTour = (guideTourName == null || guideTourName.isEmpty())
                    ? "tu tour asignado"
                    : guideTourName;

            showMessage("Este código QR pertenece a otro tour.\n\n" +
                    "Solo puedes registrar check-in/check-out para: " + nombreTour);
            return;
        }

        if (estadoActual == null) estadoActual = "pendiente";
        String estadoLower = estadoActual.toLowerCase();
        String nuevoEstado = null;
        String msg;

        switch (estadoLower) {
            case "pendiente":
                msg = "La reserva aún está pendiente. "
                        + "Primero debe ser aceptada por el administrador.";
                break;
            case "aceptado":
            case "aceptada":
                nuevoEstado = "check-in";
                msg = "✅ Check-in registrado correctamente.";
                break;
            case "check-in":
            case "checkin":
                nuevoEstado = "check-out";
                msg = "✅ Check-out registrado correctamente.";
                break;
            case "check-out":
            case "checkout":
                nuevoEstado = "finalizada";
                msg = "🎉 Servicio finalizado. ¡Gracias!";
                break;
            case "finalizada":
                msg = "Esta reserva ya está finalizada. No se puede modificar.";
                break;
            case "cancelado":
            case "cancelada":
            case "rechazado":
            case "rechazada":
                msg = "Esta reserva está " + estadoActual + " y no puede modificarse.";
                break;
            default:
                msg = "Estado actual: " + estadoActual + ". No se modifica.";
                break;
        }

        if (nuevoEstado == null) {
            showMessage(msg);
            return;
        }

        final String finalNuevoEstado = nuevoEstado;
        db.collection("tours_history")
                .document(reservaDocId)
                .update("estado", finalNuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    showMessage(msg + " (Nuevo estado: " + finalNuevoEstado + ")");
                    logGuideCheck(reservaDocId, tourId, userId, pax, finalNuevoEstado);
                })
                .addOnFailureListener(e ->
                        showMessage("Error al actualizar estado: " + e.getMessage()));
    }

    private void showMessage(String msg) {
        b.txtInstructions.setText(msg);
        Snackbar.make(b.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    private void logGuideCheck(String reservaDocId,
                               String tourId,
                               String userId,
                               int pax,
                               String nuevoEstado) {
        if (guideDocId == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("reservaId", reservaDocId);
        log.put("tourId", tourId);
        log.put("userId", userId);
        log.put("pax", pax);
        log.put("nuevoEstado", nuevoEstado);
        log.put("timestamp", System.currentTimeMillis());

        db.collection("guias").document(guideDocId)
                .collection("checkins")
                .add(log);
    }

    // ----------------- Resolución del documento del guía -----------------

    private void resolveGuideDocId() {
        User u = UserStore.get().getLogged();
        String email = (u != null ? u.getEmail() : null);
        if (email == null || email.isEmpty()) return;

        db.collection("guias").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        bindGuideDoc(snap.getDocuments().get(0));
                    } else {
                        // Fallback por si guardaste 'correo'
                        db.collection("guias").whereEqualTo("correo", email)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    if (!snap2.isEmpty()) {
                                        bindGuideDoc(snap2.getDocuments().get(0));
                                    }
                                });
                    }
                });
    }

    private void bindGuideDoc(DocumentSnapshot d) {
        guideDocId    = d.getId();
        guideTourName = d.getString("tourActual");
        guideTourId   = d.getString("tourIdAsignado");
    }

    // ----------------- Toolbar back & cleanup -----------------

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}