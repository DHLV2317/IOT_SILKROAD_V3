package com.example.silkroad_iot.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileActivity extends BaseDrawerActivity {

    private static final String PREFS = "app_prefs";
    private static final String KEY_ADMIN_NAME   = "admin_name";
    private static final String KEY_ADMIN_EMAIL  = "admin_email";
    private static final String KEY_ADMIN_PHOTO  = "admin_photo";
    private static final String KEY_PROFILE_DONE = "admin_profile_completed";

    // UI
    private ImageView imgProfilePhoto;
    private TextView txtProfileName;
    private TextView txtApprovalStatus;
    private TextView txtEmailHeader;

    private TextInputEditText inputNames;
    private TextInputEditText inputLastNames;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputAddress;
    private TextInputEditText inputLanguages;

    // Empresa
    private TextView txtCompanyName;
    private TextView txtCompanyEmail;
    private TextView txtCompanyPhone;
    private TextView txtCompanyAddress;

    // Stats
    private TextView txtTotalTours;
    private TextView txtTotalEarnings;
    private TextView txtAverageRating;
    private TextView txtRecentHistory;

    private MaterialButton btnSaveChanges;
    private MaterialButton btnEditCompany;
    private MaterialButton btnChangePassword;
    private MaterialButton btnLogout;
    private MaterialButton btnChangePhoto;

    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawer(R.layout.activity_admin_profile,
                R.menu.menu_drawer_admin,
                "Perfil");

        db    = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        bindViews();
        loadAdminAndCompany();
        setupClicks();
    }

    @Override
    protected int defaultMenuId() {
        return 0;
    }

    private void bindViews() {
        imgProfilePhoto   = findViewById(R.id.imgProfilePhoto);
        txtProfileName    = findViewById(R.id.txtProfileName);
        txtApprovalStatus = findViewById(R.id.txtApprovalStatus);
        txtEmailHeader    = findViewById(R.id.txtEmailHeader);

        inputNames     = findViewById(R.id.inputNames);
        inputLastNames = findViewById(R.id.inputLastNames);
        inputEmail     = findViewById(R.id.inputEmail);
        inputPhone     = findViewById(R.id.inputPhone);
        inputAddress   = findViewById(R.id.inputAddress);
        inputLanguages = findViewById(R.id.inputLanguages);

        txtCompanyName    = findViewById(R.id.txtCompanyName);
        txtCompanyEmail   = findViewById(R.id.txtCompanyEmail);
        txtCompanyPhone   = findViewById(R.id.txtCompanyPhone);
        txtCompanyAddress = findViewById(R.id.txtCompanyAddress);

        txtTotalTours    = findViewById(R.id.txtTotalTours);
        txtTotalEarnings = findViewById(R.id.txtTotalEarnings);
        txtAverageRating = findViewById(R.id.txtAverageRating);
        txtRecentHistory = findViewById(R.id.txtRecentHistory);

        btnSaveChanges    = findViewById(R.id.btnSaveChanges);
        btnEditCompany    = findViewById(R.id.btnEditCompany);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout         = findViewById(R.id.btnLogout);
        btnChangePhoto    = findViewById(R.id.btnChangePhoto);
    }

    private void loadAdminAndCompany() {
        // 1) Usuario logueado desde UserStore
        User u = UserStore.get().getLogged();

        // Valores por defecto antiguos (por si no hay UserStore cargado todavía)
        String storedName  = prefs.getString(KEY_ADMIN_NAME,  "Admin");
        String storedEmail = prefs.getString(KEY_ADMIN_EMAIL, "admin@demo.com");
        String storedPhoto = prefs.getString(KEY_ADMIN_PHOTO, "");

        // --- Nombre y email base ---
        String adminName  = (u != null && u.getName() != null && !u.getName().isEmpty())
                ? u.getName()
                : storedName;

        String adminEmail = (u != null && u.getEmail() != null && !u.getEmail().isEmpty())
                ? u.getEmail()
                : storedEmail;

        String adminLast  = (u != null && u.getLastName() != null) ? u.getLastName() : "";
        String adminPhone = (u != null && u.getPhone() != null) ? u.getPhone() : "";
        String adminAddr  = (u != null && u.getAddress() != null) ? u.getAddress() : "";
        String adminLangs = (u != null && u.getLanguages() != null) ? u.getLanguages() : "";

        // --- Foto: prioriza foto del User; si no, prefs; si no, ícono ---
        String adminPhotoUrl;
        if (u != null && u.getFotoUrl() != null && !u.getFotoUrl().isEmpty()) {
            adminPhotoUrl = u.getFotoUrl();
        } else if (u != null && u.getPhotoUri() != null && !u.getPhotoUri().isEmpty()) {
            adminPhotoUrl = u.getPhotoUri();
        } else if (!storedPhoto.isEmpty()) {
            adminPhotoUrl = storedPhoto;
        } else {
            adminPhotoUrl = null;
        }

        // 2) Poner datos en UI
        txtProfileName.setText(
                (adminLast == null || adminLast.isEmpty())
                        ? adminName
                        : adminName + " " + adminLast
        );
        txtEmailHeader.setText(adminEmail);

        inputNames.setText(adminName);
        inputLastNames.setText(adminLast);
        inputEmail.setText(adminEmail);
        inputPhone.setText(adminPhone);
        inputAddress.setText(adminAddr);
        inputLanguages.setText(adminLangs);

        Glide.with(this)
                .load(adminPhotoUrl == null || adminPhotoUrl.isEmpty()
                        ? R.drawable.ic_person_24
                        : adminPhotoUrl)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .into(imgProfilePhoto);

        // Estado inicial: si es la primera vez, pide completar perfil y cambiar contraseña
        boolean profileDone = prefs.getBoolean(KEY_PROFILE_DONE, false);
        if (!profileDone) {
            txtApprovalStatus.setText("Estado: ⚠️ Completa tu perfil y cambia tu contraseña");
        } else {
            txtApprovalStatus.setText("Estado: ✅ Perfil configurado");
        }

        // 3) Cargar Empresa desde Firestore si el admin tiene companyId/empresaId
        if (u != null && u.getEmpresaId() != null && !u.getEmpresaId().isEmpty()) {
            String empresaId = u.getEmpresaId();

            FirebaseFirestore.getInstance()
                    .collection("empresas")
                    .document(empresaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        EmpresaFb emp = doc.toObject(EmpresaFb.class);
                        if (emp == null) return;

                        txtCompanyName.setText(emp.getNombre() == null ? "—" : emp.getNombre());
                        txtCompanyEmail.setText(emp.getEmail() == null ? "—" : emp.getEmail());
                        txtCompanyPhone.setText(emp.getTelefono() == null ? "—" : emp.getTelefono());
                        txtCompanyAddress.setText(emp.getDireccion() == null ? "—" : emp.getDireccion());

                        // Si quieres que el header use el logo de la empresa cuando no haya foto del admin:
                        if (adminPhotoUrl == null || adminPhotoUrl.isEmpty()) {
                            String logo = emp.getDisplayLogo();
                            if (logo != null && !logo.isEmpty()) {
                                Glide.with(this)
                                        .load(logo)
                                        .placeholder(R.drawable.ic_person_24)
                                        .error(R.drawable.ic_person_24)
                                        .into(imgProfilePhoto);
                            }
                        }
                    });
        } else {
            txtCompanyName.setText("—");
            txtCompanyEmail.setText("—");
            txtCompanyPhone.setText("—");
            txtCompanyAddress.setText("—");
        }

        // 4) Estadísticas: por ahora placeholders hasta que tengas consultas reales
        txtTotalTours.setText("Tours publicados: —");
        txtTotalEarnings.setText("Ingresos totales: S/ —");
        txtAverageRating.setText("Calificación promedio: —");
        txtRecentHistory.setText("Aquí aparecerán los últimos tours de tu empresa.");
    }

    private void setupClicks() {
        // Guardar cambios de perfil (solo local + lo que tengas en UserStore)
        btnSaveChanges.setOnClickListener(v -> {
            String newName      = safeText(inputNames);
            String newLastName  = safeText(inputLastNames);
            String newPhone     = safeText(inputPhone);
            String newAddress   = safeText(inputAddress);
            String newLanguages = safeText(inputLanguages);
            String newEmail     = safeText(inputEmail);  // sigue en solo lectura, pero por si acaso

            // Actualizar encabezado
            txtProfileName.setText(
                    (newLastName.isEmpty())
                            ? newName
                            : newName + " " + newLastName
            );
            txtEmailHeader.setText(newEmail);

            // Guardar en SharedPreferences (modo offline/local)
            prefs.edit()
                    .putString(KEY_ADMIN_NAME, newName)
                    .putString(KEY_ADMIN_EMAIL, newEmail)
                    .putBoolean(KEY_PROFILE_DONE, true)
                    .apply();

            // Si tienes UserStore, actualiza también allí
            User u = UserStore.get().getLogged();
            if (u != null) {
                u.setName(newName);
                u.setLastName(newLastName);
                u.setPhone(newPhone);
                u.setAddress(newAddress);
                u.setLanguages(newLanguages);
                // TODO: si tienes colección "users" en Firestore, aquí haces update()
                UserStore.get().setLogged(u);
            }

            txtApprovalStatus.setText("Estado: ✅ Perfil configurado");

            Snackbar.make(findViewById(android.R.id.content),
                    "Perfil actualizado",
                    Snackbar.LENGTH_LONG).show();
        });

        // Editar ficha de empresa
        btnEditCompany.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminCompanyDetailActivity.class));
        });

        // Cambiar contraseña: primera vez o cuando quiera
        btnChangePassword.setOnClickListener(v -> {
            // Aquí puedes abrir una pantalla específica de cambio de contraseña
            // o mostrar un diálogo. Ejemplo simple con Activity dedicada:
            Intent i = new Intent(this, AdminChangePasswordActivity.class);
            startActivity(i);

            // Si luego de cambiar contraseña quieres marcar como completado:
            // prefs.edit().putBoolean(KEY_PROFILE_DONE, true).apply();
        });

        // Cambiar foto: puedes reutilizar la lógica que usaste en GuideProfileActivity,
        // aquí sólo dejamos el hook.
        btnChangePhoto.setOnClickListener(v -> {
            Snackbar.make(findViewById(android.R.id.content),
                    "Función de cambiar foto del admin pendiente de implementar",
                    Snackbar.LENGTH_SHORT).show();
        });

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            UserStore.get().logout();
            // Podrías redirigir al MainActivity o LoginActivity
            // finishAffinity() si quieres cerrar todo el stack.
            finishAffinity();
        });
    }

    private String safeText(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }
}