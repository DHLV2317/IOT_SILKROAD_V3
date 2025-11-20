package com.example.silkroad_iot.data;

import java.io.Serializable;

/**
 * Wrapper para juntar la reserva (historial) con los datos del tour.
 * Además expone campos simples que el PdfReportUtil leerá vía reflexión.
 */
public class ReservaWithTour implements Serializable {

    private final TourHistorialFB reserva;
    private final TourFB tour;

    // Campos auxiliares para PDF (se rellenan en el Adapter)
    public String tourName;
    public String clientName;
    public String status;
    public Double total;
    public Long date;     // millis since epoch
    public Float rating;  // opcional

    public ReservaWithTour(TourHistorialFB reserva, TourFB tour) {
        this.reserva = reserva;
        this.tour = tour;
    }

    public TourHistorialFB getReserva() { return reserva; }
    public TourFB getTour() { return tour; }
}