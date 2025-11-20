package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddCardActivity extends AppCompatActivity {

    private EditText etNumber, etName, etLastName, etExpiry, etCvv, etAlias;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Agregar tarjeta");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        etNumber   = findViewById(R.id.etCardNumber);
        etName     = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etExpiry   = findViewById(R.id.etExpiry);
        etCvv      = findViewById(R.id.etCvv);
        etAlias    = findViewById(R.id.etAlias);

        Button btnSave = findViewById(R.id.btnSaveCard);

        btnSave.setOnClickListener(v -> guardarTarjeta());
    }

    private void guardarTarjeta() {
        User u = UserStore.get().getLogged();
        if (u == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        String number   = etNumber.getText().toString().replace(" ", "");
        String name     = etName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String expiry   = etExpiry.getText().toString().trim();
        String cvv      = etCvv.getText().toString().trim();
        String alias    = etAlias.getText().toString().trim();

        if (TextUtils.isEmpty(number) || number.length() < 12) {
            Toast.makeText(this, "Número de tarjeta inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, "Completa nombre y apellido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(expiry)) {
            Toast.makeText(this, "Ingresa la fecha de vencimiento", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(cvv)) {
            Toast.makeText(this, "Ingresa el CVV", Toast.LENGTH_SHORT).show();
            return;
        }

        String last4 = number.substring(number.length() - 4);

        // Detección simple de marca
        String brand;
        if (number.startsWith("4"))      brand = "VISA";
        else if (number.startsWith("5")) brand = "MASTERCARD";
        else if (number.startsWith("3")) brand = "AMEX";
        else                             brand = "CARD";

        if (alias.isEmpty()) {
            alias = brand + " •••• " + last4;
        }

        // Saldo aleatorio 500–5000
        Random r = new Random();
        double balance = 500 + (r.nextDouble() * 4500);
        balance = Double.parseDouble(new DecimalFormat("#0.00").format(balance).replace(",", "."));

        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", u.getEmail());
        data.put("alias", alias);
        data.put("last4", last4);
        data.put("brand", brand);
        data.put("type", "CREDIT"); // simulación
        data.put("balance", balance);
        data.put("currency", "PEN");

        double finalBalance = balance;
        FirebaseFirestore.getInstance()
                .collection("cards")
                .add(data)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this,
                            "Tarjeta agregada con saldo simulado S/. " + finalBalance,
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al guardar tarjeta: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}