package it.sergiogentile.bustorino.model;

/**
 * Created by sergiogentile on 19/01/2020.
 */

public class BusPosition {
    int numMezzo;
    int speed;
    double latitude;
    double longitude;
    String hour;
    String dir;

    public BusPosition(){}

    public BusPosition(int numMezzo, int speed, double latitude, double longitude, String hour, String dir) {
        this.numMezzo = numMezzo;
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hour = hour;
        this.dir = dir;
    }

    public void setNumMezzo(int numMezzo) {
        this.numMezzo = numMezzo;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public void setDir(String dir){
        this.dir = dir;
    }

    public int getNumMezzo() {
        return numMezzo;
    }

    public int getSpeed() {
        return speed;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getHour() {
        return hour;
    }

    public String getDir(){
        return dir;
    }

}
