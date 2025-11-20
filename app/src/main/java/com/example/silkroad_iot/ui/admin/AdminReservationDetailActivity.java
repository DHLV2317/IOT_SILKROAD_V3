package com.example.silkroad_iot.ui.admin;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ReservaWithTour;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.TourHistorialFB;
import com.example.silkroad_iot.databinding.ActivityAdminReservationDetailBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminReservationDetailActivity extends AppCompatActivity {

    private ActivityAdminReservationDetailBinding b;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private FirebaseFirestore db;

    private TourHistorialFB reserva;
    private TourFB tour;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAdminReservationDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle de reserva");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        ReservaWithTour item = (ReservaWithTour) getIntent().getSerializableExtra("reserva");
        if (item == null) {
            finish();
            return;
        }

        reserva = item.getReserva();
        tour    = item.getTour();

        bindData();
        setupButtons();
    }

    private void bindData() {
        String tourName   = (tour != null ? tour.getDisplayName() : "(Sin tour)");
        double precioUnit = (tour != null ? tour.getDisplayPrice() : 0.0);

        int pax = reserva.getPax() > 0
                ? reserva.getPax()
                : (tour != null ? tour.getDisplayPeople() : 1);
        if (pax <= 0) pax = 1;

        String clientId = reserva.getIdUsuario() == null ? "—" : reserva.getIdUsuario();
        String status   = reserva.getEstado();
        if (TextUtils.isEmpty(status)) status = "pendiente";

        Date date = reserva.getFechaReserva() != null
                ? reserva.getFechaReserva()
                : reserva.getFechaRealizado();

        b.tTourName.setText(tourName);
        b.tDate.setText(date == null ? "—" : sdf.format(date));
        b.tAmount.setText("S/ " + (precioUnit * pax));
        b.tStatus.setText(status);

        b.tUser.setText(clientId);
        b.tEmail.setText(clientId);
        b.tPhone.setText("—");
        b.tDni.setText("—");

        pintarEstado(status);

        // QR
        String qrData = reserva.getQrData();
        if ((qrData == null || qrData.isEmpty())
                && reserva.getId() != null && !reserva.getId().isEmpty()) {

            qrData = "RESERVA|" +
                    reserva.getId() + "|" +
                    reserva.getIdTour() + "|" +
                    reserva.getIdUsuario() + "|PAX:" + pax;

            reserva.setQrData(qrData);
            reserva.setPax(pax);

            db.collection("tours_history")
                    .document(reserva.getId())
                    .update("qrData", qrData, "pax", pax);
        }

        if (qrData != null && !qrData.isEmpty()) {
            Bitmap bmp = makeQr(qrData);
            if (bmp != null) b.imgQr.setImageBitmap(bmp);
        } else {
            b.imgQr.setImageResource(R.drawable.qr_code_24);
        }

        b.tQrMessage.setText("Muestra este QR en el punto de encuentro para hacer check-in.");

        // Rating
        if (reserva.getRating() != null) {
            b.cardRating.setVisibility(View.VISIBLE);
            RatingBar rb = b.tRating;
            if (rb != null) rb.setRating(reserva.getRating());

            String comment = reserva.getComentario();
            b.tRatingComment.setText(
                    (comment == null || comment.trim().isEmpty())
                            ? "Sin comentario."
                            : comment
            );
        } else {
            b.cardRating.setVisibility(View.GONE);
        }

        String st = status.toLowerCase(Locale.getDefault());
        if (st.contains("acept") || st.contains("final")
                || st.contains("cancel") || st.contains("rech")) {
            b.btnAccept.setVisibility(View.GONE);
            b.btnReject.setVisibility(View.GONE);
        }

        b.btnBack.setOnClickListener(v -> finish());
    }

    private void setupButtons() {
        b.btnAccept.setOnClickListener(v -> cambiarEstado("aceptado"));
        b.btnReject.setOnClickListener(v -> cambiarEstado("rechazado"));
    }

    private void cambiarEstado(String nuevoEstado) {
        if (reserva == null || reserva.getId() == null || reserva.getId().isEmpty()) {
            Toast.makeText(this, "Reserva sin ID, no se puede actualizar", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tours_history")
                .document(reserva.getId())
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    reserva.setEstado(nuevoEstado);
                    b.tStatus.setText(nuevoEstado);
                    pintarEstado(nuevoEstado);
                    b.btnAccept.setVisibility(View.GONE);
                    b.btnReject.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Estado actualizado a " + nuevoEstado,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al actualizar estado",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void pintarEstado(String status) {
        int bg = R.color.pill_gray;
        if (status == null) {
            b.tStatus.setBackgroundResource(bg);
            return;
        }

        String st = status.toLowerCase(Locale.getDefault());
        if (st.contains("check-in") || st.contains("check-out")
                || st.contains("final") || st.contains("acept")) {
            bg = R.color.teal_200;
        } else if (st.contains("cancel") || st.contains("rech")) {
            bg = android.R.color.holo_red_light;
        }
        b.tStatus.setBackgroundResource(bg);
    }

    private Bitmap makeQr(String text) {
        try {
            QRCodeWriter w = new QRCodeWriter();
            int size = 512;
            BitMatrix bit = w.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, bit.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }
}