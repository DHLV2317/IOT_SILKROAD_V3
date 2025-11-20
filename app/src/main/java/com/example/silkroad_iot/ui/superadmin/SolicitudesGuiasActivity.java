package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.databinding.ActivitySuperadminSolicitudesGuiasBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolicitudesGuiasActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ActivitySuperadminSolicitudesGuiasBinding binding;
    private final List<User> data = new ArrayList<>();
    private final List<String> userIds = new ArrayList<>();

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private ReqAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminSolicitudesGuiasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Solicitudes de guías");
        }

        // Drawer
        drawerLayout   = binding.drawerLayout;
        navigationView = binding.navView;
        navigationView.setNavigationItemSelectedListener(this);
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Recycler
        adapter = new ReqAdapter(data, position -> mostrarDialogoAcciones(position));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        // Firestore
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        cargarLista();
    }

    private void cargarLista() {
        data.clear();
        userIds.clear();
        adapter.notifyDataSetChanged();

        db.collection("usuarios")// colección "guias" o "usuarios(usando)"
                .whereEqualTo("rol", "GUIDE")                  // campo "rol" en tu BD
                .whereEqualTo("guideApprovalStatus", "PENDING") // solo pendientes
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        data.add(u);
                        userIds.add(d.getId()); // guardamos el ID del documento
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Snackbar.make(
                        binding.getRoot(),
                        "Error cargando solicitudes: " + e.getMessage(),
                        Snackbar.LENGTH_LONG
                ).show());
    }

    // --------- Diálogo para cada solicitud ----------
    private void mostrarDialogoAcciones(int position) {
        if (position < 0 || position >= data.size()) return;

        User u = data.get(position);
        String nombre = (u.getName() == null ? "" : u.getName());
        String email  = (u.getEmail() == null ? "" : u.getEmail());

        new AlertDialog.Builder(this)
                .setTitle("Solicitud de guía")
                .setMessage("Nombre: " + nombre + "\nCorreo: " + email)
                .setPositiveButton("Aprobar guía", (dialog, which) -> aprobarGuia(position))
                .setNeutralButton("Ver detalles", (dialog, which) -> {
                    Intent i = new Intent(this, DetallesSolicitudGuiaActivity.class);
                    i.putExtra("guia", u); // User debe implementar Serializable/Parcelable
                    startActivity(i);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --------- Aprobar guía ----------
    private void aprobarGuia(int position) {
        if (position < 0 || position >= data.size()) return;

        String userId = userIds.get(position);
        User u = data.get(position);
        String email = u.getEmail();

        // Campos a actualizar en "usuarios"
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("guideApproved", true);
        userUpdate.put("guideApprovalStatus", "APPROVED");

        // 1) Actualizar documento en colección "usuarios"
        db.collection("usuarios")
                .document(userId)
                .update(userUpdate)
                .addOnSuccessListener(unused -> {
                    // 2) (Opcional pero recomendado) Actualizar también en colección "guias"
                    if (email != null && !email.isEmpty()) {
                        db.collection("guias")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    for (QueryDocumentSnapshot d : snap) {
                                        d.getReference().update(userUpdate);
                                    }
                                });
                    }

                    Snackbar.make(binding.getRoot(),
                            "Guía aprobado correctamente.",
                            Snackbar.LENGTH_LONG).show();

                    // 3) Quitar de la lista local
                    data.remove(position);
                    userIds.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> Snackbar.make(
                        binding.getRoot(),
                        "Error aprobando guía: " + e.getMessage(),
                        Snackbar.LENGTH_LONG
                ).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_inicio) {
            startActivity(new Intent(this, SuperAdminHomeActivity.class));
        } else if (id == R.id.nav_administradores) {
            startActivity(new Intent(this, AdministradoresActivity.class));
        } else if (id == R.id.nav_solicitudes_guias) {
            // ya estamos aquí
        } else if (id == R.id.nav_guias) {
            startActivity(new Intent(this, GuiasActivity.class));
        } else if (id == R.id.nav_clientes) {
            startActivity(new Intent(this, ClientesActivity.class));
        } else if (id == R.id.nav_reportes) {
            startActivity(new Intent(this, ReportesActivity.class));
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, LogsActivity.class));
        }
        else if (id == R.id.nav_cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ------------------- ADAPTER -------------------
    private static class ReqAdapter extends RecyclerView.Adapter<ReqAdapter.VH> {

        interface OnItemClickListener {
            void onItemClick(int position);
        }

        private final List<User> items;
        private final OnItemClickListener listener;

        ReqAdapter(List<User> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sp_administrador_rv, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = items.get(position);

            String name  = u.getName()  == null ? "(Sin nombre)" : u.getName();
            String email = u.getEmail() == null ? ""            : u.getEmail();

            holder.t1.setText(name);
            holder.t2.setText(email);

            holder.card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView t1, t2;
            CardView card;
            VH(@NonNull View v) {
                super(v);
                t1   = v.findViewById(R.id.textView1);
                t2   = v.findViewById(R.id.textView2);
                card = v.findViewById(R.id.cardView1);
            }
        }
    }
}