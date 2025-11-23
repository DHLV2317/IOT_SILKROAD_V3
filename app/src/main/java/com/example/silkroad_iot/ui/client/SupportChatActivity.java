package com.example.silkroad_iot.ui.client;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupportChatActivity extends AppCompatActivity {

    private enum ChatState {
        AWAITING_MENU_CHOICE,
        AWAITING_TICKET_CONTEXT,
        AWAITING_TICKET_SELECTION
    }

    private final List<String> messages = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private RecyclerView rvMessages;

    private ChatState currentState = ChatState.AWAITING_MENU_CHOICE;
    private List<QueryDocumentSnapshot> listedTickets = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat de soporte");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        rvMessages = findViewById(R.id.rvMessages);
        EditText input = findViewById(R.id.inputMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        // Adapter
        adapter = new SimpleTextAdapter(messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Mensaje inicial con el menú
        showMenu();

        // Envío de mensajes
        btnSend.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) return;

            addMessage("Tú: " + txt);
            input.setText("");

            switch (currentState) {
                case AWAITING_MENU_CHOICE:
                    handleMenuChoice(txt);
                    break;
                case AWAITING_TICKET_CONTEXT:
                    handleTicketContext(txt);
                    break;
                case AWAITING_TICKET_SELECTION:
                    handleTicketSelection(txt);
                    break;
            }
        });
    }

    private void handleMenuChoice(String choice) {
        if ("1".equals(choice)) {
            listExistingTickets();
        } else if ("2".equals(choice)) {
            addMessage("Soporte: Por favor, describa brevemente su problema para crear un nuevo chat.");
            currentState = ChatState.AWAITING_TICKET_CONTEXT;
        } else {
            addMessage("Soporte: Opción no válida. Por favor, intente de nuevo.");
            showMenu();
        }
    }

    private void handleTicketContext(String context) {
        saveNewChat(context);
    }

    private void handleTicketSelection(String selection) {
        try {
            int choiceIndex = Integer.parseInt(selection) - 1;
            if (choiceIndex >= 0 && choiceIndex < listedTickets.size()) {
                QueryDocumentSnapshot selectedTicket = listedTickets.get(choiceIndex);
                String ticketId = selectedTicket.getString("idticket");
                addMessage("Soporte: Ha seleccionado el ticket " + ticketId + ". Próximamente podrá chatear con soporte aquí.");
                resetToMenu();
            } else {
                addMessage("Soporte: Selección no válida. Por favor, elija un número de la lista.");
                resetToMenu();
            }
        } catch (NumberFormatException e) {
            addMessage("Soporte: Por favor, ingrese solo el número del ticket que desea seleccionar.");
            resetToMenu();
        }
    }

    private void listExistingTickets() {
        if (currentUser == null) {
            addMessage("Soporte: Debe iniciar sesión para ver sus tickets.");
            resetToMenu();
            return;
        }
        addMessage("Soporte: Consultando sus tickets existentes...");
        db.collection("chats")
                .whereEqualTo("idusuario", currentUser.getUid())
                .orderBy("fecha_creacion", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        listedTickets.clear();
                        StringBuilder ticketsList = new StringBuilder("Soporte: Estos son sus tickets:\n");
                        int i = 1;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            listedTickets.add(document);
                            String idticket = document.getString("idticket");
                            String contexto = document.getString("contexto");
                            ticketsList.append(i++).append(") Ticket #").append(idticket).append(" - ").append(contexto).append("\n");
                        }
                        ticketsList.append("\nIngrese el número del ticket que desea seleccionar.");
                        addMessage(ticketsList.toString());
                        currentState = ChatState.AWAITING_TICKET_SELECTION;
                    } else if (task.isSuccessful()) {
                        addMessage("Soporte: No hemos encontrado tickets existentes para su cuenta.");
                        resetToMenu();
                    } else {
                        addMessage("Soporte: Hubo un error al consultar sus tickets.");
                        resetToMenu();
                    }
                });
    }

    private void saveNewChat(String contexto) {
        if (currentUser == null) {
            addMessage("Soporte: Debe iniciar sesión para crear un chat.");
            resetToMenu();
            return;
        }

        String ticketId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> chat = new HashMap<>();
        chat.put("idticket", ticketId);
        chat.put("idusuario", currentUser.getUid());
        chat.put("idsoporte", "");
        chat.put("fecha_creacion", new Date());
        chat.put("contexto", contexto);

        db.collection("chats").add(chat)
                .addOnSuccessListener(documentReference -> {
                    addMessage("Soporte: ¡Chat de soporte iniciado con éxito! ID de Ticket: " + ticketId);
                    resetToMenu();
                })
                .addOnFailureListener(e -> {
                    addMessage("Soporte: Hubo un error al iniciar el chat.");
                    resetToMenu();
                });
    }

    private void showMenu() {
        String menu = "Soporte: Ingrese el número de la opción que desea:\n1) Revisar tickets existentes\n2) Crear nuevo ticket";
        addMessage(menu);
    }

    private void resetToMenu() {
        currentState = ChatState.AWAITING_MENU_CHOICE;
        addMessage("----------");
        showMenu();
    }

    private void addMessage(String message) {
        messages.add(message.replace("\n", System.getProperty("line.separator")));
        adapter.notifyDataSetChanged();
        rvMessages.scrollToPosition(messages.size() - 1);
    }
}
