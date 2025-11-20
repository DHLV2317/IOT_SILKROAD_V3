package com.example.silkroad_iot.ui.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.TourFB;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.VH> {

    private final List<TourFB> tours;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TourAdapter(List<TourFB> tours) {
        // Usamos la referencia directamente (se actualiza desde afuera)
        this.tours = tours;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tName, tInfo, tFecha, tDesc;
        ImageView img;

        VH(View v) {
            super(v);
            img    = v.findViewById(R.id.imgTour);
            tName  = v.findViewById(R.id.tTourName);
            tInfo  = v.findViewById(R.id.tTourPrice);      // aquí mostramos precio + cupos
            tFecha = v.findViewById(R.id.tFecha);
            tDesc  = v.findViewById(R.id.tTourDescription);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        TourFB t = tours.get(i);

        String displayName = t.getDisplayName();
        double price       = t.getDisplayPrice();
        int cupos          = t.getCuposTotalesSafe();
        if (cupos <= 0) cupos = t.getDisplayPeople();

        Log.d("TOUR_ADAPTER_BIND",
                "🖼️ Dibujando tour: " + displayName +
                        " / precio=" + price +
                        " / cupos=" + cupos);

        // Nombre
        h.tName.setText(displayName.isEmpty() ? "Tour sin nombre" : displayName);

        // Precio + cupos
        String info = String.format(Locale.getDefault(),
                "S/ %.2f - %d personas", price, Math.max(cupos, 0));
        h.tInfo.setText(info);

        // Fecha (usa dateFrom si existe)
        if (t.getDateFrom() != null) {
            h.tFecha.setText("Inicio: " + sdf.format(t.getDateFrom()));
        } else {
            h.tFecha.setText("Inicio: por definir");
        }

        // Descripción corta
        String desc = t.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            h.tDesc.setText("Sin descripción.");
        } else {
            String shortDesc = desc.trim();
            if (shortDesc.length() > 90) {
                shortDesc = shortDesc.substring(0, 90) + "...";
            }
            h.tDesc.setText(shortDesc);
        }

        // Imagen
        Glide.with(h.itemView.getContext())
                .load(t.getDisplayImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.img);

        // Click → Detalle del tour
        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, TourDetailActivity.class);
            intent.putExtra("tour", t);
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tours.size();
    }

    public void updateData(List<TourFB> newList) {
        Log.d("TOUR_ADAPTER",
                "updateData() llamado. Recibidos: " + newList.size() +
                        " tours. tours.hash=" + tours.hashCode() +
                        " newList.hash=" + newList.hashCode());

        tours.clear();
        tours.addAll(newList);

        Log.d("TOUR_ADAPTER", "Adapter actualizado. tours.size() = " + tours.size());
        notifyDataSetChanged();
    }
}