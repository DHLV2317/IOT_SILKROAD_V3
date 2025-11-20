package com.example.silkroad_iot.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AdminRepository {
    private static final AdminRepository I = new AdminRepository();
    public static AdminRepository get() { return I; }

    // ====== MODELOS ======

    // ---------- TOUR ----------
    public static class Tour {
        public String id;
        public String name;          // <— usado por adapters / UI
        public double price;
        public int people;           // capacidad o aforo
        public String description;
        public String imageUrl;
        public double rating;
        public Date   FechaTour;     // usado por algunas vistas

        // Campos que las vistas leen por reflexión (opcionalmente)
        public String duration;      // p.e. "3 días"
        public String langs;         // p.e. "Español/Inglés"
        public List<Stop> stops = new ArrayList<>();
        public List<Service> services = new ArrayList<>();
        public String assignedGuideName;   // para “Guía asignado”
        public Double paymentProposal;     // para “Pago propuesto”

        // Submodelos simples para “Paradas” y “Servicios”
        public static class Stop {
            public String address;
            public int minutes;
            public Stop(String address, int minutes){ this.address = address; this.minutes = minutes; }
        }
        public static class Service {
            public String name;
            public boolean included;
            public double price;
            public Service(String name, boolean included, double price){
                this.name = name; this.included = included; this.price = price;
            }
        }

        public Tour(String name, double price, int people,
                    String description, String imageUrl, double rating, Date fecha){
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.price = price;
            this.people = people;
            this.description = description;
            this.imageUrl = imageUrl;
            this.rating = rating;
            this.FechaTour = fecha;

            // Defaults razonables para que las pantallas tengan datos
            this.duration = "3 días";
            this.langs = "Español/Inglés";
            this.stops.add(new Stop("Jr. Puno", 30));
            this.services.add(new Service("Desayuno", true, 0));
            this.services.add(new Service("Almuerzo", true, 0));
            this.services.add(new Service("Cena", false, 12));
        }
    }

    // ---------- COMPANY ----------
    public static class Company {
        public String id;
        public String name;
        public String email;
        public String phone;
        public String address;
        public double lat;
        public double lng;
        public double rating;
        public String imageUrl;

        public List<Tour> tours = new ArrayList<>();

        public Company(String name, String email, String phone,
                       String address, double lat, double lng) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.rating = 4.0;
            this.imageUrl = null;
        }
    }

    // ---------- GUIDE ----------
    public static class Guide {
        public String id;
        public String name;
        public String langs;
        public String state; // Disponible / Ocupado

        // Campos opcionales que algunas pantallas usan por reflexión
        public String phone;
        public String email;
        public String currentTour;
        public List<String> history = new ArrayList<>();

        public Guide(String name, String langs, String state){
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.langs = langs;
            this.state = state;
        }
    }

    // ---------- RESERVATION ----------
    public static class Reservation {
        public String id;
        public String clientName;
        public Tour tour;
        public Date date;
        public int people;

        public Reservation(String clientName, Tour tour, Date date, int people){
            this.id = UUID.randomUUID().toString();
            this.clientName = clientName;
            this.tour = tour;
            this.date = date;
            this.people = people;
        }
    }

    // ====== DATOS INTERNOS ======
    private final List<Company> companies = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private List<ReservationVM> reservationsVM; // para la lista de reservas en UI

    private AdminRepository(){
        // Semilla de ejemplo
        Company c1 = new Company("SilkRoad Travel", "info@silkroad.com", "999111222",
                "Av. Principal 123", -12.06, -77.04);
        c1.tours.add(new Tour("Tour Histórico", 150.0, 10,
                "Recorrido por el centro histórico", null, 4.5, new Date()));
        c1.tours.add(new Tour("Tour Gastronómico", 200.0, 8,
                "Degustación de platos típicos", null, 4.8, new Date()));
        companies.add(c1);

        guides.add(new Guide("Juan Pérez", "Español/Inglés", "Disponible"));
        guides.add(new Guide("María Gómez", "Español/Francés", "Ocupado"));

        reservations.add(new Reservation("Carlos Ruiz", c1.tours.get(0), new Date(), 2));
    }

    // ====== CRUD COMPANIES ======
    public List<Company> getCompanies(){ return companies; }

    public Company findCompany(String id){
        for (Company c: companies) if (c.id.equals(id)) return c;
        return null;
    }

    public void addCompany(Company c){ companies.add(c); }

    public void updateCompany(Company c){ /* en memoria ya se modifica */ }

    public void deleteCompany(String id){
        Company c = findCompany(id);
        if (c!=null) companies.remove(c);
    }

    public Company getOrCreateCompany() {
        if (companies.isEmpty()) {
            Company c = new Company("", "", "", "", 0.0, 0.0);
            companies.add(c);
            return c;
        }
        return companies.get(0);
    }

    // ====== CRUD TOURS ======
    public List<Tour> getTours(){
        if (companies.isEmpty()) return Collections.emptyList();
        return companies.get(0).tours;
    }

    public void addTour(Tour t) {
        if (companies.isEmpty()) {
            Company c = new Company("Default Company", "info@default.com", "000000000",
                    "Dirección", 0.0, 0.0);
            companies.add(c);
        }
        companies.get(0).tours.add(t);
    }

    public void deleteTour(Tour t) {
        if (!companies.isEmpty()) {
            companies.get(0).tours.remove(t);
        }
    }

    public Tour getTourAt(int index) {
        if (companies.isEmpty()) return null;
        List<Tour> tours = companies.get(0).tours;
        if (index < 0 || index >= tours.size()) return null;
        return tours.get(index);
    }

    // ====== CRUD GUIDES ======
    public List<Guide> getGuides(){ return guides; }

    public Guide findGuide(String id){
        for (Guide g: guides) if (g.id.equals(id)) return g;
        return null;
    }

    public void addGuide(Guide g){ guides.add(g); }

    public void deleteGuide(String id){
        Guide g = findGuide(id);
        if (g!=null) guides.remove(g);
    }

    // ====== CRUD RESERVATIONS (modelo) ======
    public List<Reservation> getReservations(){ return reservations; }

    public Reservation findReservation(String id){
        for (Reservation r: reservations) if (r.id.equals(id)) return r;
        return null;
    }

    public void addReservation(Reservation r){ reservations.add(r); }

    public void deleteReservation(String id){
        Reservation r = findReservation(id);
        if (r!=null) reservations.remove(r);
    }

    // ====== ViewModels para UI (listas simples) ======
    public static class ReservationVM {
        public String id, client, tour, date;
        public double amount;
        public ReservationVM(String id, String client, String tour, String date, double amount){
            this.id=id; this.client=client; this.tour=tour; this.date=date; this.amount=amount;
        }
    }

    public List<ReservationVM> getReservationsVM(){
        if (reservationsVM == null){
            reservationsVM = new ArrayList<>();
            reservationsVM.add(new ReservationVM("r1","Juan Pérez","Machu Picchu","05-07-2025",120.50));
            reservationsVM.add(new ReservationVM("r2","María López","Valle Sagrado","10-12-2025",95.00));
        }
        return reservationsVM;
    }

    // ====== Reportes (resumen mock) ======
    public static class ReportSummary {
        public double totalRevenue;
        public int reservations;
        public String topService;
        public ReportSummary(double r,int c,String s){
            totalRevenue=r; reservations=c; topService=s;
        }
    }

    public ReportSummary getReportSummary(){
        return new ReportSummary(8500, 85, "Transportes VIP");
    }
}