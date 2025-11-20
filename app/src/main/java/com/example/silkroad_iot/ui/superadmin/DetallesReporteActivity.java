package com.example.silkroad_iot.ui.superadmin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.silkroad_iot.R;
import com.example.silkroad_iot.data.EmpresaFb;
import com.example.silkroad_iot.databinding.ActivitySuperadminDetallesReporteBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.firebase.Timestamp;
import com.google.firebase.database.collection.LLRBNode;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


import android.os.Environment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class DetallesReporteActivity extends AppCompatActivity {

    private ActivitySuperadminDetallesReporteBinding binding;
    private FirebaseFirestore db;
    private String id="1";
    private String TAG = "DetallesReporteActivity";
    private HashMap<Timestamp, Double> data = new HashMap<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetallesReporteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbar3);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        Intent i = getIntent();
        EmpresaFb empresa = (EmpresaFb) i.getSerializableExtra("empresa");
        if (empresa != null) {
            id = empresa.getId();
            // Opcional: Mostrar nombre de la empresa en la toolbar
            getSupportActionBar().setTitle("Reporte: " + empresa.getNombre());
        }

        // Necesaria...
        // 1. Iniciar la cadena de llamadas asíncronas
        obtenerIdsTours(ids -> {
            // 2. Este bloque se ejecuta cuando los IDs de los tours están listos
            cargarDatosReporte(ids, data -> {
                // 3. ¡Este bloque se ejecuta cuando TODOS los datos del historial están listos!
                // Ahora es seguro procesarlos y actualizar la UI.

                // Procesar los datos para obtener los puntos del gráfico
                LineDataSet ds1 = new LineDataSet(getEntriesLast7Days(data), "Últimos 7 días");
                ds1.setColor(Color.BLUE);
                ds1.setCircleColor(Color.BLUE);

                LineDataSet ds2 = new LineDataSet(getEntriesLast7Months(data), "Últimos 7 meses");
                ds2.setColor(Color.RED);
                ds2.setCircleColor(Color.RED);

                LineDataSet ds3 = new LineDataSet(getEntriesLast7Years(data), "Últimos 7 años");
                ds3.setColor(Color.GREEN);
                ds3.setCircleColor(Color.GREEN);

                // Configurar el gráfico
                LineData lineData= new LineData(ds1, ds2, ds3);
                //LineData lineData= new LineData(ds1);
                LineChart chart = binding.lchart;
                chart.setData(lineData);

                // Añadir más configuración al gráfico (descripción, ejes, etc.)
                chart.getDescription().setText("Ingresos por periodo");
                chart.invalidate(); // Refrescar el gráfico
            });
        });

    }


    private void obtenerIdsTours(FirestoreCallback firestoreCallback){
        db.collection("tours")
                .whereEqualTo("empresaId", id) // Asumo que el campo es 'id_empresa', ajústalo si es diferente
                .get()
                .addOnSuccessListener(snap -> {
                    ArrayList<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap){
                        ids.add(d.getId());
                    }
                    // Llama al callback con los IDs obtenidos
                    firestoreCallback.onCallback(ids);
                })
                .addOnFailureListener(e -> {
                    // Maneja el error, quizás mostrando un Toast
                    Toast.makeText(this, "Error al obtener tours: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    firestoreCallback.onCallback(new ArrayList<>()); // Llama con lista vacía para no bloquear
                });
    }

    private interface FirestoreCallback {
        void onCallback(ArrayList<String> ids);
    }

    // Modifica getEntries7 (ahora lo llamaremos cargarDatosReporte)
    private void cargarDatosReporte(ArrayList<String> ids, final DataCallback callback) {
        // Si no hay tours, no hay nada que buscar.
        if (ids == null || ids.isEmpty()) {
            callback.onDataLoaded(new HashMap<>()); // Devuelve data vacía
            return;
        }

        HashMap<Timestamp, Double> data = new HashMap<>();
        final int[] tasksCompleted = {0}; // Contador para saber cuándo han terminado todas las tareas

        String finalizada="finalizada";
        for(String tourId : ids){
            //Log.d(TAG, "Tour ID: " + tourId);
            db.collection("tours_history")
                    .whereEqualTo("id_tour", tourId)
                    .whereEqualTo("estado", finalizada)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot d : task.getResult()) {
                                Timestamp fecha = d.getTimestamp("fechaReserva");
                                Log.d(TAG, "Fecha: " + fecha);
                                Double fix = d.getDouble("precio") != null ? d.getDouble("precio") : 0.0;//XD funciona pax pero no precio ffffff
                                data.put(fecha, fix);
                            }
                        }
                        tasksCompleted[0]++;
                        if (tasksCompleted[0] == ids.size()) {
                            callback.onDataLoaded(data);
                        }
                    });
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu_download, menu);
        return true;
    }

    public interface DataCallback {
        void onDataLoaded(HashMap<Timestamp, Double> data);
    }

    private ArrayList<Entry> getEntriesLast7Days(HashMap<Timestamp, Double> data) {
        // aMapa para acumular los montos por día
        Map<LocalDate, Double> dailyTotals = new TreeMap<>();
        LocalDate today = LocalDate.now();
        // Inicializar los últimos 7 días con un monto de 0.0
        for (int i = 0; i < 7; i++) {
            dailyTotals.put(today.minusDays(i), 0.0);
        }

        // Acumular los montos del HashMap 'data'
        for (Map.Entry<Timestamp, Double> entry : data.entrySet()) {
            LocalDate entryDate = entry.getKey().toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            if (dailyTotals.containsKey(entryDate)) {
                dailyTotals.put(entryDate, dailyTotals.get(entryDate) + entry.getValue());
            }
        }

        // Convertir el mapa de totales a Entries para el gráfico
        ArrayList<Entry> entries = new ArrayList<>();
        int dayIndex = 0;
        for (LocalDate date : dailyTotals.keySet().stream().sorted().collect(Collectors.toList())) {
            // El eje X puede ser simplemente un índice (0 a 6)
            entries.add(new Entry(dayIndex, dailyTotals.get(date).floatValue()));
            dayIndex++;
        }
        return entries;
    }


    private ArrayList<Entry> getEntriesLast7Months(HashMap<Timestamp, Double> data) {
        Map<YearMonth, Double> monthlyTotals = new TreeMap<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 7; i++) {
            monthlyTotals.put(currentMonth.minusMonths(i), 0.0);
        }

        for (Map.Entry<Timestamp, Double> entry : data.entrySet()) {
            YearMonth entryMonth = YearMonth.from(entry.getKey().toDate().toInstant().atZone(ZoneId.systemDefault()));
            if (monthlyTotals.containsKey(entryMonth)) {
                monthlyTotals.put(entryMonth, monthlyTotals.get(entryMonth) + entry.getValue());
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        int monthIndex = 0;
        // Iteramos en orden natural (del más antiguo al más nuevo) para el gráfico
        for (Map.Entry<YearMonth, Double> entry : monthlyTotals.entrySet()) {
            entries.add(new Entry(monthIndex, entry.getValue().floatValue()));
            monthIndex++;
        }
        return entries;
    }

    private ArrayList<Entry> getEntriesLast7Years(HashMap<Timestamp, Double> data) {
        Map<Integer, Double> yearlyTotals = new TreeMap<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i < 7; i++) {
            yearlyTotals.put(currentYear - i, 0.0);
        }

        for (Map.Entry<Timestamp, Double> entry : data.entrySet()) {
            int entryYear = entry.getKey().toDate().toInstant().atZone(ZoneId.systemDefault()).getYear();
            if (yearlyTotals.containsKey(entryYear)) {
                yearlyTotals.put(entryYear, yearlyTotals.get(entryYear) + entry.getValue());
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        int yearIndex = 0;
        for (Map.Entry<Integer, Double> entry : yearlyTotals.entrySet()) {
            entries.add(new Entry(yearIndex, entry.getValue().floatValue()));
            yearIndex++;
        }
        return entries;
    }


    /*Excel*/
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permiso concedido. Iniciar la descarga de datos.
                    Toast.makeText(this, "Permiso concedido. Iniciando descarga...", Toast.LENGTH_SHORT).show();
                    iniciarProcesoDeExportacion();
                } else {
                    // Permiso denegado. Informar al usuario.
                    Toast.makeText(this, "Permiso de almacenamiento denegado. No se puede exportar el archivo.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.descargar_excel) { // Asegúrate de que este es el ID de tu item
            verificarYExportar();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Maneja el clic en la flecha de "atrás"
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void verificarYExportar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Ya tienes el permiso, inicia la exportación.
            iniciarProcesoDeExportacion();
        } else {
            // No tienes el permiso, solicítalo.
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
    private void iniciarProcesoDeExportacion() {
        obtenerIdsTours(ids -> {
            // Este callback se ejecuta cuando los IDs están listos
            obtenerDatosParaExcel(ids, this::crearArchivoExcel);
        });
    }

    // Obtener todos los datos necesarios para el Excel
    private void obtenerDatosParaExcel(ArrayList<String> ids, final ExcelDataCallback callback) {
        if (ids == null || ids.isEmpty()) {
            callback.onDataReady(new ArrayList<>()); // Devuelve lista vacía
            return;
        }

        ArrayList<Map<String, Object>> reportData = new ArrayList<>();
        final int[] tasksCompleted = {0};

        for (String tourId : ids) {
            db.collection("tours_history")
                    .whereEqualTo("id_tour", tourId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot d : task.getResult()) {
                                Map<String, Object> rowData = new HashMap<>();
                                rowData.put("fecha", d.getTimestamp("fechaReserva"));
                                rowData.put("precio", d.getDouble("precio") != null ? d.getDouble("precio") : 0.0);
                                rowData.put("estado", d.getString("estado"));
                                reportData.add(rowData);
                            }
                        }

                        tasksCompleted[0]++;
                        if (tasksCompleted[0] == ids.size()) {
                            callback.onDataReady(reportData);
                        }
                    });
        }
    }

    private interface ExcelDataCallback {
        void onDataReady(ArrayList<Map<String, Object>> data);
    }

    private void crearArchivoExcel(ArrayList<Map<String, Object>> reportData) {
       if (reportData.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Historial de Tours");

        // Crea la fila de encabezado
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Fecha");
        headerRow.createCell(1).setCellValue("Precio");
        headerRow.createCell(2).setCellValue("Estado");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        int rowNum = 1;
        for (Map<String, Object> rowData : reportData) {
            Row row = sheet.createRow(rowNum++);
            Timestamp fechaTimestamp = (Timestamp) rowData.get("fecha");
            String fechaFormateada = (fechaTimestamp != null) ? sdf.format(fechaTimestamp.toDate()) : "N/A";

            Double precio = (Double) rowData.get("precio");
            double precioValor = (precio != null) ? precio : 0.0;

            String estado = (String) rowData.get("estado");

            row.createCell(0).setCellValue(fechaFormateada);
            row.createCell(1).setCellValue(precioValor);
            row.createCell(2).setCellValue(estado);
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            String fileName = "Reporte_SilkRoad_" + System.currentTimeMillis() + ".xlsx";
            File file = new File(downloadsDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            runOnUiThread(() -> Toast.makeText(this, "Reporte guardado en Descargas/" + fileName, Toast.LENGTH_LONG).show());

        } catch (IOException e) {
            Log.e(TAG, "Error al escribir el archivo de Excel", e);
            runOnUiThread(() -> Toast.makeText(this, "Error al guardar el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

}