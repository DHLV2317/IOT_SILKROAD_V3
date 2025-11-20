package com.example.silkroad_iot.ui.client;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.Department;

import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.VH> {

    public interface OnClick {
        void onClick(Department d);
    }

    private final List<Department> data;
    private final OnClick cb;

    // Si quieres resaltar la ciudad seleccionada
    private int selectedIndex = -1;

    public DepartmentAdapter(List<Department> data, OnClick cb) {
        this.data = data;
        this.cb = cb;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtCiudad;

        VH(View v) {
            super(v);
            txtCiudad = v.findViewById(R.id.tTitle);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_department, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, @SuppressLint("RecyclerView") int position) {
        Department departamento = data.get(position);

        holder.txtCiudad.setText(departamento.getNombre());

        // Estilo seleccionado (opcional)
        holder.itemView.setSelected(position == selectedIndex);

        holder.itemView.setOnClickListener(v -> {
            selectedIndex = position;
            notifyDataSetChanged();
            cb.onClick(departamento);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}