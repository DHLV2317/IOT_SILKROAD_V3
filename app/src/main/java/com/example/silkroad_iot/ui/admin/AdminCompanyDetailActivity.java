package com.example.silkroad_iot.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.databinding.ActivityAdminCompanyDetailBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminCompanyDetailActivity extends AppCompatActivity {

    private ActivityAdminCompanyDetailBinding b;
    private FirebaseFirestore db;
    private EmpresaFb empresa;
    private boolean firstRun;

    // Preferencias
    private static final String PREFS = "app_prefs";
    private static final String KEY_COMPANY_DONE = "admin_company_done";
    // 🔗 Usado también en AdminReservationsActivity para filtrar por empresa
    private static final String KEY_EMPRESA_ID   = "empresa_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAdminCompanyDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Empresa");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        firstRun = getIntent().getBooleanExtra("firstRun", false);

        String empresaId = getIntent().getStringExtra("id");

        if (!TextUtils.isEmpty(empresaId)) {
            cargarEmpresaDesdeFirestore(empresaId);
        } else {
            empresa = new EmpresaFb();
            Glide.with(this).load(R.drawable.ic_image_24).into(b.imgLogo);
        }

        b.btnSaveCompany.setOnClickListener(v -> guardarEmpresaEnFirestore());
        b.btnChangePhoto.setOnClickListener(v ->
                Toast.makeText(this, "Implementar selector de imágenes", Toast.LENGTH_SHORT).show()
        );
    }

    private void cargarEmpresaDesdeFirestore(String id) {
        db.collection("empresas").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        empresa = doc.toObject(EmpresaFb.class);
                        if (empresa != null) empresa.setId(id);
                        rellenarCampos();
                    } else {
                        empresa = new EmpresaFb();
                        Toast.makeText(this, "Empresa no encontrada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void rellenarCampos() {
        b.inputCompanyName.setText(n(empresa.getNombre()));
        b.inputEmail.setText(n(empresa.getEmail()));
        b.inputPhone.setText(n(empresa.getTelefono()));
        b.inputAddress.setText(n(empresa.getDireccion()));
        b.inputLat.setText(String.valueOf(empresa.getLat()));
        b.inputLng.setText(String.valueOf(empresa.getLng()));

        b.txtCompanyTitle.setText(
                n(empresa.getNombre()).isEmpty() ? "Nombre de la empresa" : empresa.getNombre()
        );

        Glide.with(this)
                .load(n(empresa.getImagen()))
                .placeholder(R.drawable.ic_image_24)
                .error(R.drawable.ic_image_24)
                .centerCrop()
                .into(b.imgLogo);
    }

    private void guardarEmpresaEnFirestore() {
        String name = b.inputCompanyName.getText().toString().trim();
        String email = b.inputEmail.getText().toString().trim();
        String phone = b.inputPhone.getText().toString().trim();
        String address = b.inputAddress.getText().toString().trim();

        if (name.isEmpty()) { b.inputCompanyName.setError("Requerido"); return; }
        if (email.isEmpty()) { b.inputEmail.setError("Requerido"); return; }
        if (phone.isEmpty()) { b.inputPhone.setError("Requerido"); return; }
        if (address.isEmpty()) { b.inputAddress.setError("Requerido"); return; }

        double lat = pDouble(b.inputLat.getText().toString().trim());
        double lng = pDouble(b.inputLng.getText().toString().trim());

        empresa.setNombre(name);
        empresa.setEmail(email);
        empresa.setTelefono(phone);
        empresa.setDireccion(address);
        empresa.setLat(lat);
        empresa.setLng(lng);

        if (!TextUtils.isEmpty(empresa.getId())) {
            db.collection("empresas")
                    .document(empresa.getId())
                    .set(empresa)
                    .addOnSuccessListener(aVoid -> onSaveSuccess("Empresa actualizada"))
                    .addOnFailureListener(e -> showError(e.getMessage()));
        } else {
            DocumentReference ref = db.collection("empresas").document();
            empresa.setId(ref.getId());
            ref.set(empresa)
                    .addOnSuccessListener(aVoid -> onSaveSuccess("Empresa registrada"))
                    .addOnFailureListener(e -> showError(e.getMessage()));
        }
    }

    private void onSaveSuccess(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        // ✅ Guardamos tanto que ya configuró empresa como el ID de la empresa
        var editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_COMPANY_DONE, true);
        if (!TextUtils.isEmpty(empresa.getId())) {
            editor.putString(KEY_EMPRESA_ID, empresa.getId());
        }
        editor.apply();

        if (firstRun) {
            startActivity(new Intent(this, AdminToursActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finishAffinity();
        } else {
            finish();
        }
    }

    private void showError(String m) { Toast.makeText(this, "Error: " + m, Toast.LENGTH_LONG).show(); }
    private double pDouble(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0d; } }
    private String n(String s) { return s == null ? "" : s; }
}