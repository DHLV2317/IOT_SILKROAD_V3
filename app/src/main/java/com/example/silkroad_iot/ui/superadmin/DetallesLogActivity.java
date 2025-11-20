package com.example.silkroad_iot.ui.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ActivitySuperadminDetallesLogBinding;

import java.text.SimpleDateFormat;

public class DetallesLogActivity extends AppCompatActivity {

    private ActivitySuperadminDetallesLogBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetallesLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent i = getIntent();
        String tipo = i.getStringExtra("tipo");
        String tipoUsuario = i.getStringExtra("tipoUsuario");
        String nombre = i.getStringExtra("nombre");
        String fecha = i.getStringExtra("fecha");

        assert fecha != null;
        long fechaLong = Long.parseLong(fecha);
        java.util.Date fechaDate = new java.util.Date(fechaLong);

        String hora = i.getStringExtra("hora");
        String usuario = i.getStringExtra("usuario");
        String descripcion = i.getStringExtra("descripcion");

        binding.textView.setText("Evento " + tipo + " de " + tipoUsuario);
        binding.textView10.setText(nombre);
        binding.textView12.setText(String.valueOf(fechaDate));
        //binding.textView13.setText(String.valueOf(hora));
        binding.textView13.setText(String.valueOf(usuario));
        binding.textView16.setText(String.valueOf(descripcion));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}