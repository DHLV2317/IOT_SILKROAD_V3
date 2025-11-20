package com.example.silkroad_iot.ui.guide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.TourFB;
import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TourOfferAdapter extends RecyclerView.Adapter<TourOfferAdapter.TourOfferViewHolder> {

    private final List<TourFB> tourOfferList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public TourOfferAdapter(List<TourFB> tourOfferList) {
        this.tourOfferList = tourOfferList;
    }

    @NonNull
    @Override
    public TourOfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_offer, parent, false);
        return new TourOfferViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TourOfferViewHolder holder, int position) {
        TourFB tour = tourOfferList.get(position);

        // ==== Título del tour ====
        String tourTitle = tour.getNombre();
        if (tourTitle == null || tourTitle.trim().isEmpty()) {
            tourTitle = tour.getName();
        }
        if (tourTitle == null || tourTitle.trim().isEmpty()) {
            tourTitle = "Tour sin nombre";
        }
        holder.tvTourName.setText(tourTitle);

        // ==== Empresa / Ciudad (fallback) ====
        String fallbackCompany = tour.getCiudad();
        if (fallbackCompany == null || fallbackCompany.trim().isEmpty()) {
            fallbackCompany = "Empresa / Ciudad no definida";
        }
        holder.tvCompanyName.setText(fallbackCompany);

        String empresaId = tour.getEmpresaId();
        if (empresaId != null && !empresaId.trim().isEmpty()) {
            db.collection("empresas")
                    .document(empresaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        String empName = doc.getString("nombre");
                        if (empName == null || empName.trim().isEmpty()) return;

                        int currentPos = holder.getBindingAdapterPosition();
                        if (currentPos == RecyclerView.NO_POSITION) return;
                        if (currentPos >= tourOfferList.size()) return;
                        TourFB currentTour = tourOfferList.get(currentPos);

                        if (tour.getId() != null &&
                                tour.getId().equals(currentTour.getId())) {
                            holder.tvCompanyName.setText(empName);
                        }
                    });
        }

        // ==== Pago propuesto ====
        Double pay = tour.getPaymentProposal();
        String payText = (pay == null
                ? "Pago a negociar"
                : String.format(Locale.getDefault(), "Pago propuesto: S/ %.2f", pay));
        holder.tvPayment.setText(payText);

        // ====================== ACEPTAR OFERTA ======================
        String finalTourTitle = tourTitle;
        holder.btnAcceptOffer.setOnClickListener(v -> {
            if (tour.getId() == null || tour.getId().trim().isEmpty()) {
                Toast.makeText(v.getContext(), "Tour sin ID de documento", Toast.LENGTH_SHORT).show();
                return;
            }

            User logged = UserStore.get().getLogged();
            if (logged == null) {
                Toast.makeText(v.getContext(),
                        "Sesión expirada. Vuelve a iniciar sesión.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String guideEmail = logged.getEmail();
            if (guideEmail == null || guideEmail.trim().isEmpty()) {
                Toast.makeText(v.getContext(),
                        "Guía sin email. No se puede asignar.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            holder.btnAcceptOffer.setEnabled(false);
            holder.btnRejectOffer.setEnabled(false);

            String companyNameForHist = holder.tvCompanyName.getText().toString();

            // 1) Buscar documento del guía por email
            db.collection("guias")
                    .whereEqualTo("email", guideEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(q -> {
                        if (q.isEmpty()) {
                            holder.btnAcceptOffer.setEnabled(true);
                            holder.btnRejectOffer.setEnabled(true);
                            Toast.makeText(v.getContext(),
                                    "No se encontró el perfil de guía en 'guias'.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot guideDoc = q.getDocuments().get(0);
                        String guideDocId = guideDoc.getId();
                        String nombre = guideDoc.getString("nombre");
                        String apellidos = guideDoc.getString("apellidos");
                        Boolean ocupado = guideDoc.getBoolean("ocupado");
                        String tourActualId = guideDoc.getString("tourActualId");
                        if (tourActualId == null || tourActualId.trim().isEmpty()) {
                            // compatibilidad con campo viejo
                            tourActualId = guideDoc.getString("tourIdAsignado");
                        }
                        String guideLangs = guideDoc.getString("langs");
                        if (guideLangs == null || guideLangs.trim().isEmpty()) {
                            guideLangs = guideDoc.getString("idiomas");
                        }

                        String fullName;
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            if (apellidos != null && !apellidos.trim().isEmpty()) {
                                fullName = nombre + " " + apellidos;
                            } else {
                                fullName = nombre;
                            }
                        } else {
                            fullName = guideEmail;
                        }

                        // 2) Si el guía está ocupado y tiene tour actual, validar choque de fechas
                        boolean ocupadoSafe = (ocupado != null && ocupado);
                        if (ocupadoSafe && tourActualId != null && !tourActualId.trim().isEmpty()) {
                            String finalTourActualId = tourActualId;
                            String finalGuideLangs = guideLangs;
                            String finalFullName = fullName;

                            db.collection("tours")
                                    .document(tourActualId)
                                    .get()
                                    .addOnSuccessListener(currTourDoc -> {
                                        Date currFrom = currTourDoc.getDate("dateFrom");
                                        Date currTo = currTourDoc.getDate("dateTo");
                                        Date newFrom = tour.getDateFrom();
                                        Date newTo = tour.getDateTo();

                                        // Si hay choque de fechas, no puede aceptar
                                        if (hasDateConflict(currFrom, currTo, newFrom, newTo)) {
                                            holder.btnAcceptOffer.setEnabled(true);
                                            holder.btnRejectOffer.setEnabled(true);
                                            Toast.makeText(v.getContext(),
                                                    "No puedes aceptar este tour: se superpone con tu tour actual.",
                                                    Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        // Si no hay conflicto, se puede asignar el nuevo tour
                                        assignTourToGuide(
                                                holder,
                                                v,
                                                tour,
                                                finalTourTitle,
                                                companyNameForHist,
                                                guideDocId,
                                                finalFullName,
                                                finalGuideLangs,
                                                finalTourActualId
                                        );
                                    })
                                    .addOnFailureListener(e -> {
                                        holder.btnAcceptOffer.setEnabled(true);
                                        holder.btnRejectOffer.setEnabled(true);
                                        Toast.makeText(v.getContext(),
                                                "Error validando fechas del tour actual.",
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Guía libre -> asignar directamente
                            assignTourToGuide(
                                    holder,
                                    v,
                                    tour,
                                    finalTourTitle,
                                    companyNameForHist,
                                    guideDocId,
                                    fullName,
                                    guideLangs,
                                    null
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.btnAcceptOffer.setEnabled(true);
                        holder.btnRejectOffer.setEnabled(true);
                        Toast.makeText(v.getContext(),
                                "Error buscando guía: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        // ====================== RECHAZAR OFERTA ======================
        String finalTourTitle1 = tourTitle;
        holder.btnRejectOffer.setOnClickListener(v -> {
            if (tour.getId() == null || tour.getId().trim().isEmpty()) {
                Toast.makeText(v.getContext(), "Tour sin ID de documento", Toast.LENGTH_SHORT).show();
                return;
            }

            holder.btnAcceptOffer.setEnabled(false);
            holder.btnRejectOffer.setEnabled(false);

            Toast.makeText(v.getContext(),
                    "Has rechazado el tour: " + finalTourTitle1,
                    Toast.LENGTH_SHORT).show();

            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                tourOfferList.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tourOfferList.size();
    }

    // ====================== HELPERS ======================

    /**
     * Devuelve true si los rangos [from1, to1] y [from2, to2] se superponen.
     * Si falta alguna fecha, por seguridad asumimos que hay conflicto.
     */
    private boolean hasDateConflict(Date from1, Date to1, Date from2, Date to2) {
        if (from1 == null || to1 == null || from2 == null || to2 == null) {
            // si no hay info clara, asumimos conflicto para no romper la lógica
            return true;
        }
        // overlap clásico: A.start <= B.end && A.end >= B.start
        return !from1.after(to2) && !to1.before(from2);
    }

    /**
     * Lógica central para asignar el tour al guía:
     * - actualiza tour
     * - actualiza guía
     * - registra historial
     * - quita card de la lista
     */
    private void assignTourToGuide(
            @NonNull TourOfferViewHolder holder,
            @NonNull View v,
            @NonNull TourFB tour,
            @NonNull String finalTourTitle,
            @NonNull String companyNameForHist,
            @NonNull String guideDocId,
            @NonNull String fullName,
            String guideLangs,
            String previousTourId
    ) {

        if (tour.getId() == null || tour.getId().trim().isEmpty()) {
            Toast.makeText(v.getContext(), "Tour sin ID de documento", Toast.LENGTH_SHORT).show();
            holder.btnAcceptOffer.setEnabled(true);
            holder.btnRejectOffer.setEnabled(true);
            return;
        }

        // 1) Actualizar TOUR
        Map<String, Object> updTour = new HashMap<>();
        updTour.put("assignedGuideId", guideDocId);
        updTour.put("assignedGuideName", fullName);
        updTour.put("paymentProposal", tour.getPaymentProposal());
        updTour.put("status", "EN_CURSO");   // capa lógica
        updTour.put("estado", "en_curso");   // legacy
        updTour.put("publicado", false);     // ya no es oferta

        // 🔹 Copiamos los idiomas del guía al tour
        if (guideLangs != null && !guideLangs.trim().isEmpty()) {
            updTour.put("langs", guideLangs);
            updTour.put("idiomas", guideLangs);
        }

        db.collection("tours")
                .document(tour.getId())
                .update(updTour)
                .addOnSuccessListener(unused -> {

                    // 2) Actualizar estado del guía
                    Map<String, Object> updGuide = new HashMap<>();
                    updGuide.put("estado", "ocupado");
                    updGuide.put("ocupado", true);
                    updGuide.put("tourActual", finalTourTitle);
                    updGuide.put("tourIdAsignado", tour.getId());
                    updGuide.put("tourActualId", tour.getId());

                    db.collection("guias")
                            .document(guideDocId)
                            .update(updGuide);

                    // 3) Registrar historial del guía
                    Map<String, Object> hist = new HashMap<>();
                    hist.put("tourId", tour.getId());
                    hist.put("tourName", finalTourTitle);
                    hist.put("companyName", companyNameForHist);
                    hist.put("payment", tour.getPaymentProposal());
                    hist.put("status", "EN_CURSO");
                    hist.put("estado", "asignado");
                    hist.put("timestamp", System.currentTimeMillis());
                    // rating se añadirá luego cuando el tour se califique

                    db.collection("guias")
                            .document(guideDocId)
                            .collection("historial")
                            .add(hist);

                    Toast.makeText(v.getContext(),
                            "Has aceptado el tour: " + finalTourTitle,
                            Toast.LENGTH_SHORT).show();

                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        tourOfferList.remove(pos);
                        notifyItemRemoved(pos);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.btnAcceptOffer.setEnabled(true);
                    holder.btnRejectOffer.setEnabled(true);
                    Toast.makeText(v.getContext(),
                            "Error al aceptar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ====================== VIEW HOLDER ======================

    static class TourOfferViewHolder extends RecyclerView.ViewHolder {

        TextView tvTourName, tvPayment, tvCompanyName;
        Button btnAcceptOffer, btnRejectOffer;

        public TourOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvPayment = itemView.findViewById(R.id.tvPayment);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            btnAcceptOffer = itemView.findViewById(R.id.btnAcceptOffer);
            btnRejectOffer = itemView.findViewById(R.id.btnRejectOffer);
        }
    }
}