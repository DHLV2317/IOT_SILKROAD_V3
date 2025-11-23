package com.example.silkroad_iot.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ContentAdminSupportChatBinding;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminSupportChatActivity extends BaseDrawerActivity {

    private static final String TAG = "AdminSupportChat";
    private ContentAdminSupportChatBinding b;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TicketAdapter adapter;
    private final List<QueryDocumentSnapshot> tickets = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawer(R.layout.content_admin_support_chat, R.menu.menu_drawer_admin, "Soporte / Chat");

        FrameLayout container = findViewById(R.id.contentContainer);
        b = ContentAdminSupportChatBinding.bind(container.getChildAt(0));

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        fetchTickets();
    }

    private void setupRecyclerView() {
        adapter = new TicketAdapter(tickets);
        b.rvTickets.setLayoutManager(new LinearLayoutManager(this));
        b.rvTickets.setAdapter(adapter);
    }

    private void fetchTickets() {
        if (currentUser == null) {
            Toast.makeText(this, "No se pudo autenticar al administrador.", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminEmail = currentUser.getEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            Toast.makeText(this, "El correo del administrador no está disponible.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("chats")
                .whereEqualTo("idsoporte", adminEmail)
                .orderBy("fecha_creacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() == null || task.getResult().isEmpty()) {
                            Toast.makeText(this, "No tiene tickets asignados.", Toast.LENGTH_SHORT).show();
                        } else {
                            tickets.clear();
                            // Correct way: Iterate through the QuerySnapshot to add documents to the list.
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                tickets.add(document);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Error fetching tickets: ", task.getException());
                        Toast.makeText(this, "Error al cargar los tickets. Verifique el índice de Firestore.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected int defaultMenuId() {
        return R.id.m_support;
    }

    // Adapter para el RecyclerView
    private static class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

        private final List<QueryDocumentSnapshot> ticketList;

        TicketAdapter(List<QueryDocumentSnapshot> ticketList) {
            this.ticketList = ticketList;
        }

        @NonNull
        @Override
        public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new TicketViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
            QueryDocumentSnapshot ticket = ticketList.get(position);

            String userId = ticket.getString("idusuario");
            String context = ticket.getString("contexto");
            String tourName = ticket.getString("nombre del tour");

            holder.text1.setText("Tour: " + (tourName != null ? tourName : "N/A"));
            holder.text2.setText("Usuario: " + userId + " - " + context);
        }

        @Override
        public int getItemCount() {
            return ticketList.size();
        }

        static class TicketViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;

            TicketViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
