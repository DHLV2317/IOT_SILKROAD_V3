package com.example.silkroad_iot.data;

import java.io.Serializable;

public class Stop implements Serializable {
    public String name;
    public String address;
    public String time;     // Ejemplo: "20 min"
    public double cost;

    public Stop() {
        this.name = name;
        this.address = address;
        this.time = time;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
