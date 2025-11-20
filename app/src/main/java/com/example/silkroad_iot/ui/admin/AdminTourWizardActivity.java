package com.example.silkroad_iot.ui.admin;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.ServiceFB;
import com.example.silkroad_iot.data.TourFB;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminTourWizardActivity extends AppCompatActivity {

    // Firestore
    private FirebaseFirestore db;

    // Vistas comunes
    private View group1, group2, group3, group4;
    private MaterialButton btnPrev, btnNext;
    private MaterialToolbar toolbar;

    // Paso 1
    private TextInputEditText inName, inDesc, inDuration, inDate, inPrice, inPeople;
    private MaterialAutoCompleteTextView inCity;
    private TextInputEditText inLangs; // SOLO DISPLAY (idiomas se llenan cuando el guía acepte)
    private android.widget.ImageView img;

    // Paso 2
    private TextInputEditText inStopAddr, inStopMin;
    private LinearLayout boxStops;

    // Paso 3
    private TextInputEditText inSrvName, inSrvPrice;
    private LinearLayout boxServices;

    // Paso 4
    private LinearLayout boxGuides;
    private TextInputEditText inPayment;

    // Estado
    private int step = 1;
    private final Set<String> langsSel = new LinkedHashSet<>(); // solo para mostrar en UI
    private Long dateRangeStart = null, dateRangeEnd = null;
    private Uri pickedImage;

    // Datos reunidos (texto plano para stops y services)
    private final List<String> stops = new ArrayList<>();
    private final List<String> services = new ArrayList<>();

    // Guías
    private static class GuideItem {
        String id;
        String name;
        String langs;
        String email;
    }

    private final List<GuideItem> guideItems = new ArrayList<>();
    private final List<String> selectedGuideIds = new ArrayList<>();
    private final List<String> selectedGuideNames = new ArrayList<>();

    // Modo edición
    private String editingDocId = null;
    private String defaultEmpresaId = null;
    private TourFB editingTour;
    private String existingImageUrl;
    private String existingAssignedGuideId;
    private String existingAssignedGuideName;

    // Picker de imagen (por ahora usamos la URI local, la subida real a Storage se puede
    // implementar luego si quieres que sea pública entre dispositivos)
    private final androidx.activity.result.ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            pickedImage = uri;
                            Glide.with(this).load(uri).into(img);
                        }
                    });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_admin_tour_wizard);

        db = FirebaseFirestore.getInstance();

        editingDocId = getIntent().getStringExtra("docId");
        defaultEmpresaId = getIntent().getStringExtra("empresaId");

        bindViews();
        setupStep1();
        setupStep2();
        setupStep3();
        setupStep4(); // solo prepara la UI, la carga de guías se hace abajo

        if (editingDocId == null) {
            // NUEVO TOUR
            setTitle("Nuevo Tour (1/4)");
            addServiceDefaultsIfNew();
            loadGuidesFromFirestore(); // Carga guías para que el admin pueda invitarlos
        } else {
            // EDITAR TOUR
            setTitle("Editar Tour (1/4)");
            loadTourForEdit();
        }

        updateUiForStep();

        btnPrev.setOnClickListener(v -> {
            if (step > 1) {
                step--;
                updateUiForStep();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (step < 4) {
                if (!validateStep(step)) return;
                step++;
                updateUiForStep();
            } else {
                if (!validateStep(4)) return;
                // Validar duración vs paradas antes de guardar
                if (!validateDurationVsStops()) return;
                saveTourToFirestore();
            }
        });
    }

    /* ================== Bind ================== */
    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        group1 = findViewById(R.id.groupStep1);
        group2 = findViewById(R.id.groupStep2);
        group3 = findViewById(R.id.groupStep3);
        group4 = findViewById(R.id.groupStep4);

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        img = findViewById(R.id.img);
        inName = findViewById(R.id.inputName);
        inDesc = findViewById(R.id.inputDesc);
        inDuration = findViewById(R.id.inputDuration);
        inLangs = findViewById(R.id.inputLangs);
        inDate = findViewById(R.id.inputDate);
        inPrice = findViewById(R.id.inputPrice);
        inPeople = findViewById(R.id.inputPeople);
        inCity = findViewById(R.id.inputCity);

        inStopAddr = findViewById(R.id.inputStopAddress);
        inStopMin = findViewById(R.id.inputStopMinutes);
        boxStops = findViewById(R.id.boxStops);

        inSrvName = findViewById(R.id.inputServiceName);
        inSrvPrice = findViewById(R.id.inputServicePrice);
        boxServices = findViewById(R.id.boxServices);

        boxGuides = findViewById(R.id.boxGuides);
        inPayment = findViewById(R.id.inputPayment);
    }

    /* ================== Paso 1 ================== */
    private void setupStep1() {
        img.setOnClickListener(v -> pickImage.launch("image/*"));

        com.google.android.material.chip.Chip btnChangeImage = findViewById(R.id.btnChangeImage);
        if (btnChangeImage != null) {
            btnChangeImage.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        // ❗ Idiomas: en esta versión NO se seleccionan manualmente.
        // Se mostrarán cuando el tour tenga guía asignado.
        inLangs.setInputType(InputType.TYPE_NULL);
        inLangs.setFocusable(false);
        inLangs.setHint("Se llenará cuando un guía acepte el tour");

        // Ciudad: departamentos de Perú
        String[] departamentosPeru = {
                "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho",
                "Cajamarca", "Callao", "Cusco", "Huancavelica", "Huánuco",
                "Ica", "Junín", "La Libertad", "Lambayeque", "Lima",
                "Loreto", "Madre de Dios", "Moquegua", "Pasco", "Piura",
                "Puno", "San Martín", "Tacna", "Tumbes", "Ucayali"
        };

        ArrayAdapter<String> depAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                departamentosPeru
        );
        inCity.setAdapter(depAdapter);
        inCity.setInputType(InputType.TYPE_NULL);
        inCity.setOnClickListener(v -> inCity.showDropDown());
        inCity.setOnFocusChangeListener((v, has) -> {
            if (has) inCity.showDropDown();
        });

        // Selector de rango de fechas
        inDate.setOnClickListener(v -> {
            MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> b =
                    MaterialDatePicker.Builder.dateRangePicker();
            MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = b.build();
            picker.addOnPositiveButtonClickListener(pair -> {
                if (pair != null) {
                    dateRangeStart = pair.first;
                    dateRangeEnd = pair.second;
                    inDate.setText(fmtDate(pair.first) + " - " + fmtDate(pair.second));
                }
            });
            picker.show(getSupportFragmentManager(), "range");
        });
    }

    /* ================== Paso 2 ================== */
    private void setupStep2() {
        MaterialButton btnAddStop = findViewById(R.id.btnAddStop);
        btnAddStop.setOnClickListener(v -> {
            String addr = safeText(inStopAddr);
            String mins = safeText(inStopMin);
            if (addr.isEmpty()) {
                inStopAddr.setError("Requerido");
                return;
            }
            if (mins.isEmpty()) {
                inStopMin.setError("Requerido");
                return;
            }
            int mVal = parseInt(mins, -1);
            if (mVal <= 0) {
                inStopMin.setError("Minutos inválidos");
                return;
            }
            addStopRow(addr, String.valueOf(mVal));
            inStopAddr.setText("");
            inStopMin.setText("");
        });
    }

    private void addStopRow(String addr, String mins) {
        String label = addr + " · " + mins + " min";
        stops.add(label);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);

        MaterialButton rm = createOutlinedButton("Quitar");
        rm.setOnClickListener(v -> {
            boxStops.removeView(row);
            stops.remove(label);
        });
        row.addView(rm);

        boxStops.addView(row);
    }

    /* ================== Paso 3 ================== */
    private void setupStep3() {
        MaterialButton btnAddService = findViewById(R.id.btnAddService);
        btnAddService.setOnClickListener(v -> {
            String name = safeText(inSrvName);
            String price = safeText(inSrvPrice);
            if (name.isEmpty()) {
                inSrvName.setError("Requerido");
                return;
            }
            if (price.isEmpty()) price = "0";
            addServiceRow(name, "S/ " + price);
            inSrvName.setText("");
            inSrvPrice.setText("");
        });
    }

    private void addServiceDefaultsIfNew() {
        addServiceRow("Desayuno", "Incluido");
        addServiceRow("Almuerzo", "Incluido");
        addServiceRow("Cena", "Incluido");
    }

    private void addServiceRow(String name, String value) {
        String label = name + " - " + value;
        services.add(label);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);

        MaterialButton rm = createOutlinedButton("Quitar");
        rm.setOnClickListener(v -> {
            boxServices.removeView(row);
            services.remove(label);
        });
        row.addView(rm);

        boxServices.addView(row);
    }

    /* ================== Paso 4 ================== */
    private void setupStep4() {
        // La carga real de guías se hace en:
        //  - onCreate (si es nuevo)
        //  - loadTourForEdit() -> luego loadGuidesFromFirestore() (si es edición)
    }

    private void loadGuidesFromFirestore() {
        boxGuides.removeAllViews();
        guideItems.clear();

        db.collection("guias")
                .whereEqualTo("guideApproved", true)
                .get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap) {
                        GuideItem gi = new GuideItem();
                        gi.id = d.getId();
                        String n = d.getString("nombres");
                        String a = d.getString("apellidos");
                        gi.name = ((n == null ? "" : n) + " " + (a == null ? "" : a)).trim();
                        if (gi.name.isEmpty()) gi.name = d.getString("nombre");
                        // IMPORTANTE: aquí depende de cómo guardas los idiomas en "guias"
                        // en GuideRegisterActivity guardas "langs" (códigos), aquí lo leemos:
                        gi.langs = d.getString("langs");
                        gi.email = d.getString("email") != null
                                ? d.getString("email")
                                : d.getString("correo");
                        guideItems.add(gi);

                        CheckBox cb = new CheckBox(this);
                        String label = (gi.name == null ? "Guía" : gi.name)
                                + "  ·  " + (gi.langs == null ? "—" : gi.langs);
                        cb.setText(label);

                        // Preseleccionar si ya estaba invitado (modo edición)
                        if (selectedGuideIds.contains(gi.id)) {
                            cb.setChecked(true);
                        }

                        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                if (!selectedGuideIds.contains(gi.id))
                                    selectedGuideIds.add(gi.id);
                                if (!selectedGuideNames.contains(gi.name))
                                    selectedGuideNames.add(gi.name);
                            } else {
                                selectedGuideIds.remove(gi.id);
                                selectedGuideNames.remove(gi.name);
                            }
                            // SOLO UI: mostrar idiomas combinados en el campo, NO se guardan aquí
                            updateLangsFromSelectedGuides();
                        });
                        boxGuides.addView(cb);
                    }
                });
    }

    /**
     * Solo para mostrar en el campo "Idiomas" un resumen de los idiomas
     * de los guías seleccionados. NO se persiste en Firestore desde aquí.
     */
    private void updateLangsFromSelectedGuides() {
        langsSel.clear();
        for (GuideItem gi : guideItems) {
            if (selectedGuideIds.contains(gi.id) && gi.langs != null) {
                for (String token : splitLangs(gi.langs)) {
                    if (!TextUtils.isEmpty(token)) langsSel.add(normalizeLang(token));
                }
            }
        }
        if (!langsSel.isEmpty()) {
            inLangs.setText(String.join("/", langsSel));
        } else if (editingTour != null && !TextUtils.isEmpty(editingTour.getLangs())) {
            // Si estamos editando un tour ya aceptado, mantenemos sus idiomas
            inLangs.setText(editingTour.getLangs());
        } else {
            inLangs.setText("");
        }
    }

    /* ================== Carga en modo EDICIÓN ================== */
    private void loadTourForEdit() {
        db.collection("tours")
                .document(editingDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        finish();
                        return;
                    }
                    editingTour = doc.toObject(TourFB.class);
                    if (editingTour == null) editingTour = new TourFB();

                    // Empresa
                    if (TextUtils.isEmpty(editingTour.getEmpresaId()) && defaultEmpresaId != null) {
                        editingTour.setEmpresaId(defaultEmpresaId);
                    }

                    // Imagen existente
                    existingImageUrl = editingTour.getDisplayImageUrl();

                    // Guía asignado (si lo hubiera)
                    existingAssignedGuideId = editingTour.getAssignedGuideId();
                    existingAssignedGuideName = editingTour.getAssignedGuideName();

                    bindEditingToUi(editingTour, doc);
                    loadGuidesFromFirestore();
                })
                .addOnFailureListener(e -> {
                    showToast("Error cargando tour: " + e.getMessage());
                    finish();
                });
    }

    private void bindEditingToUi(TourFB t, com.google.firebase.firestore.DocumentSnapshot doc) {

        // ===== Paso 1 =====
        String imageToShow = t.getDisplayImageUrl();
        if (TextUtils.isEmpty(imageToShow)) {
            Glide.with(this)
                    .load(R.drawable.ic_image_24)
                    .into(img);
        } else {
            Glide.with(this)
                    .load(imageToShow)
                    .placeholder(R.drawable.ic_image_24)
                    .error(R.drawable.ic_image_24)
                    .into(img);
        }

        inName.setText(nz(t.getDisplayName()));
        inDesc.setText(nz(t.getDescription()));
        inDuration.setText(nz(t.getDuration()));
        inCity.setText(nz(t.getCiudad()));

        String langsRaw = nz(t.getLangs());
        if (!langsRaw.isEmpty()) {
            langsSel.clear();
            for (String token : splitLangs(langsRaw)) {
                if (!TextUtils.isEmpty(token)) {
                    langsSel.add(normalizeLang(token));
                }
            }
            inLangs.setText(String.join("/", langsSel));
        }

        if (t.getDateFrom() != null && t.getDateTo() != null) {
            dateRangeStart = t.getDateFrom().getTime();
            dateRangeEnd = t.getDateTo().getTime();
            inDate.setText(fmtDate(dateRangeStart) + " - " + fmtDate(dateRangeEnd));
        } else if (t.getDateFrom() != null) {
            dateRangeStart = t.getDateFrom().getTime();
            inDate.setText(fmtDate(dateRangeStart));
        }

        if (t.getDisplayPrice() > 0) {
            inPrice.setText(String.valueOf(t.getDisplayPrice()));
        }
        if (t.getDisplayPeople() > 0) {
            inPeople.setText(String.valueOf(t.getDisplayPeople()));
        }

        // ===== Paso 2 (paradas) =====
        stops.clear();
        boxStops.removeAllViews();
        List<String> id_paradas_list = t.getIdParadasList();
        if (id_paradas_list != null && !id_paradas_list.isEmpty()) {
            // MODO NUEVO: lista de strings
            if (id_paradas_list.size() > 1 || !id_paradas_list.get(0).contains("|")) {
                for (String item : id_paradas_list) {
                    String s = item.trim();
                    if (s.isEmpty()) continue;
                    String addr = s;
                    String mins = "0";
                    int dotIdx = s.indexOf("·");
                    int minIdx = s.toLowerCase(Locale.getDefault()).indexOf("min");
                    if (dotIdx > 0 && minIdx > dotIdx) {
                        addr = s.substring(0, dotIdx).trim();
                        String mid = s.substring(dotIdx + 1, minIdx)
                                .replace("·", "")
                                .replace("min", "")
                                .trim();
                        if (mid.isEmpty()) mid = "0";
                        mins = mid;
                    }
                    addStopRow(addr, mins);
                }
            } else {
                // LEGACY: un solo string con " | "
                String raw = id_paradas_list.get(0);
                if (!TextUtils.isEmpty(raw)) {
                    String[] chunks = raw.split("\\|");
                    for (String ch : chunks) {
                        String s = ch.trim();
                        if (s.isEmpty()) continue;
                        String addr = s;
                        String mins = "0";
                        int dotIdx = s.indexOf("·");
                        int minIdx = s.toLowerCase(Locale.getDefault()).indexOf("min");
                        if (dotIdx > 0 && minIdx > dotIdx) {
                            addr = s.substring(0, dotIdx).trim();
                            String mid = s.substring(dotIdx + 1, minIdx)
                                    .replace("·", "")
                                    .replace("min", "")
                                    .trim();
                            if (mid.isEmpty()) mid = "0";
                            mins = mid;
                        }
                        addStopRow(addr, mins);
                    }
                }
            }
        }

        // ===== Paso 3 (servicios) =====
        services.clear();
        boxServices.removeAllViews();
        List<ServiceFB> srvList = t.getServices();
        if (srvList != null && !srvList.isEmpty()) {
            for (ServiceFB sv : srvList) {
                String name = nz(sv.getName());
                String value;
                if (Boolean.TRUE.equals(sv.getIncluded())) {
                    value = "Incluido";
                } else {
                    double p = sv.getPrice() == null ? 0.0 : sv.getPrice();
                    value = "S/ " + String.format(Locale.getDefault(), "%.2f", p);
                }
                addServiceRow(name, value);
            }
        }

        // ===== Paso 4 (guías + pago) =====
        List<String> invitedIds = (List<String>) doc.get("invitedGuideIds");
        List<String> invitedNames = (List<String>) doc.get("invitedGuideNames");
        selectedGuideIds.clear();
        selectedGuideNames.clear();
        if (invitedIds != null) selectedGuideIds.addAll(invitedIds);
        if (invitedNames != null) selectedGuideNames.addAll(invitedNames);

        Double payProp = t.getPaymentProposal();
        if (payProp != null && payProp > 0) {
            inPayment.setText(String.valueOf(payProp));
        }
    }

    /* ================== Guardado Firestore ================== */
    private void saveTourToFirestore() {
        boolean isNew = (editingDocId == null);

        String nombre = safeText(inName);
        String desc = safeText(inDesc);
        String duration = safeText(inDuration);
        String ciudad = safeText(inCity);

        // ❗ Idiomas:
        // - Si es NUEVO: se dejan vacíos. Se llenarán cuando el guía acepte el tour.
        // - Si es EDICIÓN: mantenemos lo que ya tenga el documento.
        String langsFinal = "";
        if (!isNew && editingTour != null && !TextUtils.isEmpty(editingTour.getLangs())) {
            langsFinal = editingTour.getLangs();
        }

        double precio = parseDouble(safeText(inPrice), 0);
        int cantidad = parseInt(safeText(inPeople), 1);
        Double payProp = parseDouble(safeText(inPayment), 0);

        // Imagen:
        String imagen = null;
        if (pickedImage != null) {
            imagen = pickedImage.toString();
        } else if (editingTour != null && !TextUtils.isEmpty(editingTour.getDisplayImageUrl())) {
            imagen = editingTour.getDisplayImageUrl();
        }

        // Empresa
        String empresaId;
        if (defaultEmpresaId != null) {
            empresaId = defaultEmpresaId;
        } else if (editingTour != null && !TextUtils.isEmpty(editingTour.getEmpresaId())) {
            empresaId = editingTour.getEmpresaId();
        } else {
            empresaId = "1";
        }

        // Descripción grande (FULL TEXT)
        StringBuilder full = new StringBuilder();
        if (!desc.isEmpty()) full.append(desc).append("\n");
        if (!duration.isEmpty()) full.append("Duración: ").append(duration).append(" horas\n");
        if (!TextUtils.isEmpty(langsFinal)) full.append("Idiomas: ").append(langsFinal).append("\n");
        if (!services.isEmpty()) {
            full.append("Servicios:\n");
            for (String s : services) full.append("• ").append(s).append("\n");
        }

        // Guía asignado:
        String assignedGuideName = null;
        String assignedGuideId = null;

        // Si estamos editando y el tour ya tiene guía asignado, lo preservamos
        if (!isNew && editingTour != null) {
            assignedGuideId = existingAssignedGuideId;
            assignedGuideName = existingAssignedGuideName;
        }

        TourFB t = new TourFB();
        t.setNombre(nombre);
        t.setDescription(desc);
        t.setImagen(imagen);
        t.setPrecio(precio);
        t.setCantidad_personas(cantidad);
        t.setCiudad(ciudad);
        t.setEmpresaId(empresaId);
        t.setLangs(langsFinal);
        t.setDuration(duration);
        t.setAssignedGuideName(assignedGuideName);
        t.setAssignedGuideId(assignedGuideId);
        t.setPaymentProposal(payProp);

        // Paradas como lista (nuevo formato)
        if (stops.isEmpty()) {
            t.setId_paradas(Collections.emptyList());
        } else {
            t.setId_paradas(new ArrayList<>(stops));
        }

        // Imagen por defecto si no se eligió ninguna ni había previa
        if (t.getImagen() == null || t.getImagen().trim().isEmpty()) {
            t.setImagen("https://llerena.org/wp-content/uploads/2017/11/imagen-no-disponible-1.jpg");
        }

        // Servicios embebidos
        List<ServiceFB> serviceList = new ArrayList<>();
        for (String label : services) {
            String[] parts = label.split(" - ");
            if (parts.length < 2) continue;
            String sName = parts[0].trim();
            String sVal = parts[1].trim();

            ServiceFB sv = new ServiceFB();
            sv.setName(sName);

            if (sVal.equalsIgnoreCase("Incluido")) {
                sv.setIncluded(true);
                sv.setPrice(0.0);
            } else {
                String num = sVal.replace("S/", "")
                        .replace("s/", "")
                        .replace("S/.", "")
                        .trim();
                double p = parseDouble(num, 0.0);
                sv.setIncluded(false);
                sv.setPrice(p);
            }
            serviceList.add(sv);
        }
        t.setServices(serviceList);

        // CUPOS: por ahora igual a cantidad de personas (por día se maneja en otra capa)
        t.setCuposTotales(cantidad);
        t.setCuposDisponibles(cantidad);

        // Extras para merge
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        if (full.length() > 0) extra.put("description", full.toString());
        if (dateRangeStart != null) extra.put("dateFrom", new Date(dateRangeStart));
        if (dateRangeEnd != null) extra.put("dateTo", new Date(dateRangeEnd));
        if (!selectedGuideIds.isEmpty())
            extra.put("invitedGuideIds", new ArrayList<>(selectedGuideIds));
        if (!selectedGuideNames.isEmpty())
            extra.put("invitedGuideNames", new ArrayList<>(selectedGuideNames));
        if (assignedGuideId != null) extra.put("assignedGuideId", assignedGuideId);
        if (assignedGuideName != null) extra.put("assignedGuideName", assignedGuideName);
        if (!TextUtils.isEmpty(langsFinal))
            extra.put("idiomas_array", new ArrayList<>(splitLangs(langsFinal)));

        // Solo cuando es NUEVO tour:
        if (isNew) {
            extra.put("status", "PENDING");
            extra.put("estado", "pendiente");
            extra.put("publicado", true);
            extra.put("createdAt", new Date());
            t.setEstado("pendiente");
            t.setPublicado(true);
        }

        if (isNew) {
            db.collection("tours")
                    .add(t)
                    .addOnSuccessListener(ref -> {
                        if (!extra.isEmpty()) {
                            ref.set(extra, SetOptions.merge())
                                    .addOnSuccessListener(unused -> finish());
                        } else finish();
                    });
        } else {
            DocumentReference ref = db.collection("tours").document(editingDocId);
            ref.set(t, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        if (!extra.isEmpty()) {
                            // En edición NO tocamos status/estado/publicado
                            ref.set(extra, SetOptions.merge())
                                    .addOnSuccessListener(u2 -> finish());
                        } else finish();
                    });
        }
    }

    /* ================== Helpers ================== */
    private MaterialButton createOutlinedButton(String text) {
        int styleM3 = R.style.M3_OutlinedButton;
        int styleMDC = R.style.MDC_OutlinedButton;

        ContextThemeWrapper ctw;
        try {
            ctw = new ContextThemeWrapper(this, styleM3);
        } catch (Throwable ignore) {
            ctw = new ContextThemeWrapper(this, styleMDC);
        }

        MaterialButton b = new MaterialButton(ctw, null, 0);
        b.setText(text);
        return b;
    }

    private List<String> splitLangs(String raw) {
        if (raw == null) return new ArrayList<>();
        String[] arr = raw.split("[/,;]");
        List<String> out = new ArrayList<>();
        for (String a : arr) {
            String s = a.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private String normalizeLang(String s) {
        s = s.trim();
        if (s.equalsIgnoreCase("es") || s.equalsIgnoreCase("español")) return "Español";
        if (s.equalsIgnoreCase("en") || s.equalsIgnoreCase("ingles") || s.equalsIgnoreCase("inglés"))
            return "Inglés";
        if (s.equalsIgnoreCase("fr") || s.equalsIgnoreCase("frances") || s.equalsIgnoreCase("francés"))
            return "Francés";
        if (s.equalsIgnoreCase("de") || s.equalsIgnoreCase("alemán") || s.equalsIgnoreCase("aleman"))
            return "Alemán";
        if (s.equalsIgnoreCase("pt") || s.equalsIgnoreCase("portugues") || s.equalsIgnoreCase("portugués"))
            return "Portugués";
        if (s.length() > 1)
            return s.substring(0, 1).toUpperCase(Locale.getDefault())
                    + s.substring(1).toLowerCase(Locale.getDefault());
        return s.toUpperCase(Locale.getDefault());
    }

    private boolean validateStep(int s) {
        if (s == 1) {
            if (isEmpty(inName)) {
                inName.setError("Requerido");
                return false;
            }
            if (isEmpty(inCity)) {
                inCity.setError("Requerido");
                return false;
            }
            if (isEmpty(inPrice)) {
                inPrice.setError("Requerido");
                return false;
            }
            if (isEmpty(inPeople)) {
                inPeople.setError("Requerido");
                return false;
            }
            // Duración en horas, solo números
            String durText = safeText(inDuration);
            double durHours = parseDouble(durText, -1);
            if (durHours <= 0) {
                inDuration.setError("Duración en horas (solo números, > 0)");
                return false;
            }
        }
        return true;
    }

    private boolean validateDurationVsStops() {
        String durText = safeText(inDuration);
        double durHours = parseDouble(durText, -1);
        if (durHours <= 0) {
            inDuration.setError("Duración inválida");
            return false;
        }
        int tourMinutes = (int) Math.round(durHours * 60);

        int totalStopMinutes = 0;
        for (String label : stops) {
            int dotIdx = label.indexOf("·");
            int minIdx = label.toLowerCase(Locale.getDefault()).indexOf("min");
            if (dotIdx > 0 && minIdx > dotIdx) {
                String mid = label.substring(dotIdx + 1, minIdx)
                        .replace("·", "")
                        .replace("min", "")
                        .trim();
                int mins = parseInt(mid, 0);
                totalStopMinutes += mins;
            }
        }

        if (totalStopMinutes > tourMinutes) {
            showToast("La suma de minutos en paradas (" + totalStopMinutes +
                    ") no puede superar la duración del tour (" + tourMinutes + " min)");
            return false;
        }
        return true;
    }

    private void updateUiForStep() {
        setTitle((editingDocId == null ? "Nuevo Tour" : "Editar Tour") + " (" + step + "/4)");

        group1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        group2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        group3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        group4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        btnPrev.setEnabled(step > 1);
        btnNext.setText(step == 4
                ? (editingDocId == null ? "Publicar Tour" : "Guardar cambios")
                : "Siguiente ➜");
    }

    private String fmtDate(long millis) {
        SimpleDateFormat sdfUi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdfUi.format(new Date(millis));
    }

    private String safeText(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isEmpty(EditText e) {
        return TextUtils.isEmpty(safeText(e));
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
    }
}