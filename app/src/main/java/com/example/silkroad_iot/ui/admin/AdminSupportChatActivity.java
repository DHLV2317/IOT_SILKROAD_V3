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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.databinding.ContentAdminSupportChatBinding;
import com.example.silkroad_iot.ui.client.SupportChatActivity;
import com.example.silkroad_iot.ui.common.BaseDrawerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminSupportChatActivity extends BaseDrawerActivity {

    private static final String TAG = "AdminSupportChat";

    private enum ViewState {
        TICKET_LIST,
        IN_CHAT
    }

    private ContentAdminSupportChatBinding b;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final List<QueryDocumentSnapshot> tickets = new ArrayList<>();
    private ViewState currentView = ViewState.TICKET_LIST;
    private String currentChatId;
    private ListenerRegistration chatListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawer(R.layout.content_admin_support_chat, R.menu.menu_drawer_admin, "Soporte / Chat");

        FrameLayout container = findViewById(R.id.contentContainer);
        b = ContentAdminSupportChatBinding.bind(container.getChildAt(0));

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        b.btnSend.setOnClickListener(v -> sendMessage());

        // CORRECCIÓN: Obtener la Toolbar con findViewById
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (currentView == ViewState.IN_CHAT) {
                exitChatMode();
            } else {
                // Comportamiento por defecto del drawer
                super.b.drawer.openDrawer(GravityCompat.START);
            }
        });

        showTicketList();
    }

    private void showTicketList() {
        currentView = ViewState.TICKET_LIST;
        // Usar la toolbar para establecer el título
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Tickets de Soporte");
        b.chatBox.setVisibility(View.GONE);

        TicketAdapter adapter = new TicketAdapter(tickets, this::enterChatMode);
        b.rvItems.setLayoutManager(new LinearLayoutManager(this));
        b.rvItems.setAdapter(adapter);

        fetchTickets();
    }

    private void fetchTickets() {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "No se pudo autenticar al administrador.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("chats")
                .whereEqualTo("idsoporte", currentUser.getEmail())
                .orderBy("fecha_creacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tickets.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            tickets.add(doc);
                        }
                        b.rvItems.getAdapter().notifyDataSetChanged();
                        if (tickets.isEmpty()) {
                            Toast.makeText(this, "No tiene tickets asignados.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error fetching tickets: ", task.getException());
                        Toast.makeText(this, "Error al cargar los tickets.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void enterChatMode(String chatId) {
        currentChatId = chatId;
        currentView = ViewState.IN_CHAT;
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Ticket #" + chatId.substring(0, 6));

        b.chatBox.setVisibility(View.VISIBLE);

        MessageAdapter adapter = new MessageAdapter(new ArrayList<>(), currentUser.getUid());
        b.rvItems.setAdapter(adapter);

        chatListener = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    List<SupportChatActivity.ChatMessage> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        messages.add(doc.toObject(SupportChatActivity.ChatMessage.class));
                    }
                    adapter.updateMessages(messages);
                    b.rvItems.scrollToPosition(messages.size() - 1);
                });
    }

    private void exitChatMode() {
        if (chatListener != null) {
            chatListener.remove(); // Detener la escucha de mensajes para evitar memory leaks
        }
        currentChatId = null;
        showTicketList();
    }

    private void sendMessage() {
        String text = b.inputMessage.getText().toString().trim();
        if (text.isEmpty() || currentUser == null || currentChatId == null) return;

        b.inputMessage.setText("");
        SupportChatActivity.ChatMessage message = new SupportChatActivity.ChatMessage(currentUser.getUid(), text);
        db.collection("chats").document(currentChatId).collection("messages").add(message);
    }

    @Override
    public void onBackPressed() {
        if (currentView == ViewState.IN_CHAT) {
            exitChatMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int defaultMenuId() {
        return R.id.m_support;
    }

    // --- ADAPTERS ---

    private interface OnTicketClickListener {
        void onTicketClick(String chatId);
    }

    private static class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
        private final List<QueryDocumentSnapshot> ticketList;
        private final OnTicketClickListener listener;

        TicketAdapter(List<QueryDocumentSnapshot> ticketList, OnTicketClickListener listener) {
            this.ticketList = ticketList;
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QueryDocumentSnapshot ticket = ticketList.get(position);
            String tourName = ticket.getString("nombre del tour");
            String userContext = ticket.getString("contexto");

            holder.text1.setText("Tour: " + (tourName != null ? tourName : "N/A"));
            holder.text2.setText("Contexto: " + userContext);
            holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket.getId()));
        }

        @Override public int getItemCount() { return ticketList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private List<SupportChatActivity.ChatMessage> messages;
        private final String adminId;

        MessageAdapter(List<SupportChatActivity.ChatMessage> messages, String adminId) {
            this.messages = messages;
            this.adminId = adminId;
        }

        public void updateMessages(List<SupportChatActivity.ChatMessage> newMessages) {
            this.messages = newMessages;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SupportChatActivity.ChatMessage message = messages.get(position);
            String prefix = message.senderId.equals(adminId) ? "Tú: " : "Cliente: ";
            ((TextView) holder.itemView).setText(prefix + message.text);
        }

        @Override public int getItemCount() { return messages.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(@NonNull View itemView) { super(itemView); } }
    }
}
