package com.example.silkroad_iot.ui.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.CardFB;

import java.text.DecimalFormat;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {

    public interface OnCardSelected {
        void onSelected(CardFB card);
    }

    private final List<CardFB> cards;
    private final OnCardSelected callback;
    private int selectedPosition = -1;
    private final DecimalFormat moneyFormat = new DecimalFormat("#0.00");

    public CardAdapter(List<CardFB> cards, OnCardSelected callback) {
        this.cards = cards;
        this.callback = callback;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAlias, tvBrand, tvLast4, tvBalance;
        RadioButton rbSelect;
        VH(View v) {
            super(v);
            tvAlias   = v.findViewById(R.id.tvAlias);
            tvBrand   = v.findViewById(R.id.tvBrand);
            tvLast4   = v.findViewById(R.id.tvLast4);
            tvBalance = v.findViewById(R.id.tvBalance);
            rbSelect  = v.findViewById(R.id.rbSelect);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CardFB c = cards.get(position);

        holder.tvAlias.setText(c.getAlias());
        holder.tvBrand.setText(c.getBrand() + " (" + c.getType() + ")");
        holder.tvLast4.setText("•••• " + c.getLast4());
        holder.tvBalance.setText("Saldo: S/. " + moneyFormat.format(c.getBalance()));

        holder.rbSelect.setChecked(position == selectedPosition);

        View.OnClickListener click = v -> {
            int old = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (old >= 0) notifyItemChanged(old);
            notifyItemChanged(selectedPosition);

            if (callback != null && selectedPosition >= 0 && selectedPosition < cards.size()) {
                callback.onSelected(cards.get(selectedPosition));
            }
        };

        holder.itemView.setOnClickListener(click);
        holder.rbSelect.setOnClickListener(click);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }
}