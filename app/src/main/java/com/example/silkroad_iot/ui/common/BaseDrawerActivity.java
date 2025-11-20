package com.example.silkroad_iot.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.MainActivity;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityBaseDrawerBinding;
import com.example.silkroad_iot.ui.admin.AdminGuidesActivity;
import com.example.silkroad_iot.ui.admin.AdminProfileActivity;
import com.example.silkroad_iot.ui.admin.AdminReportsActivity;
import com.example.silkroad_iot.ui.admin.AdminReservationsActivity;
import com.example.silkroad_iot.ui.admin.AdminToursActivity;
import com.example.silkroad_iot.ui.admin.AdminPaymentsActivity;
import com.example.silkroad_iot.ui.admin.AdminSupportChatActivity;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected ActivityBaseDrawerBinding b;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        b = ActivityBaseDrawerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Toolbar + hamburguesa
        setSupportActionBar(b.toolbar);
        toggle = new ActionBarDrawerToggle(
                this, b.drawer, b.toolbar,
                R.string.nav_open, R.string.nav_close
        );
        b.drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Nav
        b.navView.setNavigationItemSelectedListener(this);

        // Header (si no hay, infla)
        if (b.navView.getHeaderCount() == 0) {
            b.navView.inflateHeaderView(R.layout.nav_header_common);
        }
        View header = b.navView.getHeaderView(0);

        TextView tName  = header.findViewById(R.id.tName);
        TextView tEmail = header.findViewById(R.id.tEmail);
        ImageView img   = header.findViewById(R.id.imgProfile);
        View btnLogout  = header.findViewById(R.id.btnLogout);

        User u = UserStore.get().getLogged();
        if (u != null) {
            tName.setText(u.getName());
            tEmail.setText(u.getEmail());
            String photo = u.getPhotoUri();
            if (photo != null && !photo.isEmpty()) {
                Glide.with(this).load(photo).circleCrop().into(img);
            } else {
                Glide.with(this).load(R.drawable.ic_person_24).circleCrop().into(img);
            }
        }

        img.setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            UserStore.get().logout();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finishAffinity();
        });
    }

    /** Infla el layout de contenido dentro del drawer y carga el menú correspondiente. */
    protected void setupDrawer(@LayoutRes int contentLayout, @MenuRes int menu, String title) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);

        b.navView.getMenu().clear();
        b.navView.inflateMenu(menu);

        FrameLayout container = b.contentContainer;
        container.removeAllViews();
        getLayoutInflater().inflate(contentLayout, container, true);

        int sel = defaultMenuId();
        if (sel != 0) b.navView.setCheckedItem(sel);
    }

    /** Devuelve el id del item del menú a marcar como seleccionado en esta pantalla. */
    protected @IdRes int defaultMenuId() { return 0; }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Si ya estoy en la misma sección, no navego
        if (id == defaultMenuId()) {
            b.drawer.closeDrawers();
            return true;
        }

        if (id == R.id.m_tours) {
            startActivity(new Intent(this, AdminToursActivity.class));

        } else if (id == R.id.m_reservations) {
            startActivity(new Intent(this, AdminReservationsActivity.class));

        } else if (id == R.id.m_guides) {
            startActivity(new Intent(this, AdminGuidesActivity.class));

        } else if (id == R.id.m_reports) {
            startActivity(new Intent(this, AdminReportsActivity.class));

        } else if (id == R.id.m_payments) {
            // Nuevo: pantalla de pagos
            startActivity(new Intent(this, AdminPaymentsActivity.class));

        } else if (id == R.id.m_support) {
            // Nuevo: pantalla de soporte / chat
            startActivity(new Intent(this, AdminSupportChatActivity.class));
        }

        b.drawer.closeDrawers();
        return true;
    }
}