package com.example.silkroad_iot.data;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

@IgnoreExtraProperties
public class User implements Serializable {

    // ---------------- ENUM ROL ----------------
    public enum Role { CLIENT, GUIDE, ADMIN, SUPERADMIN }

    // ---------------- CAMPOS GENERALES ----------------
    private String name;
    private String email;
    private String password;
    private Role role;

    private boolean active;

    // IDs externos (Firebase / Firestore)
    private String uid;         // ID del usuario en Firebase Auth

    // en Firestore solemos guardar "empresaId"
    private String companyId;   // para admins/empresas
    private String guideId;     // para guías

    // ---------------- PERFIL CLIENTE ----------------
    private String lastName;
    private String phone;
    private String address;
    private String photoUri;
    private boolean clientProfileCompleted;

    // NUEVO: DNI
    private String dni;

    // ---------------- PERFIL GUÍA ----------------
    private String documentType;
    private String documentNumber;
    private String birthDate;
    private String languages;
    private boolean guideApproved = false; // por defecto no aprobado
    private String guideApprovalStatus = "PENDING"; // PENDING, APPROVED, REJECTED
    private String estado; // “Disponible”, “Ocupado”, etc.
    private String fotoUrl; // para versión pública en apps cliente

    // ---------------- CONSTRUCTORES ----------------
    public User() {}

    public User(String name, String email, String password) {
        this(name, email, password, Role.CLIENT);
    }

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = (role == null ? Role.CLIENT : role);
    }

    // ---------------- GETTERS / SETTERS ----------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // ===== empresaId <-> companyId =====
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    @PropertyName("empresaId")
    public String getEmpresaId() { return companyId; }
    @PropertyName("empresaId")
    public void setEmpresaId(String empresaId) { this.companyId = empresaId; }

    public String getGuideId() { return guideId; }
    public void setGuideId(String guideId) { this.guideId = guideId; }

    // -------- Cliente --------
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String v) { this.photoUri = v; }

    public boolean isClientProfileCompleted() { return clientProfileCompleted; }
    public void setClientProfileCompleted(boolean v) { this.clientProfileCompleted = v; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    // -------- Guía --------
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public boolean isGuideApproved() { return guideApproved; }
    public void setGuideApproved(boolean guideApproved) { this.guideApproved = guideApproved; }

    public String getGuideApprovalStatus() { return guideApprovalStatus; }
    public void setGuideApprovalStatus(String guideApprovalStatus) { this.guideApprovalStatus = guideApprovalStatus; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    // ---------------- MÉTODOS ÚTILES ----------------
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isGuide() { return role == Role.GUIDE; }
    public boolean isClient() { return role == Role.CLIENT; }
    public boolean isSuperAdmin() { return role == Role.SUPERADMIN; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", uid='" + uid + '\'' +
                ", guideApproved=" + guideApproved +
                ", guideApprovalStatus='" + guideApprovalStatus + '\'' +
                '}';
    }
}