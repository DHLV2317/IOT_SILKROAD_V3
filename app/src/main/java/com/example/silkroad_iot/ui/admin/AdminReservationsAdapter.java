package com.example.silkroad_iot.ui.admin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ReservaWithTour;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.TourHistorialFB;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReservationsAdapter extends RecyclerView.Adapter<AdminReservationsAdapter.VH> {

    private final List<ReservaWithTour> all;
    private final List<ReservaWithTour> data;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private String statusFilter = "Todos";

    public AdminReservationsAdapter(List<ReservaWithTour> items) {
        this.all  = new ArrayList<>(items);
        this.data = new ArrayList<>(items);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tTitle, tSub, tDate, tStatus, btnDetail;
        VH(View v){
            super(v);
            img       = v.findViewById(R.id.aImg);
            tTitle    = v.findViewById(R.id.aTitle);
            tSub      = v.findViewById(R.id.aSubtitle);
            tDate     = v.findViewById(R.id.aDate);
            tStatus   = v.findViewById(R.id.aStatus);
            btnDetail = v.findViewById(R.id.btnDetail);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt){
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_admin_reservation, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i){
        ReservaWithTour item = data.get(i);
        TourHistorialFB r = item.getReserva();
        TourFB tour       = item.getTour();

        String tourName = tour != null ? tour.getDisplayName() : "(Sin tour)";
        String clientName = r.getIdUsuario() != null ? r.getIdUsuario() : "Cliente sin nombre";

        int pax = r.getPax() > 0
                ? r.getPax()
                : (tour != null && tour.getDisplayPeople() > 0 ? tour.getDisplayPeople() : 1);

        double precioUnit = tour != null ? tour.getDisplayPrice() : 0.0;
        double total = precioUnit * pax;

        String status = r.getEstado();
        if (status == null || status.trim().isEmpty()) status = "pendiente";

        Date date = r.getFechaReserva() != null
                ? r.getFechaReserva()
                : r.getFechaRealizado();

        String imageUrl = tour != null ? tour.getImageUrl() : null;

        // Datos auxiliares para PDF
        item.tourName   = tourName;
        item.clientName = clientName;
        item.status     = status;
        item.total      = total;
        item.date       = (date == null ? null : date.getTime());
        item.rating     = r.getRating();

        // Bind UI
        h.tTitle.setText(tourName);

        String money = "S/ " + String.format(Locale.getDefault(), "%.2f", total);
        String baseSub = clientName + " · " + pax + " pax · " + money;

        if (r.getRating() != null) {
            baseSub += " · ⭐ " + String.format(Locale.getDefault(), "%.1f", r.getRating());
        }

        h.tSub.setText(baseSub);
        h.tDate.setText(date == null ? "—" : sdf.format(date));
        h.tStatus.setText(status);

        int bg = R.color.pill_gray;
        String st = status.toLowerCase(Locale.getDefault());
        if (st.contains("check-in") || st.contains("check-out")
                || st.contains("final") || st.contains("acept")) {
            bg = R.color.teal_200;
        } else if (st.contains("rech") || st.contains("cancel")) {
            bg = android.R.color.holo_red_light;
        }
        h.tStatus.setBackgroundResource(bg);

        if (imageUrl == null || imageUrl.trim().isEmpty()){
            Glide.with(h.itemView)
                    .load(R.drawable.ic_menu_24)
                    .error(R.drawable.ic_menu_24)
                    .into(h.img);
        } else {
            Glide.with(h.itemView)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_menu_24)
                    .error(R.drawable.ic_menu_24)
                    .into(h.img);
        }

        h.btnDetail.setOnClickListener(v -> {
            Intent it = new Intent(v.getContext(), AdminReservationDetailActivity.class);
            it.putExtra("reserva", item);
            v.getContext().startActivity(it);
        });
    }

    @Override public int getItemCount(){ return data.size(); }

    public void filter(String query, String status){
        String q  = query  == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        String stFilter = status == null ? "Todos" : status.toLowerCase(Locale.getDefault());

        data.clear();
        for (ReservaWithTour item : all){

            TourHistorialFB r = item.getReserva();
            TourFB tour       = item.getTour();

            String tourName = tour != null ? tour.getDisplayName() : "";
            String cli      = r.getIdUsuario() != null ? r.getIdUsuario() : "";
            String s        = r.getEstado() != null ? r.getEstado() : "pendiente";

            String sLower = s.toLowerCase(Locale.getDefault());

            boolean matchText = q.isEmpty()
                    || tourName.toLowerCase(Locale.getDefault()).contains(q)
                    || cli.toLowerCase(Locale.getDefault()).contains(q);

            boolean matchStatus = stFilter.equals("todos")
                    || sLower.equals(stFilter);

            if (matchText && matchStatus) data.add(item);
        }
        notifyDataSetChanged();
    }

    public void setStatusFilter(String status){
        this.statusFilter = status == null ? "Todos" : status;
    }

    public String getStatusFilter(){ return statusFilter; }

    public void replace(List<ReservaWithTour> newItems) {
        all.clear();
        data.clear();
        all.addAll(newItems);
        data.addAll(newItems);
        notifyDataSetChanged();
    }
}