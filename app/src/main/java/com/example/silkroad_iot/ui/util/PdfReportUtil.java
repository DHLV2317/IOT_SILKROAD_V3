package com.example.silkroad_iot.ui.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportUtil {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public static void createReservationsPdf(Context ctx, List<Object> items){
        try {
            PdfDocument pdf = new PdfDocument();
            Paint p = new Paint();
            int pageWidth = 595, pageHeight = 842; // A4 puntos

            int y = 40;
            PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create());
            Canvas c = page.getCanvas();

            p.setTextSize(16); p.setFakeBoldText(true);
            c.drawText("Reporte de Reservas", 40, y, p); y += 20;
            p.setTextSize(10); p.setFakeBoldText(false);
            c.drawText("Generado: " + SDF.format(new Date()), 40, y, p); y += 20;

            for (int i=0; i<items.size(); i++){
                Object r = items.get(i);
                String tour   = get(r,"tourName");
                String cli    = get(r,"clientName");
                String st     = get(r,"status");
                String total  = get(r,"total");
                String date   = getDate(r,"date");
                String rating = get(r,"rating");

                String ratingPart = rating.isEmpty() ? "" : " | ⭐ " + rating;

                String line = (i+1)+". "+(tour.isEmpty()?"(Sin tour)":tour)+" | "+cli+" | "+date+
                        " | S/ "+(total.isEmpty()?"0":total)+" | "+(st.isEmpty()?"pendiente":st) + ratingPart;

                y += 16;
                if (y > pageHeight-40){
                    pdf.finishPage(page);
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, (i/40)+2).create());
                    c = page.getCanvas(); y = 40;
                }
                c.drawText(line, 40, y, p);
            }
            pdf.finishPage(page);

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "reporte_reservas.pdf");
            FileOutputStream fos = new FileOutputStream(out);
            pdf.writeTo(fos);
            fos.close();
            pdf.close();
            Toast.makeText(ctx, "PDF guardado en Descargas/reporte_reservas.pdf", Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(ctx, "Error creando PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String get(Object o, String n){
        try {
            var f=o.getClass().getDeclaredField(n);
            f.setAccessible(true);
            Object v=f.get(o);
            return v==null? "" : String.valueOf(v);
        }
        catch(Throwable ignore){ return ""; }
    }

    private static String getDate(Object o, String n){
        try {
            var f=o.getClass().getDeclaredField(n); f.setAccessible(true);
            Object v=f.get(o);
            if (v instanceof Date) return SDF.format((Date) v);
            if (v instanceof Number) return SDF.format(new Date(((Number) v).longValue()));
            return "";
        } catch(Throwable ignore){ return ""; }
    }
}