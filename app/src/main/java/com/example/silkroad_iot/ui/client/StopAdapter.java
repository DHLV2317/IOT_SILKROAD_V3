package com.example.silkroad_iot.ui.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ParadaFB;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class StopAdapter extends RecyclerView.Adapter<StopAdapter.VH> {

    private final List<ParadaFB> paradas;

    public StopAdapter(List<ParadaFB> paradas) {
        this.paradas = paradas;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvTime, tvCost;
        ImageView ivMap;

        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvStopName);
            tvAddress = v.findViewById(R.id.tvStopAddress);
            tvTime    = v.findViewById(R.id.tvStopTime);
            tvCost    = v.findViewById(R.id.tvStopCost);
            ivMap     = v.findViewById(R.id.ivMapImage);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stop, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ParadaFB p = paradas.get(position);

        // Nombre
        String nombre = nz(p.getNombre());
        if (nombre.isEmpty()) nombre = "(Sin nombre)";
        h.tvName.setText(String.format(Locale.getDefault(), "%d° Parada: %s", position + 1, nombre));

        // Dirección / descripción
        String address = nz(safeGetString(p, "getAddress"));
        if (address.isEmpty()) address = nz(p.getDescripcion());
        if (address.isEmpty()) address = "—";
        h.tvAddress.setText("Dirección: " + address);

        // Tiempo
        String time = nz(safeGetString(p, "getTime"));
        if (time.isEmpty()) {
            Integer minutes = safeGetInt(p, "getMinutes");
            time = minutes == null ? "—" : (minutes + " min");
        }
        h.tvTime.setText("Tiempo: " + time);

        // Costo
        String costText = "—";
        Double cost = safeGetDouble(p, "getCost");
        if (cost != null) {
            costText = String.format(Locale.getDefault(), "S/. %.2f", cost);
        }
        h.tvCost.setText("Costo: " + costText);

        // Imagen (si existiera getImageUrl en ParadaFB)
        String imageUrl = nz(safeGetString(p, "getImageUrl"));
        if (!imageUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_map_placeholder)
                    .error(R.drawable.ic_map_placeholder)
                    .into(h.ivMap);
        } else {
            h.ivMap.setImageResource(R.drawable.ic_map_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return paradas == null ? 0 : paradas.size();
    }

    /* ===== Helpers ===== */
    private static String nz(String s){ return s == null ? "" : s.trim(); }

    private static String safeGetString(Object target, String getter) {
        try {
            Method m = target.getClass().getMethod(getter);
            Object v = m.invoke(target);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception ignored) { return ""; }
    }

    private static Integer safeGetInt(Object target, String getter) {
        try {
            Method m = target.getClass().getMethod(getter);
            Object v = m.invoke(target);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignored) {}
        return null;
    }

    private static Double safeGetDouble(Object target, String getter) {
        try {
            Method m = target.getClass().getMethod(getter);
            Object v = m.invoke(target);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Exception ignored) {}
        return null;
    }
}