package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.databinding.ActivitySuperadminDetallesSolicitudGuiaBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetallesSolicitudGuiaActivity extends AppCompatActivity {

    private ActivitySuperadminDetallesSolicitudGuiaBinding binding;
    private String docId;
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetallesSolicitudGuiaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        User guia = (User) intent.getSerializableExtra("guia");
        if (guia == null) { Toast.makeText(this, "Guía no encontrado", Toast.LENGTH_SHORT).show(); finish(); return; }
        docId = guia.getEmail();

        binding.textInputLayout.getEditText().setText(guia.getName());
        binding.textInputLayout2.getEditText().setText(guia.getLastName());
        binding.textInputLayout3.getEditText().setText(guia.getDocumentType());
        binding.textInputLayout4.getEditText().setText(guia.getDocumentNumber());
        binding.textInputLayout5.getEditText().setText(guia.getBirthDate());
        binding.textInputLayout6.getEditText().setText(guia.getEmail());
        binding.textInputLayout7.getEditText().setText(guia.getPhone());
        binding.textInputLayout8.getEditText().setText(guia.getAddress());
        binding.textInputLayout9.getEditText().setText(guia.getLanguages());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.en.setOnClickListener(v -> setAprobado(true));
        binding.di.setOnClickListener(v -> setAprobado(false));
    }

    private void setAprobado(boolean aprobado){
        //db.collection("users").document(docId)
        db.collection("usuarios").document(docId)
                .update("guideApproved", aprobado,
                        "guideApprovalStatus", aprobado ? "APPROVED" : "REJECTED")
                .addOnSuccessListener(v -> { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}