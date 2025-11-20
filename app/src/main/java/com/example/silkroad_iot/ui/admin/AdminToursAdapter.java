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
import com.example.silkroad_iot.data.TourFB;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminToursAdapter extends RecyclerView.Adapter<AdminToursAdapter.VH> {

    private final List<TourFB> data = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public AdminToursAdapter(List<TourFB> items){ replace(items); }

    public void replace(List<TourFB> items){
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tTitle, tSubtitle, tDate, btnDetail;
        ImageView img;
        VH(@NonNull View v){
            super(v);
            tTitle    = v.findViewById(R.id.aTitle);
            tSubtitle = v.findViewById(R.id.aSubtitle);
            tDate     = v.findViewById(R.id.aDate);
            btnDetail = v.findViewById(R.id.btnDetail);
            img       = v.findViewById(R.id.aImg);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt){
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_admin_tour, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i){
        TourFB t = data.get(i);

        String name   = t.getDisplayName();
        String desc   = t.getDescription() == null ? "" : t.getDescription();
        double price  = t.getDisplayPrice();
        int people    = t.getDisplayPeople();
        String imgUrl = t.getDisplayImageUrl();

        // Fechas
        String fecha = "sin fecha";
        if (t.getDateFrom() != null && t.getDateTo() != null) {
            fecha = sdf.format(t.getDateFrom()) + " - " + sdf.format(t.getDateTo());
        } else if (t.getDateFrom() != null) {
            fecha = sdf.format(t.getDateFrom());
        } else if (t.getDateTo() != null) {
            fecha = sdf.format(t.getDateTo());
        }

        // Estado legible
        String estado = t.getEstado();
        String prettyEstado = (estado == null || estado.trim().isEmpty())
                ? "pendiente"
                : estado;

        h.tTitle.setText(name == null || name.isEmpty() ? "Sin nombre" : name);

        String meta = "S/ " + String.format(Locale.getDefault(),"%.2f", price)
                + " · " + people + " personas";
        h.tSubtitle.setText(desc.isEmpty() ? meta : meta + " · " + desc);

        // Fecha + estado
        h.tDate.setText(fecha + " · " + prettyEstado);

        // Imagen
        if (imgUrl == null || imgUrl.isEmpty()) {
            h.img.setImageResource(R.drawable.ic_menu_24);
        } else {
            Glide.with(h.itemView)
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_menu_24)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(h.img);
        }

        // Ver detalle
        h.btnDetail.setOnClickListener(v -> {
            Intent it = new Intent(v.getContext(), AdminTourDetailViewActivity.class);
            it.putExtra("tourId", t.getId());
            v.getContext().startActivity(it);
        });
    }

    @Override public int getItemCount(){ return data.size(); }
}