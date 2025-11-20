package com.example.silkroad_iot.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

/** Representa una empresa en Firestore (cliente + admin). */
@IgnoreExtraProperties
public class EmpresaFb implements Serializable {

    private String id;
    private String nombre;

    // Imagen genérica (compatibilidad con código anterior)
    private String imagen;

    // NUEVOS: logo y banner independientes
    @PropertyName("logoUrl")     private String logoUrl;
    @PropertyName("bannerUrl")   private String bannerUrl;

    private String email;
    private String telefono;
    private String direccion;
    private double lat;
    private double lng;

    public EmpresaFb() {}

    public EmpresaFb(String id, String nombre, String imagen,
                     String logoUrl, String bannerUrl,
                     String email, String telefono, String direccion,
                     double lat, double lng) {
        this.id = id;
        this.nombre = nombre;
        this.imagen = imagen;
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.lat = lat;
        this.lng = lng;
    }

    // ==== Getters/Setters ====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("nombre")
    public String getNombre() { return nombre; }
    @PropertyName("nombre")
    public void setNombre(String nombre) { this.nombre = nombre; }

    @PropertyName("imagen")
    public String getImagen() { return imagen; }
    @PropertyName("imagen")
    public void setImagen(String imagen) { this.imagen = imagen; }

    @PropertyName("logoUrl")
    public String getLogoUrl() { return logoUrl; }
    @PropertyName("logoUrl")
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    @PropertyName("bannerUrl")
    public String getBannerUrl() { return bannerUrl; }
    @PropertyName("bannerUrl")
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    @PropertyName("email")
    public String getEmail() { return email; }
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("telefono")
    public String getTelefono() { return telefono; }
    @PropertyName("telefono")
    public void setTelefono(String telefono) { this.telefono = telefono; }

    @PropertyName("direccion")
    public String getDireccion() { return direccion; }
    @PropertyName("direccion")
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @PropertyName("lat")
    public double getLat() { return lat; }
    @PropertyName("lat")
    public void setLat(double lat) { this.lat = lat; }

    @PropertyName("lng")
    public double getLng() { return lng; }
    @PropertyName("lng")
    public void setLng(double lng) { this.lng = lng; }

    // ===== Helpers para la UI =====
    @Exclude
    public String getDisplayLogo() {
        if (logoUrl != null && !logoUrl.isEmpty()) return logoUrl;
        if (imagen != null && !imagen.isEmpty()) return imagen;
        return null;
    }

    @Exclude
    public String getDisplayBanner() {
        if (bannerUrl != null && !bannerUrl.isEmpty()) return bannerUrl;
        return null;
    }

    @Override
    public String toString() {
        return "EmpresaFb{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", imagen='" + imagen + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", bannerUrl='" + bannerUrl + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                ", direccion='" + direccion + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                '}';
    }
}