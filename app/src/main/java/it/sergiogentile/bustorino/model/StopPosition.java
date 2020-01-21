package it.sergiogentile.bustorino.model;

/**
 * Created by sergiogentile on 19/01/2020.
 */

public class StopPosition {
    public void setNumFermata(int numFermata) {
        this.numFermata = numFermata;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    int numFermata;
    String dir;
    double lat;
    double lng;
    String name;
    String address;

    public int getNumFermata() {
        return numFermata;
    }

    public String getDir() {
        return dir;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
