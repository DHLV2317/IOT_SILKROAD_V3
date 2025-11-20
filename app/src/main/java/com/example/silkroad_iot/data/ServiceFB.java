package com.example.silkroad_iot.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

@IgnoreExtraProperties
public class ServiceFB implements Serializable {

    // Campos reales en Firestore
    private String name;       // nombre del servicio/adicional
    private Boolean included;  // true = incluido para todos (sin costo extra)
    private Double price;      // precio por persona (o por unidad)

    // (Opcional) compatibilidad si en algún documento se guardó como "precio"
    private Double precioLegacy;

    public ServiceFB() {}

    public ServiceFB(String name, Boolean included, Double price) {
        this.name = name;
        this.included = included;
        this.price = price;
    }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("included")
    public Boolean getIncluded() { return included; }
    @PropertyName("included")
    public void setIncluded(Boolean included) { this.included = included; }

    @PropertyName("price")
    public Double getPrice() { return price; }
    @PropertyName("price")
    public void setPrice(Double price) { this.price = price; }

    // ---- Compatibilidad con posible campo "precio" en Firestore ----
    @PropertyName("precio")
    public Double getPrecioLegacy() { return precioLegacy; }
    @PropertyName("precio")
    public void setPrecioLegacy(Double precioLegacy) { this.precioLegacy = precioLegacy; }

    // ================== HELPERS PARA LA UI Y CÁLCULOS ==================

    /** Nombre amigable para mostrar en la UI de extras */
    @Exclude
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) return name.trim();
        return "Extra";
    }

    /** Precio seguro por persona/unidad (usa price o precioLegacy si existe) */
    @Exclude
    public double getPricePerPersonSafe() {
        if (price != null && price > 0) return price;
        if (precioLegacy != null && precioLegacy > 0) return precioLegacy;
        return 0.0;
    }

    /** Indica si es un servicio incluido sin costo extra */
    @Exclude
    public boolean isIncludedSafe() {
        return included != null && included;
    }
}