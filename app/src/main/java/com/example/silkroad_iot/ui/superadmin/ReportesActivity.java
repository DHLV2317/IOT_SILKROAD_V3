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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.databinding.ActivitySuperadminReportesBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReportesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivitySuperadminReportesBinding binding;
    //private final List<ReporteItem> data = new ArrayList<>();
    private final List<EmpresaFb> data = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ReportesAdapter adapter;
    private FirebaseFirestore db;

    static class ReporteItem {
        String id;
        String titulo;
        String descripcion;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminReportesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle); toggle.syncState();

        adapter = new ReportesAdapter(data);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
    }

    @Override protected void onStart() {
        super.onStart();
        cargarLista();
    }

    private void cargarLista() {
        data.clear();
        db.collection("empresas")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot d : snap) {
                        if(d.contains("correo")){
                            EmpresaFb e = d.toObject(EmpresaFb.class);;
                            data.add(e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_inicio) startActivity(new Intent(this, SuperAdminHomeActivity.class));
        else if (id == R.id.nav_administradores) startActivity(new Intent(this, AdministradoresActivity.class));
        else if (id == R.id.nav_solicitudes_guias) startActivity(new Intent(this, SolicitudesGuiasActivity.class));
        else if (id == R.id.nav_guias) startActivity(new Intent(this, GuiasActivity.class));
        else if (id == R.id.nav_clientes) startActivity(new Intent(this, ClientesActivity.class));
        else if (id == R.id.nav_reportes) { /* aquí */ }
        else if (id == R.id.nav_logs) startActivity(new Intent(this, LogsActivity.class));
        else if (id == R.id.nav_cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private static class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.VH> {
        //private final List<ReporteItem> items;
        private final List<EmpresaFb> items;
        //ReportesAdapter(List<ReporteItem> items){ this.items = items; }
        ReportesAdapter(List<EmpresaFb> items){ this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.sp_administrador_rv, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            //ReporteItem r = items.get(pos);
            EmpresaFb r = items.get(pos);
            h.t1.setText(r.getNombre() == null ? "(Sin título)" : r.getNombre());
            h.t2.setText(r.getEmail() == null ? "" : r.getEmail());
            h.card.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), DetallesReporteActivity.class);
                //i.putExtra("titulo", r.titulo);
                //i.putExtra("descripcion", r.descripcion);
                i.putExtra("empresa", r);
                v.getContext().startActivity(i);
            });
        }
        @Override public int getItemCount(){ return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView t1, t2;
            CardView card;
            VH(@NonNull View v){
                super(v);
                t1=v.findViewById(R.id.textView1);
                t2=v.findViewById(R.id.textView2);
                card=v.findViewById(R.id.cardView1);
            }
        }
    }
}