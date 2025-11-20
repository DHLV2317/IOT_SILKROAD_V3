package com.example.silkroad_iot.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

@IgnoreExtraProperties
public class GuideFb implements Serializable {

    private String id;

    private String nombre;
    private String apellidos;
    private String langs;      // campo interno
    private String estado;
    private String email;
    private String telefono;
    private String direccion;

    private String fotoUrl;

    private String tourActual;     // texto legible tipo "Tour X"
    private String tourIdAsignado; // id de tour si está ocupado

    private List<String> historial;

    // ---------- 🔵 GEOLOCALIZACIÓN REAL ----------
    private Double latActual;
    private Double lngActual;
    private Long lastUpdate;

    // Aprobación en módulo admin
    private boolean guideApproved;
    private String guideApprovalStatus;

    // 🔵 Campos que usamos en Firestore en colección "guias"
    // aprobado / activo / ocupado / tourActualId
    private Boolean aprobado;
    private Boolean activo;
    private Boolean ocupado;
    private String tourActualId;

    public GuideFb() {}

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("nombre")
    public String getNombre() { return nombre; }
    @PropertyName("nombre")
    public void setNombre(String nombre) { this.nombre = nombre; }

    @PropertyName("apellidos")
    public String getApellidos() { return apellidos; }
    @PropertyName("apellidos")
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    // ===== idiomas / langs / idiomas (compatibilidad) =====
    @PropertyName("langs")
    public String getLangs() { return langs; }
    @PropertyName("langs")
    public void setLangs(String langs) { this.langs = langs; }

    // Firestore puede guardar "idiomas": lo mapeamos al mismo campo
    @PropertyName("idiomas")
    public String getIdiomas() { return langs; }
    @PropertyName("idiomas")
    public void setIdiomas(String idiomas) { this.langs = idiomas; }

    @PropertyName("estado")
    public String getEstado() { return estado; }
    @PropertyName("estado")
    public void setEstado(String estado) { this.estado = estado; }

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

    @PropertyName("fotoUrl")
    public String getFotoUrl() { return fotoUrl; }
    @PropertyName("fotoUrl")
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    @PropertyName("tourActual")
    public String getTourActual() { return tourActual; }
    @PropertyName("tourActual")
    public void setTourActual(String tourActual) { this.tourActual = tourActual; }

    @PropertyName("tourIdAsignado")
    public String getTourIdAsignado() { return tourIdAsignado; }
    @PropertyName("tourIdAsignado")
    public void setTourIdAsignado(String tourIdAsignado) { this.tourIdAsignado = tourIdAsignado; }

    @PropertyName("historial")
    public List<String> getHistorial() { return historial; }
    @PropertyName("historial")
    public void setHistorial(List<String> historial) { this.historial = historial; }

    // 🔵 GEO
    @PropertyName("latActual")
    public Double getLatActual() { return latActual; }
    @PropertyName("latActual")
    public void setLatActual(Double latActual) { this.latActual = latActual; }

    @PropertyName("lngActual")
    public Double getLngActual() { return lngActual; }
    @PropertyName("lngActual")
    public void setLngActual(Double lngActual) { this.lngActual = lngActual; }

    @PropertyName("lastUpdate")
    public Long getLastUpdate() { return lastUpdate; }
    @PropertyName("lastUpdate")
    public void setLastUpdate(Long lastUpdate) { this.lastUpdate = lastUpdate; }

    @PropertyName("guideApproved")
    public boolean isGuideApproved() { return guideApproved; }
    @PropertyName("guideApproved")
    public void setGuideApproved(boolean guideApproved) { this.guideApproved = guideApproved; }

    @PropertyName("guideApprovalStatus")
    public String getGuideApprovalStatus() { return guideApprovalStatus; }
    @PropertyName("guideApprovalStatus")
    public void setGuideApprovalStatus(String guideApprovalStatus) { this.guideApprovalStatus = guideApprovalStatus; }

    // ====== flags simples que usamos en queries (aprobado / activo / ocupado) ======

    @PropertyName("aprobado")
    public Boolean getAprobado() { return aprobado; }
    @PropertyName("aprobado")
    public void setAprobado(Boolean aprobado) { this.aprobado = aprobado; }

    @PropertyName("activo")
    public Boolean getActivo() { return activo; }
    @PropertyName("activo")
    public void setActivo(Boolean activo) { this.activo = activo; }

    @PropertyName("ocupado")
    public Boolean getOcupado() { return ocupado; }
    @PropertyName("ocupado")
    public void setOcupado(Boolean ocupado) { this.ocupado = ocupado; }

    @PropertyName("tourActualId")
    public String getTourActualId() { return tourActualId; }
    @PropertyName("tourActualId")
    public void setTourActualId(String tourActualId) { this.tourActualId = tourActualId; }

    // ===== Helpers para UI =====

    @Exclude
    public String getDisplayName() {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellidos == null ? "" : apellidos.trim();
        String full = (n + " " + a).trim();
        return full.isEmpty() ? "Guía" : full;
    }

    @Exclude
    public String getDisplayLangs() {
        if (langs != null && !langs.trim().isEmpty()) return langs;
        return "—";
    }

    @Exclude
    public boolean isAprobadoSafe() {
        if (aprobado != null) return aprobado;
        return guideApproved;
    }

    @Exclude
    public boolean isActivoSafe() {
        return activo == null || activo; // si es null asumimos activo
    }

    @Exclude
    public boolean isOcupadoSafe() {
        return ocupado != null && ocupado;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "GuideFb{id='%s', nombre='%s', estados=%s, aprobado=%s}",
                id, getDisplayName(), estado, isAprobadoSafe());
    }
}