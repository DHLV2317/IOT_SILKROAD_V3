package com.example.silkroad_iot.ui.client;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import com.example.silkroad_iot.data.TourHistorialFB;
import com.example.silkroad_iot.data.TourOrder;

import java.util.ArrayList;
import java.util.List;

public class OrderStore {
    private static final List<TourHistorialFB> orders = new ArrayList<>();

    public static void addOrder(TourHistorialFB order) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tours_history")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    // También puedes guardar localmente si quieres mantenerlo en memoria
                    orders.add(order);
                    Log.d("FIRESTORE", "Historial agregado con ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al agregar historial", e);
                });
    }

    public static void getOrdersByUser(String email, OnOrdersLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tours_history")
                .whereEqualTo("id_usuario", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TourHistorialFB> result = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TourHistorialFB order = doc.toObject(TourHistorialFB.class);
                        result.add(order);
                    }
                    listener.onOrdersLoaded(result);
                })
                .addOnFailureListener(e -> {
                    listener.onOrdersLoaded(new ArrayList<>()); // vacío si falla
                });
    }

    public interface OnOrdersLoadedListener {
        void onOrdersLoaded(List<TourHistorialFB> orders);
    }


}

