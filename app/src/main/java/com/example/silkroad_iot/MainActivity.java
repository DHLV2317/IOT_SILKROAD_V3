package com.example.silkroad_iot;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.silkroad_iot.data.User;
import com.example.silkroad_iot.data.UserStore;
import com.example.silkroad_iot.databinding.ActivityMainBinding;
import com.example.silkroad_iot.ui.admin.AdminCompanyDetailActivity;
import com.example.silkroad_iot.ui.admin.AdminToursActivity;
import com.example.silkroad_iot.ui.auth.ForgotPasswordActivity;
import com.example.silkroad_iot.ui.auth.RegisterActivity;
import com.example.silkroad_iot.ui.client.ClientHomeActivity;
import com.example.silkroad_iot.ui.client.ClientOnboardingActivity;
import com.example.silkroad_iot.ui.guide.GuideHomeActivity;
import com.example.silkroad_iot.ui.guide.GuidePendingApprovalActivity;
import com.example.silkroad_iot.ui.superadmin.SuperAdminHomeActivity;
import com.google.android.material.snackbar.Snackbar;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// 🔵 Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;

// 🔷 Facebook
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;
    private final UserStore store = UserStore.get();
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleClient;
    private static final int RC_GOOGLE = 9001;

    // Facebook
    private CallbackManager callbackManager;

    // Preferencias para controlar flujo del admin
    private static final String PREFS = "app_prefs";
    private static final String KEY_COMPANY_DONE = "admin_company_done";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);

        // Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        callbackManager = CallbackManager.Factory.create();

        // Google Sign-In
        setupGoogleLogin();

        // Login normal (email/clave)
        b.btnLogin.setOnClickListener(v -> doLogin());

        // Registro
        b.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Olvidé contraseña
        b.tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // Botón Google
        b.btnGoogle.setOnClickListener(v -> doGoogleLogin());

        // Botón Facebook
        b.btnFacebook.setOnClickListener(v -> doFacebookLogin());

        // Registrar callback de Facebook
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Snackbar.make(b.getRoot(),
                                "Login de Facebook cancelado",
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Snackbar.make(b.getRoot(),
                                "Facebook error: " + error.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                });

        //Reinicia Superadmin DX
    }

    // --------------------------
    // 🔵 GOOGLE LOGIN
    // --------------------------
    private void setupGoogleLogin() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .build();

        googleClient = GoogleSignIn.getClient(this, gso);
    }

    private void doGoogleLogin() {
        try {
            Intent sign = googleClient.getSignInIntent();
            startActivityForResult(sign, RC_GOOGLE);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(b.getRoot(),
                    "Google Play Services no disponible",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // --------------------------
    // 🔷 FACEBOOK LOGIN
    // --------------------------
    private void doFacebookLogin() {
        // ❗ Importante: sólo pedimos public_profile para evitar Invalid Scopes: email
        LoginManager.getInstance()
                .logInWithReadPermissions(this, Arrays.asList("public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        setLoading(true);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnSuccessListener(res -> {
                    FirebaseUser fUser = auth.getCurrentUser();
                    if (fUser == null) {
                        setLoading(false);
                        Snackbar.make(b.getRoot(),
                                "Error inesperado (Facebook)",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String email = fUser.getEmail();
                    if (email == null) {
                        // Fallback si no tenemos permiso de email
                        email = fUser.getUid() + "@facebook.local";
                    }

                    checkUserProfileAfterLogin(email, "Facebook");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Facebook auth: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // --------------------------
    // onActivityResult
    // --------------------------
    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        // Primero intentamos que Facebook consuma el resultado
        if (callbackManager != null && callbackManager.onActivityResult(req, res, data)) {
            return;
        }

        super.onActivityResult(req, res, data);

        if (req == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acc = task.getResult(Exception.class);
                if (acc != null) {
                    firebaseLoginWithGoogle(acc);
                } else {
                    Snackbar.make(b.getRoot(),
                            "Cuenta Google nula",
                            Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Snackbar.make(b.getRoot(),
                        "Error con Google: " + e.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseLoginWithGoogle(GoogleSignInAccount acc) {
        setLoading(true);

        AuthCredential cred = GoogleAuthProvider.getCredential(acc.getIdToken(), null);
        auth.signInWithCredential(cred)
                .addOnSuccessListener(res -> {
                    FirebaseUser fUser = auth.getCurrentUser();
                    if (fUser == null) {
                        setLoading(false);
                        Snackbar.make(b.getRoot(),
                                "Error inesperado",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String email = fUser.getEmail();
                    if (email == null) email = acc.getEmail();

                    checkUserProfileAfterLogin(email, "Google");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Google auth: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // --------------------------
    // 🔐 LOGIN CON EMAIL
    // --------------------------
    private void doLogin() {
        String email = safe(b.inputEmail.getText());
        String pass  = safe(b.inputPass.getText());

        if (email.isEmpty()) { b.inputEmail.setError("Requerido"); return; }
        if (pass.isEmpty())  { b.inputPass.setError("Requerido");  return; }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> checkUserProfileAfterLogin(email, "Email"))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Login fallido: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // --------------------------
    // 🔎 POST LOGIN (Google / Facebook / Email)
    // --------------------------
    private void checkUserProfileAfterLogin(String email, String provider) {

        db.collection("usuarios")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        // Usuario social nuevo → Cliente por defecto
                        createSocialUser(email, provider);
                        return;
                    }

                    DocumentSnapshot d = snap.getDocuments().get(0);

                    User u = new User();
                    u.setEmail(email);
                    u.setUid(auth.getCurrentUser() != null
                            ? auth.getCurrentUser().getUid()
                            : "");
                    u.setName(nz(d.getString("nombre")));
                    u.setCompanyId(nz(d.getString("empresaId")));
                    u.setGuideId(nz(d.getString("guiaId")));

                    String r = nz(d.getString("rol")).toLowerCase();
                    if (r.equals("admin") || r.equals("empresa")) {
                        u.setRole(User.Role.ADMIN);
                    } else if (r.equals("guia") || r.equals("guide")) {
                        u.setRole(User.Role.GUIDE);
                    } else if (r.equals("superadmin")) {
                        u.setRole(User.Role.SUPERADMIN);
                    } else {
                        u.setRole(User.Role.CLIENT);
                    }

                    boolean ap = d.getBoolean("aprobado") != null && d.getBoolean("aprobado");
                    u.setGuideApproved(ap);
                    u.setGuideApprovalStatus(ap ? "APPROVED" : "PENDING");

                    // Datos de perfil cliente
                    u.setPhone(nz(d.getString("telefono")));
                    u.setAddress(nz(d.getString("direccion")));
                    u.setDni(nz(d.getString("dni")));
                    u.setPhotoUri(nz(d.getString("photoUri")));

                    Boolean cpc = d.getBoolean("clientProfileCompleted");
                    u.setClientProfileCompleted(cpc != null && cpc);

                    store.setLogged(u);
                    setLoading(false);
                    routeAfterLogin(u);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Firestore error: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // Crear usuario por defecto para Google/Facebook
    private void createSocialUser(String email, String provider) {
        User u = new User();
        u.setEmail(email);
        u.setName("Usuario " + provider);
        u.setRole(User.Role.CLIENT);
        u.setClientProfileCompleted(false);

        Map<String, Object> data = new HashMap<>();
        data.put("email", u.getEmail());
        data.put("nombre", u.getName());
        data.put("rol", "CLIENT");
        data.put("empresaId", u.getCompanyId());
        data.put("guiaId", u.getGuideId());
        data.put("aprobado", u.isGuideApproved());
        data.put("guideApprovalStatus", u.getGuideApprovalStatus());
        data.put("clientProfileCompleted", u.isClientProfileCompleted());

        db.collection("usuarios")
                .add(data)
                .addOnSuccessListener(id -> {
                    store.setLogged(u);
                    setLoading(false);
                    routeAfterLogin(u);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(b.getRoot(),
                            "Error creando usuario: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    // --------------------------
    // 🚀 RUTEO
    // --------------------------
    private void routeAfterLogin(User u) {
        Intent next;

        switch (u.getRole()) {

            case CLIENT:
                if (u.isClientProfileCompleted())
                    next = new Intent(this, ClientHomeActivity.class);
                else
                    next = new Intent(this, ClientOnboardingActivity.class);
                break;

            case GUIDE:
                if (u.isGuideApproved())
                    next = new Intent(this, GuideHomeActivity.class);
                else
                    next = new Intent(this, GuidePendingApprovalActivity.class);
                break;

            case ADMIN: {
                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                boolean companyCompleted = sp.getBoolean(KEY_COMPANY_DONE, false);

                if (!companyCompleted) {
                    // Primera vez → llenar datos de empresa
                    next = new Intent(this, AdminCompanyDetailActivity.class)
                            .putExtra("firstRun", true);
                } else {
                    // Después de la primera vez → AdminToursActivity es el home
                    next = new Intent(this, AdminToursActivity.class);
                }
                break;
            }

            case SUPERADMIN:
                next = new Intent(this, SuperAdminHomeActivity.class);
                break;

            default:
                next = new Intent(this, ClientHomeActivity.class);
                break;
        }

        next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(next);
        finish();
    }

    // --------------------------
    // HELPERS
    // --------------------------
    private void setLoading(boolean loading) {
        b.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.btnLogin.setEnabled(!loading);
        b.btnGoRegister.setEnabled(!loading);
        b.btnGoogle.setEnabled(!loading);
        b.btnFacebook.setEnabled(!loading);
        b.tvForgot.setEnabled(!loading);
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}