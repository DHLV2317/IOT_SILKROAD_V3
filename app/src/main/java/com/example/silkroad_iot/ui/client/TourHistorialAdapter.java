package com.example.silkroad_iot.ui.client;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.TourHistorialFB;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TourHistorialAdapter extends RecyclerView.Adapter<TourHistorialAdapter.VH> {

    private final List<TourHistorialFB> historialList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public TourHistorialAdapter(List<TourHistorialFB> historialList) {
        this.historialList = historialList;
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvTourName, tvDate, tvStatus, tvTotalPrice, tvOrderDate;

        public VH(@NonNull View v) {
            super(v);
            tvTourName   = v.findViewById(R.id.tvCompanyName);
            tvDate       = v.findViewById(R.id.tvTourDate);
            tvStatus     = v.findViewById(R.id.tvStatus);
            tvTotalPrice = v.findViewById(R.id.tvTotalPrice);
            tvOrderDate  = v.findViewById(R.id.tvOrderDate);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TourHistorialFB historial = historialList.get(position);

        // 🕒 Fecha del tour (fechaRealizado)
        Date fechaTour = historial.getFechaRealizado();
        if (fechaTour != null) {
            holder.tvDate.setText("Inicio del Tour: " + sdf.format(fechaTour));
        } else {
            holder.tvDate.setText("Inicio del Tour: -");
            Log.w("HISTORIAL", "⚠️ fechaRealizado es null en posición " + position);
        }

        // 📅 Fecha de reserva
        Date fechaReserva = historial.getFechaReserva();
        if (fechaReserva != null) {
            holder.tvOrderDate.setText("Reservado el: " + sdf.format(fechaReserva));
        } else {
            holder.tvOrderDate.setText("Reservado el: -");
            Log.w("HISTORIAL", "⚠️ fechaReserva es null en posición " + position);
        }

        holder.tvStatus.setText("Estado: " + (historial.getEstado() == null ? "-" : historial.getEstado()));

        int pax = historial.getPax() > 0 ? historial.getPax() : 1;

        // ✅ Cargar datos del tour
        FirebaseFirestore.getInstance()
                .collection("tours")
                .document(historial.getIdTour())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        holder.tvTourName.setText("Tour no encontrado");
                        holder.tvTotalPrice.setText("S/ -");
                        return;
                    }

                    TourFB tour = doc.toObject(TourFB.class);
                    if (tour == null) {
                        tour = new TourFB();
                        tour.setId(doc.getId());
                        tour.setNombre(doc.getString("nombre"));
                    } else {
                        tour.setId(doc.getId());
                    }

                    String displayName = tour.getDisplayName();
                    holder.tvTourName.setText(displayName.isEmpty() ? "Sin nombre" : displayName);

                    double precioUnit = tour.getDisplayPrice();
                    double total = precioUnit * pax;
                    holder.tvTotalPrice.setText(
                            String.format(Locale.getDefault(), "Total: S/ %.2f", total)
                    );

                    TourFB finalTour = tour;
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(v.getContext(), OrderDetailActivity.class);
                        intent.putExtra("tourFB", finalTour);
                        intent.putExtra("historialFB", historial);
                        intent.putExtra("historialId", historial.getId());
                        v.getContext().startActivity(intent);
                    });
                })
                .addOnFailureListener(e -> {
                    holder.tvTourName.setText("Error cargando tour");
                    holder.tvTotalPrice.setText("S/ -");
                });
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    public List<TourHistorialFB> getOrders() {
        return historialList;
    }
}