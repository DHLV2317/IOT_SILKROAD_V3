package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.CardFB;
import com.example.silkroad_iot.data.ServiceFB;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityPaymentBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PaymentActivity extends AppCompatActivity {

    private ActivityPaymentBinding b;
    private FirebaseFirestore db;

    private final List<CardFB> cards = new ArrayList<>();
    private CardAdapter cardAdapter;
    private CardFB selectedCard;

    private TourFB tour;
    private int pax;
    private double totalBase;     // base = precio * pax
    private double totalToCharge; // base + extras

    private final DecimalFormat moneyFormat = new DecimalFormat("#0.00");
    private String userEmail;

    // Día elegido
    private long selectedDateMillis = -1L;

    // ===== Extras =====
    private static class ExtraSelection {
        ServiceFB service;
        int quantity; // 0..pax
    }
    private final List<ExtraSelection> extraSelections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pago");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        tour      = (TourFB) getIntent().getSerializableExtra("tour");
        pax       = getIntent().getIntExtra("pax", 1);
        totalBase = getIntent().getDoubleExtra("totalBase", 0.0);
        selectedDateMillis = getIntent().getLongExtra("selectedDate", -1L);

        if (tour == null) {
            Toast.makeText(this, "Tour inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        User u = UserStore.get().getLogged();
        if (u == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userEmail = u.getEmail();

        b.tResumen.setText(
                "Vas a reservar: " + tour.getDisplayName() +
                        "\nPersonas: " + pax
        );
        totalToCharge = totalBase;
        updateMontoLabel();

        // Recycler de tarjetas
        b.rvTarjetas.setLayoutManager(new LinearLayoutManager(this));
        cardAdapter = new CardAdapter(cards, card -> selectedCard = card);
        b.rvTarjetas.setAdapter(cardAdapter);

        // Cargar tarjetas del usuario
        cargarTarjetas();

        // Extras (servicios adicionales)
        setupExtrasUI();

        // Ir a agregar tarjeta
        b.btnAddCard.setOnClickListener(v -> {
            Intent i = new Intent(this, AddCardActivity.class);
            startActivity(i);
        });

        // Botón Pagar
        b.btnPagar.setOnClickListener(v -> {
            if (selectedCard == null) {
                Toast.makeText(this, "Selecciona una tarjeta", Toast.LENGTH_SHORT).show();
                return;
            }
            if (totalToCharge <= 0) {
                Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            realizarPagoConTransaccion(UserStore.get().getLogged(),
                    selectedCard, tour, pax, totalToCharge);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar tarjetas por si el usuario agregó una nueva
        if (userEmail != null) {
            cargarTarjetas();
        }
    }

    private void cargarTarjetas() {
        db.collection("cards")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(q -> {
                    cards.clear();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        CardFB c = doc.toObject(CardFB.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            cards.add(c);
                        }
                    }
                    cardAdapter.notifyDataSetChanged();

                    if (cards.isEmpty()) {
                        Toast.makeText(this,
                                "No tienes tarjetas registradas. Agrega una para continuar.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando tarjetas", Toast.LENGTH_SHORT).show());
    }

    // ================= EXTRAS ==================

    private void setupExtrasUI() {
        List<ServiceFB> services = tour.getServices();
        if (services == null || services.isEmpty()) {
            b.tExtrasTitle.setVisibility(View.GONE);
            b.containerExtras.setVisibility(View.GONE);
            return;
        }

        // Filtramos solo los que tienen precio (>0) y son opcionales
        List<ServiceFB> extrasPagos = new ArrayList<>();
        for (ServiceFB s : services) {
            double p = s.getPricePerPersonSafe();
            if (!s.isIncludedSafe() && p > 0) {
                extrasPagos.add(s);
            }
        }

        if (extrasPagos.isEmpty()) {
            b.tExtrasTitle.setVisibility(View.GONE);
            b.containerExtras.setVisibility(View.GONE);
            return;
        }

        b.tExtrasTitle.setVisibility(View.VISIBLE);
        b.containerExtras.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        extraSelections.clear();
        b.containerExtras.removeAllViews();

        for (ServiceFB s : extrasPagos) {
            View itemView = inflater.inflate(R.layout.item_extra_selector, b.containerExtras, false);

            TextView tName  = itemView.findViewById(R.id.tExtraName);
            TextView tPrice = itemView.findViewById(R.id.tExtraPrice);
            TextView tQty   = itemView.findViewById(R.id.tExtraQty);
            Button btnMinus = itemView.findViewById(R.id.btnMinusExtra);
            Button btnPlus  = itemView.findViewById(R.id.btnPlusExtra);

            String name = s.getDisplayName();
            double price = s.getPricePerPersonSafe();

            tName.setText(name);
            tPrice.setText("S/. " + moneyFormat.format(price) + " por persona");

            ExtraSelection sel = new ExtraSelection();
            sel.service = s;
            sel.quantity = 0;
            extraSelections.add(sel);

            tQty.setText(String.valueOf(sel.quantity));

            btnMinus.setOnClickListener(v -> {
                if (sel.quantity > 0) {
                    sel.quantity--;
                    tQty.setText(String.valueOf(sel.quantity));
                    recalcTotal();
                }
            });

            btnPlus.setOnClickListener(v -> {
                if (sel.quantity < pax) { // máximo una unidad por persona
                    sel.quantity++;
                    tQty.setText(String.valueOf(sel.quantity));
                    recalcTotal();
                } else {
                    Toast.makeText(this,
                            "Solo puedes agregar hasta " + pax + " unidades de este adicional",
                            Toast.LENGTH_SHORT).show();
                }
            });

            b.containerExtras.addView(itemView);
        }
    }

    private void recalcTotal() {
        double extrasTotal = 0.0;
        for (ExtraSelection sel : extraSelections) {
            double p = sel.service.getPricePerPersonSafe();
            extrasTotal += p * sel.quantity;
        }
        totalToCharge = totalBase + extrasTotal;
        updateMontoLabel();
    }

    private void updateMontoLabel() {
        b.tMonto.setText("Total a pagar: S/. " + moneyFormat.format(totalToCharge));
    }

    // ================= PAGO CON TRANSACCIÓN ==================

    private void realizarPagoConTransaccion(User user, CardFB card,
                                            TourFB tour, int pax, double total) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference cardRef = db.collection("cards").document(card.getId());
        DocumentReference tourRef = db.collection("tours").document(tour.getId());
        DocumentReference historyRef = db.collection("tours_history").document();

        db.runTransaction(transaction -> {
            // 1) Leer tarjeta
            DocumentSnapshot cardSnap = transaction.get(cardRef);
            CardFB c = cardSnap.toObject(CardFB.class);
            if (c == null) {
                throw new FirebaseFirestoreException("Tarjeta no encontrada",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            double saldoActual = c.getBalance();
            if (saldoActual < total) {
                throw new FirebaseFirestoreException("Saldo insuficiente",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // 2) Leer tour para cupos (soporta keys legacy)
            DocumentSnapshot tourSnap = transaction.get(tourRef);

            Integer cuposDisp = null;

            if (tourSnap.getLong("cupos_disponibles") != null) {
                cuposDisp = tourSnap.getLong("cupos_disponibles").intValue();
            } else if (tourSnap.getLong("cuposDisponibles") != null) {
                cuposDisp = tourSnap.getLong("cuposDisponibles").intValue();
            } else if (tourSnap.getLong("capacidadTotal") != null) {
                cuposDisp = tourSnap.getLong("capacidadTotal").intValue();
            }

            if (cuposDisp == null) {
                cuposDisp = tour.getCuposDisponiblesSafe();
            }

            if (cuposDisp < pax) {
                throw new FirebaseFirestoreException("No hay cupos suficientes",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // 3) Actualizar saldo y cupos
            double nuevoSaldo = saldoActual - total;
            int nuevosCupos = cuposDisp - pax;

            transaction.update(cardRef, "balance", nuevoSaldo);

            HashMap<String, Object> tourUpdates = new HashMap<>();
            tourUpdates.put("cupos_disponibles", nuevosCupos);
            tourUpdates.put("cupos_totales", tour.getCuposTotalesSafe());
            transaction.update(tourRef, tourUpdates);

            // 4) Crear historial con estado "aceptado"
            Date ahora = new Date();
            String estado = "aceptado";

            String qrData = "RESERVA|" +
                    historyRef.getId() + "|" +
                    tour.getId() + "|" +
                    user.getEmail() + "|PAX:" + pax;

            HashMap<String, Object> histMap = new HashMap<>();
            histMap.put("id_tour", tour.getId());
            histMap.put("id_usuario", user.getEmail());
            histMap.put("fechaReserva", ahora);
            histMap.put("pax", pax);
            histMap.put("estado", estado);
            histMap.put("qrData", qrData);
            histMap.put("totalPagado", total);

            if (tour.getDateFrom() != null) {
                histMap.put("fecha_realizado", tour.getDateFrom());
            }
            if (selectedDateMillis > 0) {
                histMap.put("fechaElegida", new Date(selectedDateMillis));
            }

            // Guardamos también el detalle de extras seleccionados
            List<HashMap<String, Object>> extrasHist = new ArrayList<>();
            for (ExtraSelection sel : extraSelections) {
                if (sel.quantity <= 0) continue;
                HashMap<String, Object> ex = new HashMap<>();
                ex.put("nombre", sel.service.getDisplayName());
                ex.put("cantidad", sel.quantity);
                ex.put("precioPorPersona", sel.service.getPricePerPersonSafe());
                extrasHist.add(ex);
            }
            if (!extrasHist.isEmpty()) {
                histMap.put("extras", extrasHist);
            }

            transaction.set(historyRef, histMap);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this,
                    "Pago realizado y reserva aceptada 🎉",
                    Toast.LENGTH_LONG).show();

            // Volver a la lista de tours del cliente
            Intent i = new Intent(this, ClientHomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }
}