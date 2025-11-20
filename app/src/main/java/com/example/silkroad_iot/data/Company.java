package com.example.silkroad_iot.data;

import java.io.Serializable;
import java.util.List;

public class Company implements Serializable {
    private String n;  // nombre
    private double r;  // rating
    private String imageUrl; // nueva propiedad

    public List<Tour> tours;


    public Company(String n, double r, String imageUrl, List<Tour> tours) {
        this.n = n;
        this.r = r;
        this.imageUrl = imageUrl;
        this.tours = tours;

    }

    public String getN() {
        return n;
    }

    public double getR() {
        return r;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<Tour> getTours() {
        return tours;
    }
}