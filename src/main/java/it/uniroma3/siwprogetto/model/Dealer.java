package it.uniroma3.siwprogetto.model;


public class Dealer {
    private String name;
    private double lat;
    private double lng;

    // Costruttore vuoto
    public Dealer() {}

    // Costruttore con parametri
    public Dealer(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    // Getter e Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
