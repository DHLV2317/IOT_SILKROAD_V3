package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.CardFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethodsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<CardFB> cards = new ArrayList<>();
    private CardAdapter adapter;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tus métodos de pago");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvCards);
        Button btnAddCard = findViewById(R.id.btnAddCard);

        db = FirebaseFirestore.getInstance();

        adapter = new CardAdapter(cards, card ->
                Toast.makeText(this, "Seleccionaste " + card.getAlias(), Toast.LENGTH_SHORT).show()
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnAddCard.setOnClickListener(v -> {
            Intent i = new Intent(this, AddCardActivity.class);
            startActivity(i);
        });

        User u = UserStore.get().getLogged();
        if (u == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userEmail = u.getEmail();

        cargarTarjetas();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando tarjetas", Toast.LENGTH_SHORT).show());
    }
}