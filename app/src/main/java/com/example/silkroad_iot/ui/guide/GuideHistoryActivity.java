package com.example.silkroad_iot.ui.guide;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityGuideHistoryBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

public class GuideHistoryActivity extends AppCompatActivity {

    private ActivityGuideHistoryBinding b;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGuideHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Historial de Tours");
        }

        db = FirebaseFirestore.getInstance();
        loadHistory();
    }

    private void loadHistory() {
        User u = UserStore.get().getLogged();
        String email = (u != null ? u.getEmail() : null);
        if (email == null) {
            b.txtFullHistory.setText("Sin datos");
            b.txtMonthlyStats.setText("Sin estadísticas.");
            return;
        }

        db.collection("guias").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        b.txtFullHistory.setText("No se encontró el guía");
                        b.txtMonthlyStats.setText("Sin estadísticas.");
                        return;
                    }
                    String guideDocId = snap.getDocuments().get(0).getId();

                    db.collection("guias").document(guideDocId).collection("historial")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(hsnap -> {
                                StringBuilder sbActive = new StringBuilder();
                                StringBuilder sbFinished = new StringBuilder();

                                // Para estadísticas del mes
                                Calendar now = Calendar.getInstance();
                                int yearNow = now.get(Calendar.YEAR);
                                int monthNow = now.get(Calendar.MONTH);

                                int totalMes = 0;
                                int enCursoMes = 0;
                                int finalizadosMes = 0;
                                double ingresosMes = 0.0;

                                DateFormat df = DateFormat.getDateTimeInstance(
                                        DateFormat.SHORT,
                                        DateFormat.SHORT,
                                        Locale.getDefault()
                                );

                                for (QueryDocumentSnapshot d : hsnap) {
                                    String tour   = d.getString("tourName");
                                    String empresa= d.getString("companyName");
                                    Double pago   = d.getDouble("payment");
                                    Double rating = d.getDouble("rating");
                                    Long ts       = d.getLong("timestamp");
                                    String status = d.getString("status");

                                    java.util.Date date = ts == null ? null : new java.util.Date(ts);
                                    String dateStr = (date == null ? "—" : df.format(date));
                                    String pagoStr = (pago == null ? "—" :
                                            String.format(Locale.getDefault(), "S/ %.2f", pago));
                                    String ratingStr = (rating == null ? "—" :
                                            String.format(Locale.getDefault(),"%.1f", rating));

                                    String statusPretty;
                                    if (status == null) statusPretty = "—";
                                    else if ("EN_CURSO".equalsIgnoreCase(status)) statusPretty = "EN CURSO";
                                    else if ("FINALIZADO".equalsIgnoreCase(status)) statusPretty = "FINALIZADO";
                                    else statusPretty = status.toUpperCase(Locale.getDefault());

                                    String block = "🗓️ " + dateStr +
                                            "\n📍 " + (tour == null ? "—" : tour) +
                                            "\n🏢 " + (empresa == null ? "—" : empresa) +
                                            "\n💰 " + pagoStr +
                                            "\n⭐ " + ratingStr +
                                            "\n📌 Estado: " + statusPretty +
                                            "\n\n";

                                    // Separar en curso vs finalizados para mostrar más claro
                                    if ("EN_CURSO".equalsIgnoreCase(status)) {
                                        sbActive.append(block);
                                    } else if ("FINALIZADO".equalsIgnoreCase(status)) {
                                        sbFinished.append(block);
                                    } else {
                                        // Si no tiene estado claro, lo mando abajo
                                        sbFinished.append(block);
                                    }

                                    // Estadísticas del mes actual
                                    if (date != null) {
                                        Calendar c = Calendar.getInstance();
                                        c.setTime(date);
                                        int y = c.get(Calendar.YEAR);
                                        int m = c.get(Calendar.MONTH);
                                        if (y == yearNow && m == monthNow) {
                                            totalMes++;
                                            if ("EN_CURSO".equalsIgnoreCase(status)) {
                                                enCursoMes++;
                                            } else if ("FINALIZADO".equalsIgnoreCase(status)) {
                                                finalizadosMes++;
                                            }
                                            if (pago != null) ingresosMes += pago;
                                        }
                                    }
                                }

                                // Construir texto principal
                                StringBuilder full = new StringBuilder();
                                if (sbActive.length() > 0) {
                                    full.append("✅ Tours en curso\n\n");
                                    full.append(sbActive);
                                }
                                if (sbFinished.length() > 0) {
                                    if (full.length() > 0) full.append("──────────────\n\n");
                                    full.append("📚 Tours finalizados\n\n");
                                    full.append(sbFinished);
                                }

                                if (full.length() == 0) {
                                    full.append("Sin historial.");
                                }

                                b.txtFullHistory.setText(full.toString());

                                // Estadísticas del mes
                                String stats = "Mes actual:\n"
                                        + "• Total de tours: " + totalMes + "\n"
                                        + "• En curso: " + enCursoMes + "\n"
                                        + "• Finalizados: " + finalizadosMes + "\n"
                                        + "• Ingresos estimados: S/ "
                                        + String.format(Locale.getDefault(), "%.2f", ingresosMes);

                                b.txtMonthlyStats.setText(stats);
                            })
                            .addOnFailureListener(e -> {
                                b.txtFullHistory.setText("Error cargando historial.");
                                b.txtMonthlyStats.setText("Sin estadísticas.");
                            });
                })
                .addOnFailureListener(e -> {
                    b.txtFullHistory.setText("Error buscando guía.");
                    b.txtMonthlyStats.setText("Sin estadísticas.");
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}