package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.databinding.ActivityConfirmTourBinding;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConfirmTourActivity extends AppCompatActivity {

    private TourFB tour;
    private ActivityConfirmTourBinding b;

    private int selectedPax = 1;
    private int maxCupos = 1;
    private double unitPrice = 0.0;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final DecimalFormat moneyFormat =
            new DecimalFormat("#0.00");

    private Date selectedDate;       // día elegido
    private long selectedDateMillis; // para pasarlo por Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityConfirmTourBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Toolbar
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reservas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Obtener tour desde el intent
        tour = (TourFB) getIntent().getSerializableExtra("tour");
        if (tour == null) {
            Toast.makeText(this, "No se pudo cargar el tour", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Día seleccionado desde detalle (si lo hay)
        selectedDateMillis = getIntent().getLongExtra("selectedDate", -1);
        if (selectedDateMillis > 0) {
            selectedDate = new Date(selectedDateMillis);
        }

        // Precio unitario seguro
        unitPrice = tour.getDisplayPrice();
        if (unitPrice <= 0) {
            Toast.makeText(this, "Precio del tour no definido", Toast.LENGTH_SHORT).show();
        }

        // Cupos disponibles seguros
        maxCupos = tour.getCuposDisponiblesSafe();
        if (maxCupos <= 0) {
            // Fallback: usar capacidad declarada
            maxCupos = tour.getDisplayPeople();
        }
        if (maxCupos <= 0) {
            Toast.makeText(this, "Este tour no tiene cupos disponibles.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        selectedPax = 1;

        // Mostrar nombre del tour
        b.tTourName.setText(tour.getDisplayName());

        // Mostrar capacidad / cupos
        b.tTourPeople.setText("Cupos disponibles: " + maxCupos + " personas");

        // Mostrar fechas del tour (inicio - fin) y día elegido
        if (tour.getDateFrom() != null && tour.getDateTo() != null) {
            String inicio = sdf.format(tour.getDateFrom());
            String fin    = sdf.format(tour.getDateTo());
            String rango  = "Fechas: " + inicio + " hasta " + fin;
            if (selectedDate != null) {
                rango += "\nDía elegido: " + sdf.format(selectedDate);
            }
            b.tTourDates.setText(rango);
        } else {
            String txt = "Fechas: No definidas";
            if (selectedDate != null) {
                txt += "\nDía elegido: " + sdf.format(selectedDate);
            }
            b.tTourDates.setText(txt);
        }

        // Precio unitario visible
        b.tUnitPrice.setText("Precio por persona: S/. " + moneyFormat.format(unitPrice));

        // Inicializar selector de pax
        TextView tPaxValue = b.tPaxValue;
        Button btnMinus    = b.btnMinus;
        Button btnPlus     = b.btnPlus;

        tPaxValue.setText(String.valueOf(selectedPax));

        btnMinus.setOnClickListener(v -> {
            if (selectedPax > 1) {
                selectedPax--;
                tPaxValue.setText(String.valueOf(selectedPax));
                updateTotal();
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (selectedPax < maxCupos) {
                selectedPax++;
                tPaxValue.setText(String.valueOf(selectedPax));
                updateTotal();
            } else {
                Toast.makeText(this,
                        "No hay más cupos disponibles",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar total inicial
        updateTotal();

        // Confirmar -> ir a PaymentActivity
        b.btnConfirm.setOnClickListener(v -> {
            double totalBase = unitPrice * selectedPax;
            Intent i = new Intent(this, PaymentActivity.class);
            i.putExtra("tour", tour);
            i.putExtra("pax", selectedPax);     // cantidad de personas reservadas
            i.putExtra("totalBase", totalBase); // base sin adicionales
            if (selectedDateMillis > 0) {
                i.putExtra("selectedDate", selectedDateMillis);
            }
            startActivity(i);
        });
    }

    private void updateTotal() {
        double total = unitPrice * selectedPax;
        b.tTotal.setText("Total: S/. " + moneyFormat.format(total));
    }
}