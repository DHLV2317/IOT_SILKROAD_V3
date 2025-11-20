package com.example.silkroad_iot.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * UserStore con soporte mixto:
 * - Login local de demo (síncrono) -> login(email, pass)
 * - Login real con FirebaseAuth + Firestore (asíncrono) -> loginWithFirebase(email, pass, callback)
 */
public class UserStore {

    // Singleton
    private static final UserStore I = new UserStore();
    public static UserStore get(){ return I; }

    // Firebase
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Almacen local (demo)
    private final Map<String, User> users   = new HashMap<>();
    private final Map<String, User> pending = new HashMap<>();
    private final Map<String, String> regCodes = new HashMap<>();
    private final Random rng = new Random();

    private User logged;

    private UserStore() {
        // ===== Usuarios demo por rol (pass: 123456) =====
        users.put("client@demo.com",
                new User("Cliente Demo", "client@demo.com", "123456", User.Role.CLIENT));

        User approvedGuide = new User("Guide Demo", "guide@demo.com", "123456", User.Role.GUIDE);
        approvedGuide.setGuideApproved(true);
        approvedGuide.setGuideApprovalStatus("APPROVED");
        users.put("guide@demo.com", approvedGuide);

        User pendingGuide = new User("Carlos Mendoza", "carlos.guia@demo.com", "123456", User.Role.GUIDE);
        pendingGuide.setLastName("Mendoza López");
        pendingGuide.setDocumentType("DNI");
        pendingGuide.setDocumentNumber("12345678");
        pendingGuide.setBirthDate("15/08/1985");
        pendingGuide.setPhone("+51 987 654 321");
        pendingGuide.setAddress("Av. Arequipa 1234, Miraflores, Lima");
        pendingGuide.setPhotoUri("content://fake/photo/carlos.jpg");
        pendingGuide.setLanguages("Español, Inglés, Quechua");
        pendingGuide.setGuideApproved(false);
        pendingGuide.setGuideApprovalStatus("PENDING");
        users.put("carlos.guia@demo.com", pendingGuide);

        users.put("admin@demo.com",
                new User("Administrador Demo", "admin@demo.com", "123456", User.Role.ADMIN));

        users.put("superadmin@demo.com",
                new User("SuperAdmin Demo", "superadmin@demo.com", "123456", User.Role.SUPERADMIN));
    }

    /* =========================================================
     *               LOGIN LOCAL (DEMO - SÍNCRONO)
     * ========================================================= */
    public boolean login(String email, String pass){
        User u = users.get(safeKey(email));
        if (u != null && u.getPassword().equals(pass)) {
            logged = u;
            return true;
        }
        return false;
    }

    /* =========================================================
     *        LOGIN REAL (FIREBASE AUTH + FIRESTORE) - ASYNC
     * ========================================================= */
    public interface LoginCallback {
        void onResult(boolean ok, String errorMessage);
    }

    /**
     * Inicia sesión con FirebaseAuth y carga el perfil desde Firestore (colección "users").
     * - Primero intenta users/{uid}
     * - Si no existe, busca por email (campo "email")
     * - Si no hay perfil, crea un User CLIENT básico con datos de Auth
     */
    public void loginWithFirebase(@NonNull String email, @NonNull String password, @NonNull LoginCallback cb){
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> {
                    FirebaseUser fu = auth.getCurrentUser();
                    if (fu == null) { cb.onResult(false, "No se pudo obtener el usuario de Firebase."); return; }

                    String uid = fu.getUid();

                    // 1) intentar /users/{uid}
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    User u = fromFirestoreDoc(doc, fu);
                                    setLogged(u);
                                    cb.onResult(true, null);
                                } else {
                                    // 2) buscar por email
                                    Query q = db.collection("users").whereEqualTo("email", fu.getEmail());
                                    q.get().addOnSuccessListener(snap -> {
                                        if (!snap.isEmpty()) {
                                            DocumentSnapshot d = snap.getDocuments().get(0);
                                            User u = fromFirestoreDoc(d, fu);
                                            setLogged(u);
                                            cb.onResult(true, null);
                                        } else {
                                            // 3) No hay perfil -> crear User básico con datos de Auth
                                            User u = new User();
                                            u.setUid(uid);
                                            u.setEmail(nonNull(fu.getEmail()));
                                            u.setName(nonNull(fu.getDisplayName())); // puede venir vacío
                                            u.setRole(User.Role.CLIENT);             // por defecto
                                            setLogged(u);
                                            cb.onResult(true, null);
                                        }
                                    }).addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
                                }
                            })
                            .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    /* =========================================================
     *                     REGISTRO (DEMO)
     * ========================================================= */
    public boolean exists(String email){ return users.containsKey(safeKey(email)); }

    public String startRegistration(User u){
        String key = safeKey(u.getEmail());
        pending.put(key, u);
        String code = fourDigits();
        regCodes.put(key, code);
        return code;
    }

    public String resendRegistrationCode(String email){
        String key = safeKey(email);
        if (!pending.containsKey(key)) return null;
        String code = fourDigits();
        regCodes.put(key, code);
        return code;
    }

    public boolean verifyRegistrationCode(String email, String code){
        String key = safeKey(email);
        return code != null && code.equals(regCodes.get(key));
    }

    public boolean finalizeRegistration(String email){
        String key = safeKey(email);
        User u = pending.remove(key);
        if (u == null) return false;
        users.put(key, u);
        regCodes.remove(key);
        return true;
    }

    /* =========================================================
     *                   SESIÓN / ACTUALIZACIONES
     * ========================================================= */
    public User getLogged(){ return logged; }
    public void logout(){ logged = null; }

    /** Guarda cambios en el usuario logueado en el almacén local (demo) */
    public void updateLogged(User updated){
        if (updated == null) return;
        users.put(safeKey(updated.getEmail()), updated);
        logged = updated;
    }

    /** Permite a otros componentes fijar el logueado (útil tras Firebase login) */
    public void setLogged(User u){
        this.logged = u;
        if (u != null && !TextUtils.isEmpty(u.getEmail())) {
            users.put(safeKey(u.getEmail()), u); // también lo tenemos disponible en cache local
        }
    }

    /* =========================================================
     *                 MÉTODOS ESPECÍFICOS GUÍAS
     * ========================================================= */
    public boolean registerGuide(String names, String lastNames, String documentType,
                                 String documentNumber, String birthDate, String email,
                                 String phone, String address, String photoUri, String languages) {

        if (exists(email)) return false;

        User guide = new User(names, email, "123456", User.Role.GUIDE); // Password temporal (demo)
        guide.setLastName(lastNames);
        guide.setDocumentType(documentType);
        guide.setDocumentNumber(documentNumber);
        guide.setBirthDate(birthDate);
        guide.setPhone(phone);
        guide.setAddress(address);
        guide.setPhotoUri(photoUri);
        guide.setLanguages(languages);
        guide.setGuideApproved(false);
        guide.setGuideApprovalStatus("PENDING");

        users.put(safeKey(email), guide);
        return true;
    }

    public boolean approveGuide(String email, boolean approved) {
        User guide = users.get(safeKey(email));
        if (guide == null || guide.getRole() != User.Role.GUIDE) return false;

        guide.setGuideApproved(approved);
        guide.setGuideApprovalStatus(approved ? "APPROVED" : "REJECTED");
        return true;
    }

    public java.util.List<User> getPendingGuides() {
        java.util.List<User> out = new java.util.ArrayList<>();
        for (User u : users.values()) {
            if (u.getRole() == User.Role.GUIDE &&
                    "PENDING".equals(u.getGuideApprovalStatus())) {
                out.add(u);
            }
        }
        return out;
    }

    /* =========================================================
     *                         HELPERS
     * ========================================================= */
    private static String safeKey(String email){
        return (email == null) ? "" : email.trim().toLowerCase();
    }

    private String fourDigits(){ return String.format("%04d", rng.nextInt(10000)); }

    private static String nonNull(String s){ return s == null ? "" : s; }

    private static User.Role asRole(Object v){
        if (v == null) return User.Role.CLIENT;
        String s = String.valueOf(v).trim().toUpperCase();
        try { return User.Role.valueOf(s); }
        catch (Exception ignore){ return User.Role.CLIENT; }
    }

    /** Construye un User desde un doc Firestore + FirebaseUser (para completar uid/email/displayName) */
    private static User fromFirestoreDoc(DocumentSnapshot d, FirebaseUser fu){
        User u = new User();
        u.setUid(fu.getUid());
        u.setEmail(nonNull(fu.getEmail()));

        // nombre/displayName: aceptamos displayName o "nombre" o "displayName" del doc
        String name = null;
        if (fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()) {
            name = fu.getDisplayName();
        } else if (d.contains("displayName")) {
            name = String.valueOf(d.get("displayName"));
        } else if (d.contains("nombre")) {
            name = String.valueOf(d.get("nombre"));
        }
        u.setName(nonNull(name));

        // rol
        u.setRole(asRole(d.get("role")));

        // vínculos opcionales
        if (d.contains("companyId")) u.setCompanyId(String.valueOf(d.get("companyId")));
        if (d.contains("guideId"))   u.setGuideId(String.valueOf(d.get("guideId")));

        // datos guía opcionales
        if (d.contains("languages")) u.setLanguages(String.valueOf(d.get("languages")));
        if (d.contains("guideApproved")) {
            Object ga = d.get("guideApproved");
            if (ga instanceof Boolean) u.setGuideApproved((Boolean) ga);
        }
        if (d.contains("guideApprovalStatus")) {
            u.setGuideApprovalStatus(String.valueOf(d.get("guideApprovalStatus")));
        }

        // cliente opcional
        if (d.contains("address")) u.setAddress(String.valueOf(d.get("address")));
        if (d.contains("phone"))   u.setPhone(String.valueOf(d.get("phone")));

        // NUEVO: dni
        if (d.contains("dni")) {
            u.setDni(String.valueOf(d.get("dni")));
        }

        // NUEVO: clientProfileCompleted
        if (d.contains("clientProfileCompleted")) {
            Object cpc = d.get("clientProfileCompleted");
            if (cpc instanceof Boolean) {
                u.setClientProfileCompleted((Boolean) cpc);
            }
        }

        return u;
    }
}