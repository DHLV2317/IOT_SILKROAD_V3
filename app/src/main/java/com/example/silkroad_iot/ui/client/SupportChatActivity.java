package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupportChatActivity extends AppCompatActivity {

    // Clase para representar un mensaje en el chat
    public static class ChatMessage {
        public String senderId;
        public String text;
        public Date timestamp;

        public ChatMessage() {}

        public ChatMessage(String senderId, String text) {
            this.senderId = senderId;
            this.text = text;
            this.timestamp = new Date();
        }
    }

    private enum ViewState {
        MENU,
        CHAT
    }

    private enum MenuState {
        AWAITING_MENU_CHOICE,
        AWAITING_TOUR_NAME,
        AWAITING_TICKET_CONTEXT,
        AWAITING_TICKET_SELECTION
    }

    private RecyclerView rvMessages;
    private EditText input;
    private ImageButton btnSend;

    private ViewState currentView = ViewState.MENU;
    private MenuState currentMenuState = MenuState.AWAITING_MENU_CHOICE;
    private List<QueryDocumentSnapshot> listedTickets = new ArrayList<>();
    private String tempTourName;
    private String currentChatId;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            if (currentView == ViewState.CHAT) {
                // Si está en un chat, vuelve al menú principal
                getSupportActionBar().setTitle("Chat de Soporte");
                showMenu();
            } else {
                // Si ya está en el menú, cierra la actividad
                finish();
            }
        });

        rvMessages = findViewById(R.id.rvMessages);
        input = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        showMenu(); // Empezar siempre en el menú

        btnSend.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) return;
            input.setText("");

            if (currentView == ViewState.CHAT) {
                sendMessage(txt);
            } else {
                handleMenuLogic(txt);
            }
        });
    }

    private void handleMenuLogic(String txt) {
        addMenuMessage("Tú: " + txt);
        switch (currentMenuState) {
            case AWAITING_MENU_CHOICE:
                handleMenuChoice(txt);
                break;
            case AWAITING_TOUR_NAME:
                handleTourName(txt);
                break;
            case AWAITING_TICKET_CONTEXT:
                handleTicketContext(txt);
                break;
            case AWAITING_TICKET_SELECTION:
                handleTicketSelection(txt);
                break;
        }
    }

    private void handleMenuChoice(String choice) {
        if ("1".equals(choice)) {
            listExistingTickets();
        } else if ("2".equals(choice)) {
            addMenuMessage("Soporte: Por favor, ingrese el nombre del tour.");
            currentMenuState = MenuState.AWAITING_TOUR_NAME;
        } else {
            addMenuMessage("Soporte: Opción no válida.");
            resetToMenu();
        }
    }

    private void handleTourName(String tourName) {
        this.tempTourName = tourName;
        addMenuMessage("Soporte: Entendido. Ahora, describa el problema.");
        currentMenuState = MenuState.AWAITING_TICKET_CONTEXT;
    }

    private void handleTicketContext(String context) {
        saveNewChat(this.tempTourName, context, "admin@demo.com");
    }

    private void handleTicketSelection(String selection) {
        try {
            int choiceIndex = Integer.parseInt(selection) - 1;
            if (choiceIndex >= 0 && choiceIndex < listedTickets.size()) {
                enterChatMode(listedTickets.get(choiceIndex).getId());
            } else {
                addMenuMessage("Soporte: Selección no válida.");
                resetToMenu();
            }
        } catch (NumberFormatException e) {
            addMenuMessage("Soporte: Ingrese solo un número.");
            resetToMenu();
        }
    }

    private void listExistingTickets() {
        if (currentUser == null) return;
        addMenuMessage("Soporte: Consultando sus tickets...");
        db.collection("chats")
                .whereEqualTo("idusuario", currentUser.getUid())
                .orderBy("fecha_creacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        listedTickets.clear();
                        // SOLUCIÓN: Iterar sobre el resultado para añadir los documentos.
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            listedTickets.add(document);
                        }
                        StringBuilder ticketsList = new StringBuilder("Soporte: Estos son sus tickets:\n");
                        for (int i = 0; i < listedTickets.size(); i++) {
                            ticketsList.append(i + 1).append(") Ticket sobre: ").append(listedTickets.get(i).getString("contexto")).append("\n");
                        }
                        ticketsList.append("\nIngrese el número del ticket que desea ver.");
                        addMenuMessage(ticketsList.toString());
                        currentMenuState = MenuState.AWAITING_TICKET_SELECTION;
                    } else {
                        addMenuMessage("Soporte: No se encontraron tickets.");
                        resetToMenu();
                    }
                });
    }

    private void saveNewChat(String tourName, String contexto, String idSoporte) {
        if (currentUser == null) return;

        Map<String, Object> chat = new HashMap<>();
        chat.put("idticket", String.valueOf(System.currentTimeMillis()));
        chat.put("idusuario", currentUser.getUid());
        chat.put("idsoporte", idSoporte);
        chat.put("fecha_creacion", new Date());
        chat.put("nombre del tour", tourName);
        chat.put("contexto", contexto);

        db.collection("chats").add(chat)
                .addOnSuccessListener(docRef -> {
                    // Guardar el primer mensaje en la sub-colección
                    ChatMessage firstMessage = new ChatMessage(currentUser.getUid(), "Ticket iniciado para el tour '" + tourName + "': " + contexto);
                    docRef.collection("messages").add(firstMessage);
                    enterChatMode(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    addMenuMessage("Soporte: Error al crear el ticket.");
                    resetToMenu();
                });
    }

    private void enterChatMode(String chatId) {
        this.currentChatId = chatId;
        this.currentView = ViewState.CHAT;
        getSupportActionBar().setTitle("Ticket #" + chatId.substring(0, 6));

        MessageAdapter messageAdapter = new MessageAdapter(new ArrayList<>(), currentUser.getUid());
        rvMessages.setAdapter(messageAdapter);

        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar mensajes.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<ChatMessage> conversation = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        conversation.add(doc.toObject(ChatMessage.class));
                    }
                    messageAdapter.updateMessages(conversation);
                    rvMessages.scrollToPosition(conversation.size() - 1);
                });
    }

    private void sendMessage(String text) {
        if (currentUser == null || currentChatId == null || currentChatId.isEmpty()) return;
        ChatMessage message = new ChatMessage(currentUser.getUid(), text);
        db.collection("chats").document(currentChatId).collection("messages").add(message);
    }

    private void addMenuMessage(String messageText) {
        SimpleTextAdapter adapter = (SimpleTextAdapter) rvMessages.getAdapter();
        if (adapter == null || !(adapter instanceof SimpleTextAdapter)) {
            adapter = new SimpleTextAdapter(new ArrayList<>());
            rvMessages.setAdapter(adapter);
        }
        adapter.addMessage(messageText);
    }

    private void showMenu() {
        currentView = ViewState.MENU;
        currentMenuState = MenuState.AWAITING_MENU_CHOICE;
        rvMessages.setAdapter(new SimpleTextAdapter(new ArrayList<>()));
        addMenuMessage("Soporte: Ingrese el número de la opción que desea:\n1) Revisar tickets existentes\n2) Crear nuevo ticket");
    }

    private void resetToMenu() {
        currentMenuState = MenuState.AWAITING_MENU_CHOICE;
        addMenuMessage("----------");
        addMenuMessage("Soporte: Ingrese el número de la opción que desea:\n1) Revisar tickets existentes\n2) Crear nuevo ticket");
    }

    // --- Adapters ---

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> messages;

        public SimpleTextAdapter(List<String> messages) {
            this.messages = messages;
        }

        public void addMessage(String message) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 8, 16, 8);
            return new ViewHolder(tv);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(messages.get(position));
        }
        @Override public int getItemCount() { return messages.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(@NonNull View itemView) { super(itemView); } }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private List<ChatMessage> messages;
        private final String currentUserId;

        public MessageAdapter(List<ChatMessage> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        public void updateMessages(List<ChatMessage> newMessages) {
            this.messages = newMessages;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            String prefix = message.senderId.equals(currentUserId) ? "Tú: " : "Soporte: ";
            ((TextView) holder.itemView).setText(prefix + message.text);
        }

        @Override
        public int getItemCount() { return messages.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(@NonNull View itemView) { super(itemView); } }
    }
}
