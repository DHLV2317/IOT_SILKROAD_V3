package com.example.silkroad_iot.ui.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.VH> {
    private final List<String> items;

    public SimpleTextAdapter(List<String> items){ this.items = items; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(View v){ super(v); tv = v.findViewById(android.R.id.text1); }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos){ h.tv.setText(items.get(pos)); }
    @Override public int getItemCount(){ return items.size(); }
}