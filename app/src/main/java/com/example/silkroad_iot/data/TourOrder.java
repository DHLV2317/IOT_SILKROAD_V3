package com.example.silkroad_iot.data;

import java.io.Serializable;
import java.util.Date;

public class TourOrder implements Serializable {

    public enum Status {
        RESERVADO,
        EN_CURSO,
        CANCELADO,
        COMPLETADO
    }
    public Tour tour;
    public int quantity;
    public Date date; // fecha de compra
    public String userEmail;
    public Date createdAt;




    public Status status= null;

    public TourOrder(Tour tour, int quantity, Date date, String userEmail, Status status) {
        this.tour = tour;
        this.quantity = quantity;
        this.date = date;
        this.userEmail = userEmail;
        this.createdAt = new Date();
        this.status= status;

    }
}



