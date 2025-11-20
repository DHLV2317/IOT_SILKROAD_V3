package com.example.silkroad_iot.ui.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.silkroad_iot.databinding.ActivityAdminHomeBinding;

public class AdminHomeActivity extends AppCompatActivity {
    private ActivityAdminHomeBinding b;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        b = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar()!=null) getSupportActionBar().setTitle("Admin - Inicio");
    }
}