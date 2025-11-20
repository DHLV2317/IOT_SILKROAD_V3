package com.example.silkroad_iot.ui.admin;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ContentAdminSupportChatBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;

public class AdminSupportChatActivity extends BaseDrawerActivity {

    private ContentAdminSupportChatBinding b;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inserta el layout dentro del drawer
        setupDrawer(R.layout.content_admin_support_chat, R.menu.menu_drawer_admin, "Soporte / Chat");

        FrameLayout container = findViewById(R.id.contentContainer);
        b = ContentAdminSupportChatBinding.bind(container.getChildAt(0));

        // Aquí luego integras el chat real (Recycler + EditText + enviar)
        // Por ahora solo es el diseño base
    }

    @Override
    protected int defaultMenuId() {
        return R.id.m_support;   // item del menú para Soporte / Chat
    }
}