package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;

import java.util.ArrayList;
import java.util.List;

public class SupportChatActivity extends AppCompatActivity {

    private final List<String> messages = new ArrayList<>();
    private SimpleTextAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat de soporte");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        RecyclerView rv      = findViewById(R.id.rvMessages);
        EditText input       = findViewById(R.id.inputMessage);
        ImageButton btnSend  = findViewById(R.id.btnSend);

        // Adapter
        adapter = new SimpleTextAdapter(messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Mensaje inicial
        messages.add("👋 Hola, ¿en qué podemos ayudarte?");
        adapter.notifyDataSetChanged();

        // Envío de mensajes
        btnSend.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) return;

            messages.add("Tú: " + txt);
            messages.add("Soporte: Gracias por tu mensaje, pronto te contactaremos.");
            adapter.notifyDataSetChanged();
            rv.scrollToPosition(messages.size() - 1);
            input.setText("");
        });
    }
}