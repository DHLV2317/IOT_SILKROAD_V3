package com.example.silkroad_iot.ui.admin;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ContentAdminPaymentsBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;

public class AdminPaymentsActivity extends BaseDrawerActivity {

    private ContentAdminPaymentsBinding b;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawer(R.layout.content_admin_payments, R.menu.menu_drawer_admin, "Pagos");

        FrameLayout container = findViewById(R.id.contentContainer);
        b = ContentAdminPaymentsBinding.bind(container.getChildAt(0));

        // Más adelante aquí metes tu lógica de pagos/liquidaciones
        // (Recycler de pagos, filtros, etc.)
    }

    @Override
    protected int defaultMenuId() {
        return R.id.m_payments;
    }
}