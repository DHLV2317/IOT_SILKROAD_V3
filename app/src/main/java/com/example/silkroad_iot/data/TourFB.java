package com.example.silkroad_iot.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class TourFB implements Serializable {

    private String id;

    // Nombres / visual
    private String nombre;
    private String name;
    private String description;

    // Imagen
    private String imagen;
    private String imageUrl;

    // Precio / personas
    private double precio;
    private Double price;
    private int cantidad_personas;
    private Integer people;

    // 🔹 CUPOS
    private Integer cuposTotales;
    private Integer cuposDisponibles;

    // Metadatos
    private String empresaId;
    private String ownerUid;
    private String ciudad;
    private String langs;
    private String duration;

    // Guía y pago
    private String assignedGuideName;
    private String assignedGuideId;
    private Double paymentProposal;

    // 🔹 ESTADO / PUBLICACIÓN
    private String status;
    private String estado;
    private Boolean publicado;

    // Fechas
    private Date dateFrom;
    private Date dateTo;
    private Date createdAt;   // agregado

    // Paradas embebidas
    private List<ParadaFB> paradas;

    // Paradas referenciadas RAW
    @PropertyName("id_paradas")
    private Object idParadasRaw;

    // Servicios
    private List<ServiceFB> services;

    public TourFB() {}

    /* ================== GETTERS / SETTERS ================== */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("nombre")
    public String getNombre() { return nombre; }
    @PropertyName("nombre")
    public void setNombre(String nombre) { this.nombre = nombre; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("description")
    public String getDescription() { return description; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("imagen")
    public String getImagen() { return imagen; }
    @PropertyName("imagen")
    public void setImagen(String imagen) { this.imagen = imagen; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("precio")
    public double getPrecio() { return precio; }
    @PropertyName("precio")
    public void setPrecio(double precio) { this.precio = precio; }

    @PropertyName("price")
    public Double getPrice() { return price; }
    @PropertyName("price")
    public void setPrice(Double price) { this.price = price; }

    @PropertyName("cantidad_personas")
    public int getCantidad_personas() { return cantidad_personas; }
    @PropertyName("cantidad_personas")
    public void setCantidad_personas(int cantidad_personas) { this.cantidad_personas = cantidad_personas; }

    @PropertyName("people")
    public Integer getPeople() { return people; }
    @PropertyName("people")
    public void setPeople(Integer people) { this.people = people; }

    @PropertyName("empresaId")
    public String getEmpresaId() { return empresaId; }
    @PropertyName("empresaId")
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }

    @PropertyName("ownerUid")
    public String getOwnerUid() { return ownerUid; }
    @PropertyName("ownerUid")
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    @PropertyName("ciudad")
    public String getCiudad() { return ciudad; }
    @PropertyName("ciudad")
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    @PropertyName("langs")
    public String getLangs() { return langs; }
    @PropertyName("langs")
    public void setLangs(String langs) { this.langs = langs; }

    @PropertyName("duration")
    public String getDuration() { return duration; }
    @PropertyName("duration")
    public void setDuration(String duration) { this.duration = duration; }

    @PropertyName("assignedGuideName")
    public String getAssignedGuideName() { return assignedGuideName; }
    @PropertyName("assignedGuideName")
    public void setAssignedGuideName(String assignedGuideName) { this.assignedGuideName = assignedGuideName; }

    @PropertyName("assignedGuideId")
    public String getAssignedGuideId() { return assignedGuideId; }
    @PropertyName("assignedGuideId")
    public void setAssignedGuideId(String assignedGuideId) { this.assignedGuideId = assignedGuideId; }

    @PropertyName("paymentProposal")
    public Double getPaymentProposal() { return paymentProposal; }
    @PropertyName("paymentProposal")
    public void setPaymentProposal(Double paymentProposal) { this.paymentProposal = paymentProposal; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("estado")
    public String getEstado() { return estado; }
    @PropertyName("estado")
    public void setEstado(String estado) { this.estado = estado; }

    @PropertyName("publicado")
    public Boolean getPublicado() { return publicado; }
    @PropertyName("publicado")
    public void setPublicado(Boolean publicado) { this.publicado = publicado; }

    @PropertyName("dateFrom")
    public Date getDateFrom() { return dateFrom; }
    @PropertyName("dateFrom")
    public void setDateFrom(Date dateFrom) { this.dateFrom = dateFrom; }

    @PropertyName("dateTo")
    public Date getDateTo() { return dateTo; }
    @PropertyName("dateTo")
    public void setDateTo(Date dateTo) { this.dateTo = dateTo; }

    @PropertyName("createdAt")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("createdAt")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("paradas")
    public List<ParadaFB> getParadas() { return paradas; }
    @PropertyName("paradas")
    public void setParadas(List<ParadaFB> paradas) { this.paradas = paradas; }

    @PropertyName("id_paradas")
    public Object getIdParadasRaw() { return idParadasRaw; }
    @PropertyName("id_paradas")
    public void setIdParadasRaw(Object raw) { this.idParadasRaw = raw; }

    @PropertyName("services")
    public List<ServiceFB> getServices() { return services; }
    @PropertyName("services")
    public void setServices(List<ServiceFB> services) { this.services = services; }

    @PropertyName("cupos_totales")
    public Integer getCuposTotales() { return cuposTotales; }
    @PropertyName("cupos_totales")
    public void setCuposTotales(Integer cuposTotales) { this.cuposTotales = cuposTotales; }

    @PropertyName("cupos_disponibles")
    public Integer getCuposDisponibles() { return cuposDisponibles; }
    @PropertyName("cupos_disponibles")
    public void setCuposDisponibles(Integer cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }

    /* ================== HELPERS ================== */

    @Exclude
    public List<String> getIdParadasList() {
        if (idParadasRaw == null) return Collections.emptyList();
        if (idParadasRaw instanceof String) {
            String s = ((String) idParadasRaw).trim();
            return s.isEmpty() ? Collections.emptyList() : Collections.singletonList(s);
        }
        if (idParadasRaw instanceof List) {
            List<?> in = (List<?>) idParadasRaw;
            List<String> out = new ArrayList<>(in.size());
            for (Object o : in) if (o != null) out.add(String.valueOf(o));
            return out;
        }
        return Collections.emptyList();
    }

    @Exclude
    public void setId_paradas(List<String> ids) {
        this.idParadasRaw = (ids == null ? null : new ArrayList<>(ids));
    }

    @Exclude
    public String getDisplayName() {
        if (nombre != null && !nombre.isEmpty()) return nombre;
        return name != null ? name : "";
    }

    @Exclude
    public String getDisplayImageUrl() {
        if (imagen != null && !imagen.isEmpty()) return imagen;
        return imageUrl != null ? imageUrl : "";
    }

    @Exclude
    public int getDisplayPeople() {
        if (cantidad_personas > 0) return cantidad_personas;
        return people != null ? people : 0;
    }

    @Exclude
    public double getDisplayPrice() {
        if (precio > 0) return precio;
        return price != null ? price : 0.0;
    }

    // CUPOS
    @Exclude
    public int getCuposTotalesSafe() {
        if (cuposTotales != null && cuposTotales > 0) {
            return cuposTotales;
        }
        int base = getDisplayPeople();
        return Math.max(base, 0);
    }

    @Exclude
    public int getCuposDisponiblesSafe() {
        if (cuposDisponibles != null && cuposDisponibles >= 0) {
            return cuposDisponibles;
        }
        return getCuposTotalesSafe();
    }

    @Exclude
    public void setCuposDisponiblesSafe(int nuevosCupos) {
        this.cuposDisponibles = nuevosCupos;
    }

    // ESTADO
    @Exclude
    public boolean isPublicadoSafe() {
        return publicado != null && publicado;
    }

    @Exclude
    public String getSafeStatus() {
        if (status != null && !status.isEmpty()) {
            return status;
        }
        if (estado != null) {
            switch (estado.toLowerCase()) {
                case "pendiente":
                case "pending":
                    return "PENDING";
                case "en_curso":
                case "en curso":
                    return "EN_CURSO";
                case "finalizado":
                case "finished":
                    return "FINALIZADO";
            }
        }
        return "PENDING";
    }

    @Exclude
    public String getDisplayEstado() {
        String s = getSafeStatus();
        switch (s) {
            case "EN_CURSO":
                return "En curso";
            case "FINALIZADO":
                return "Finalizado";
            case "PENDING":
            default:
                return "Pendiente";
        }
    }

    @Exclude
    public boolean isPending() { return "PENDING".equals(getSafeStatus()); }

    @Exclude
    public boolean isEnCurso() { return "EN_CURSO".equals(getSafeStatus()); }

    @Exclude
    public boolean isFinalizado() { return "FINALIZADO".equals(getSafeStatus()); }

    @Exclude
    public boolean isAvailableForOffers() {
        return isPending()
                && isPublicadoSafe()
                && (assignedGuideId == null || assignedGuideId.isEmpty());
    }
}