package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.Department;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityClientHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientHomeActivity extends AppCompatActivity {

    private ActivityClientHomeBinding b;
    private final UserStore store = UserStore.get();

    private FirebaseFirestore db;

    // Drawer
    private ActionBarDrawerToggle drawerToggle;

    // Empresas
    private CompanyAdapter companyAdapter;
    private final List<EmpresaFb> empresasFB = new ArrayList<>();

    // Departamentos
    private DepartmentAdapter departmentAdapter;
    private final List<Department> departamentos = new ArrayList<>();

    // Mapa: departamento -> IDs de empresas que tienen tours ahí
    private final Map<String, Set<String>> deptToCompanyIds = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityClientHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Toolbar como ActionBar
        setSupportActionBar(b.toolbar);

        // DrawerToggle (icono hamburguesa)
        drawerToggle = new ActionBarDrawerToggle(
                this,
                b.drawerLayout,
                b.toolbar,
                R.string.nav_open,
                R.string.nav_close
        );
        b.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Usuario actual
        User u = store.getLogged();
        String name = (u != null ? u.getName() : "Usuario");
        String email = (u != null ? u.getEmail() : "");

        b.toolbar.setTitle("Hola " + name);
        b.tvHello.setText("Hola " + name);

        // Header del NavigationView
        View header = b.navViewClient.getHeaderView(0);
        TextView tvHeaderName = header.findViewById(R.id.tvHeaderName);
        TextView tvHeaderEmail = header.findViewById(R.id.tvHeaderEmail);
        ImageView imgAvatarHeader = header.findViewById(R.id.imgAvatarHeader);

        tvHeaderName.setText(name);
        tvHeaderEmail.setText(email);
        if (u != null && u.getPhotoUri() != null && !u.getPhotoUri().isEmpty()) {
            Glide.with(this).load(u.getPhotoUri()).into(imgAvatarHeader);
        }

        // Listener del menú lateral
        b.navViewClient.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int id = item.getItemId();

                        if (id == R.id.m_home) {
                            Log.d("NAV", "Home seleccionado");
                        } else if (id == R.id.m_profile) {
                            startActivity(new Intent(ClientHomeActivity.this, ClientOnboardingActivity.class));
                        } else if (id == R.id.m_history) {
                            startActivity(new Intent(ClientHomeActivity.this, TourHistoryActivity.class));
                        } else if (id == R.id.m_payments) {
                            startActivity(new Intent(ClientHomeActivity.this, PaymentMethodsActivity.class));
                        } else if (id == R.id.m_support_chat) {
                            startActivity(new Intent(ClientHomeActivity.this, SupportChatActivity.class));
                        } else if (id == R.id.m_logout) {
                            FirebaseAuth.getInstance().signOut();
                            finish();
                        }

                        b.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                }
        );

        // Firestore
        db = FirebaseFirestore.getInstance();

        // Recycler EMPRESAS
        companyAdapter = new CompanyAdapter(empresasFB);
        b.rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        b.rvCompanies.setAdapter(companyAdapter);

        // Recycler DEPARTAMENTOS
        departmentAdapter = new DepartmentAdapter(departamentos, d -> {
            String ciudad = d.getNombre();
            Log.d("CIUDAD_CLICK", "Departamento seleccionado: " + ciudad);
            aplicarFiltroPorCiudad(ciudad);
        });
        b.rvDepartments.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );
        b.rvDepartments.setAdapter(departmentAdapter);

        // Cargar empresas y departamentos
        cargarEmpresasDesdeFirebase();
        cargarCiudadesDesdeToursFirebase();

        // Filtro búsqueda empresas (por nombre)
        b.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                companyAdapter.filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Botón Historial
        b.btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, TourHistoryActivity.class);
            startActivity(i);
        });
    }

    @Override
    public void onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void cargarEmpresasDesdeFirebase() {
        Log.d("EMPRESAS_FIREBASE", "Cargando empresas...");

        db.collection("empresas")
                .get()
                .addOnSuccessListener(query -> {
                    List<EmpresaFb> nuevas = new ArrayList<>();

                    for (DocumentSnapshot doc : query) {
                        EmpresaFb emp = doc.toObject(EmpresaFb.class);
                        if (emp != null) {
                            emp.setId(doc.getId());
                            nuevas.add(emp);
                            Log.d("EMPRESAS_FIREBASE", "Empresa: " + emp.getNombre());
                        }
                    }

                    empresasFB.clear();
                    empresasFB.addAll(nuevas);
                    companyAdapter.updateData(nuevas);

                    Log.d("EMPRESAS_FIREBASE", "Total empresas cargadas: " + nuevas.size());
                })
                .addOnFailureListener(e -> Log.e("EMPRESAS_FIREBASE", "Error al cargar empresas", e));
    }

    private void cargarCiudadesDesdeToursFirebase() {
        Log.d("CIUDADES_FIREBASE", "Cargando departamentos desde tours...");

        db.collection("tours")
                .get()
                .addOnSuccessListener(query -> {
                    Set<String> ciudadesSet = new LinkedHashSet<>();
                    deptToCompanyIds.clear();

                    for (DocumentSnapshot doc : query) {
                        TourFB tour = doc.toObject(TourFB.class);
                        if (tour != null) {
                            // Usamos el departamento guardado en TourFB
                            String ciudad = tour.getCiudad();
                            String empresaId = tour.getEmpresaId(); // asumiendo que TourFB tiene este campo
                            if (ciudad != null && !ciudad.trim().isEmpty() && empresaId != null) {
                                ciudad = ciudad.trim();
                                ciudadesSet.add(ciudad);

                                Set<String> empresasDeCiudad =
                                        deptToCompanyIds.containsKey(ciudad)
                                                ? deptToCompanyIds.get(ciudad)
                                                : new LinkedHashSet<>();
                                empresasDeCiudad.add(empresaId);
                                deptToCompanyIds.put(ciudad, empresasDeCiudad);
                            }
                        }
                    }

                    departamentos.clear();
                    for (String c : ciudadesSet) {
                        departamentos.add(new Department(c));
                    }
                    departmentAdapter.notifyDataSetChanged();

                    Log.d("CIUDADES_FIREBASE", "Departamentos encontrados: " + ciudadesSet.size());
                })
                .addOnFailureListener(e -> Log.e("CIUDADES_FIREBASE", "Error al cargar departamentos", e));
    }

    private void aplicarFiltroPorCiudad(String ciudad) {
        Set<String> empresasPermitidas = deptToCompanyIds.get(ciudad);
        if (empresasPermitidas == null || empresasPermitidas.isEmpty()) {
            // si no hay mapeo, mostramos todas pero cambiando título
            companyAdapter.updateData(new ArrayList<>(empresasFB));
            b.tvBestRatedTitle.setText("Mejores calificadas");
            return;
        }

        List<EmpresaFb> filtradas = new ArrayList<>();
        for (EmpresaFb e : empresasFB) {
            if (empresasPermitidas.contains(e.getId())) {
                filtradas.add(e);
            }
        }

        companyAdapter.updateData(filtradas);
        b.tvBestRatedTitle.setText("Mejores calificadas en " + ciudad);
    }
}