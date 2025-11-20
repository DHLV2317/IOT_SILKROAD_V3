package com.example.silkroad_iot.ui.superadmin;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ActivitySuperAdminHomeBinding;
import com.example.silkroad_iot.databinding.ActivitySuperadminAdministradoresBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class SuperAdminHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private static final String TAG="SuperAdminHomeActivity";

    private ActivitySuperAdminHomeBinding binding;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> cambiarContrasenia());

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_inicio) { /* aquí */ }
        else if (id == R.id.nav_administradores) startActivity(new Intent(this, AdministradoresActivity.class));
        else if (id == R.id.nav_solicitudes_guias) startActivity(new Intent(this, SolicitudesGuiasActivity.class));
        else if (id == R.id.nav_guias) startActivity(new Intent(this, GuiasActivity.class));
        else if (id == R.id.nav_clientes) startActivity(new Intent(this, ClientesActivity.class));
        else if (id == R.id.nav_reportes) startActivity(new Intent(this, ReportesActivity.class));
        else if (id == R.id.nav_logs) startActivity(new Intent(this, LogsActivity.class));
        else if (id == R.id.nav_cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void cambiarContrasenia() {
        // Obtener la instancia del usuario de Firebase Authentication
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = "";
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            String uid = user.getUid();
        }

        // Verificación de seguridad: si por alguna razón no hay un usuario logueado, no continuar.
        if (user == null) {
            Toast.makeText(this, "No se ha iniciado sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        TextInputLayout contraseniaActual = findViewById(R.id.textInputLayout2);
        String contraseniaActualString = contraseniaActual.getEditText().getText().toString().trim();

        TextInputLayout contraseniaNueva = findViewById(R.id.textInputLayout4);
        String contraseniaNuevaString = contraseniaNueva.getEditText().getText().toString().trim();



        if(contraseniaActualString.isEmpty()){
            Toast.makeText(this, "Introduce tu contraseña actual.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(contraseniaNuevaString.isEmpty()){
            Toast.makeText(this, "Introduce tu nueva contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }else if(contraseniaNuevaString.length() < 6){
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(email!=null){
            AuthCredential credential = EmailAuthProvider
                    .getCredential(email, contraseniaActualString);
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "User re-authenticated.");
                        }

                    });

            user.updatePassword(contraseniaNuevaString)
                    .addOnSuccessListener(aVoid -> {
                        // Éxito
                        Toast.makeText(SuperAdminHomeActivity.this, "Contraseña actualizada correctamente.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Fallo
                        Log.e("CambiarContrasenia", "Error al actualizar", e);
                        Toast.makeText(SuperAdminHomeActivity.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(SuperAdminHomeActivity.this, SuperAdminHomeActivity.class);
                        startActivity(intent);
                        finish();
                        // NOTA: Esta operación es sensible. Si falla con un error de "requiere inicio de sesión reciente",
                        // se necesita un flujo de reautenticación más complejo.
                    });
        }
    }
}