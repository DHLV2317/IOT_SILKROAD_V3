package com.example.silkroad_iot.data;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.Date;

@IgnoreExtraProperties
public class TourOffer implements Serializable {

    private String id;

    // Tour / empresa
    private String tourId;
    private String tourName;
    private String companyId;
    private String companyName;

    // Guía
    private String guideId;
    private String guideEmail;
    private String guideName; // opcional, útil para admin

    // Pago propuesto
    private Double payment;

    // PENDING / ACCEPTED / REJECTED
    private String status;

    private Date createdAt;

    public TourOffer() { }

    // Constructor simple para pruebas locales si quieres
    public TourOffer(String tourName, String payment, String companyName) {
        this.tourName = tourName;
        try {
            this.payment = Double.parseDouble(payment);
        } catch (Exception e) {
            this.payment = 0.0;
        }
        this.companyName = companyName;
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("tourId")
    public String getTourId() { return tourId; }
    @PropertyName("tourId")
    public void setTourId(String tourId) { this.tourId = tourId; }

    @PropertyName("tourName")
    public String getTourName() { return tourName; }
    @PropertyName("tourName")
    public void setTourName(String tourName) { this.tourName = tourName; }

    @PropertyName("companyId")
    public String getCompanyId() { return companyId; }
    @PropertyName("companyId")
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    @PropertyName("companyName")
    public String getCompanyName() { return companyName; }
    @PropertyName("companyName")
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    @PropertyName("guideId")
    public String getGuideId() { return guideId; }
    @PropertyName("guideId")
    public void setGuideId(String guideId) { this.guideId = guideId; }

    @PropertyName("guideEmail")
    public String getGuideEmail() { return guideEmail; }
    @PropertyName("guideEmail")
    public void setGuideEmail(String guideEmail) { this.guideEmail = guideEmail; }

    @PropertyName("guideName")
    public String getGuideName() { return guideName; }
    @PropertyName("guideName")
    public void setGuideName(String guideName) { this.guideName = guideName; }

    @PropertyName("payment")
    public Double getPayment() { return payment; }
    @PropertyName("payment")
    public void setPayment(Double payment) { this.payment = payment; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("createdAt")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("createdAt")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}