package com.example.silkroad_iot.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.GuideFb;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminGuidesAdapter extends RecyclerView.Adapter<AdminGuidesAdapter.VH> {

    public interface Callbacks {
        void onAssignClicked(int position);
        void onDetailClicked(int position);
    }

    private final List<GuideFb> all  = new ArrayList<>();
    private final List<GuideFb> data = new ArrayList<>();
    private final Callbacks cb;

    public AdminGuidesAdapter(List<GuideFb> items, Callbacks callbacks) {
        if (items != null) {
            all.addAll(items);
            data.addAll(items);
        }
        this.cb = callbacks;
    }

    public void updateData(List<GuideFb> items) {
        all.clear();
        data.clear();
        if (items != null) {
            all.addAll(items);
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tName, tSubtitle, tStatus, tExtra;
        MaterialButton btnAssign, btnDetail;
        VH(View v) {
            super(v);
            img       = v.findViewById(R.id.aImg);
            tName     = v.findViewById(R.id.aTitle);
            tSubtitle = v.findViewById(R.id.aSubtitle);
            tStatus   = v.findViewById(R.id.aStatus);
            tExtra    = v.findViewById(R.id.aExtra);
            btnAssign = v.findViewById(R.id.btnAssign);
            btnDetail = v.findViewById(R.id.btnDetail);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_guide, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GuideFb g = data.get(position);

        // Foto
        String foto = safe(g.getFotoUrl());
        if (foto.isEmpty()) {
            Glide.with(h.itemView)
                    .load(R.drawable.ic_person_24)
                    .into(h.img);
        } else {
            Glide.with(h.itemView)
                    .load(foto)
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(h.img);
        }

        // Datos principales
        String name   = safe(g.getNombre());
        String langs  = safe(g.getLangs());
        String state  = safe(g.getEstado());
        String actual = safe(g.getTourActual());

        h.tName.setText(name.isEmpty() ? "(Sin nombre)" : name);
        h.tSubtitle.setText(langs.isEmpty() ? "—" : langs);

        // Estado amigable
        String estadoUi;
        if (!actual.isEmpty()) {
            estadoUi = "En tour";
        } else if (!state.isEmpty()) {
            estadoUi = state;
        } else {
            estadoUi = "Disponible";
        }
        h.tStatus.setText(estadoUi);

        // Historial / tour actual
        int hist = (g.getHistorial() == null) ? 0 : g.getHistorial().size();
        if (actual.isEmpty()) {
            h.tExtra.setText(hist + " tours completados");
        } else {
            h.tExtra.setText(hist + " tours completados • Actual: " + actual);
        }

        // Lógica de habilitar / deshabilitar asignación
        boolean ocupado = !actual.isEmpty()
                || estadoUi.equalsIgnoreCase("EN_CURSO")
                || estadoUi.equalsIgnoreCase("Ocupado")
                || estadoUi.equalsIgnoreCase("Busy");

        h.btnAssign.setEnabled(!ocupado);
        h.btnAssign.setAlpha(ocupado ? 0.5f : 1f);

        h.btnAssign.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && cb != null) cb.onAssignClicked(pos);
        });

        h.btnDetail.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && cb != null) cb.onDetailClicked(pos);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void filter(String q) {
        String s = q == null ? "" : q.toLowerCase(Locale.getDefault()).trim();
        data.clear();
        if (s.isEmpty()) {
            data.addAll(all);
        } else {
            for (GuideFb g : all) {
                String name  = safe(g.getNombre()).toLowerCase(Locale.getDefault());
                String langs = safe(g.getLangs()).toLowerCase(Locale.getDefault());
                String state = safe(g.getEstado()).toLowerCase(Locale.getDefault());
                if (name.contains(s) || langs.contains(s) || state.contains(s)) {
                    data.add(g);
                }
            }
        }
        notifyDataSetChanged();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}