package com.example.silkroad_iot.data;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class CardFB implements Serializable {

    private String id;
    private String userEmail;
    private String alias;
    private String last4;
    private String brand;   // VISA / MASTERCARD / AMEX / etc
    private String type;    // CREDIT / DEBIT
    private double balance;
    private String currency;

    public CardFB() {}

    public CardFB(String id, String userEmail, String alias,
                  String last4, String brand, String type,
                  double balance, String currency) {
        this.id = id;
        this.userEmail = userEmail;
        this.alias = alias;
        this.last4 = last4;
        this.brand = brand;
        this.type = type;
        this.balance = balance;
        this.currency = currency;
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("userEmail")
    public String getUserEmail() { return userEmail; }
    @PropertyName("userEmail")
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    @PropertyName("alias")
    public String getAlias() { return alias; }
    @PropertyName("alias")
    public void setAlias(String alias) { this.alias = alias; }

    @PropertyName("last4")
    public String getLast4() { return last4; }
    @PropertyName("last4")
    public void setLast4(String last4) { this.last4 = last4; }

    @PropertyName("brand")
    public String getBrand() { return brand; }
    @PropertyName("brand")
    public void setBrand(String brand) { this.brand = brand; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("balance")
    public double getBalance() { return balance; }
    @PropertyName("balance")
    public void setBalance(double balance) { this.balance = balance; }

    @PropertyName("currency")
    public String getCurrency() { return currency; }
    @PropertyName("currency")
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isCredit() {
        return "CREDIT".equalsIgnoreCase(type);
    }

    public boolean isDebit() {
        return "DEBIT".equalsIgnoreCase(type);
    }
}