package com.example.silkroad_iot.ui.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.Company;

import java.util.ArrayList;
import java.util.List;
import com.bumptech.glide.Glide;
import com.example.silkroad_iot.data.EmpresaFb;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.VH>{
    private final List<EmpresaFb> data;
    private final List<EmpresaFb> fullList;

    public CompanyAdapter(List<EmpresaFb> data) {
        this.data = data; // 👉 usa la misma lista
        this.fullList = new ArrayList<>(data);
    }

    public void updateData(List<EmpresaFb> newList) {
        data.clear();
        data.addAll(newList);

        fullList.clear();
        fullList.addAll(newList);

        notifyDataSetChanged();
    }

    public void filterList(String query) {
        data.clear();
        if (query.isEmpty()) {
            data.addAll(fullList);
        } else {
            for (EmpresaFb c : fullList) {
                if (c.getNombre().toLowerCase().contains(query.toLowerCase())) {
                    data.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        ImageView img;

        VH(View v) {
            super(v);
            t1 = v.findViewById(R.id.tTitle);
            t2 = v.findViewById(R.id.tRating);
            img = v.findViewById(R.id.imgCompany);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        EmpresaFb c = data.get(i);

        Log.d("ADAPTER_BIND", "Dibujando empresa: " + c.getNombre());

        h.t1.setText(c.getNombre());
        h.t2.setText("* 4.5"); // Simulación de rating si no lo tienes aún en Firestore



        Glide.with(h.img.getContext())
                .load(c.getImagen())
                .into(h.img);

        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, ToursActivity.class);
            intent.putExtra("company", c); // Puedes pasar solo el nombre y luego cargar tours
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


}