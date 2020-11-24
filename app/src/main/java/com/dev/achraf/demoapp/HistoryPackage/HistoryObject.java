package com.dev.achraf.demoapp.HistoryPackage;

public class HistoryObject {
    private String rideId;
    private String time;
    private String destination;

    public HistoryObject(String rideId, String time, String destination){
        this.rideId = rideId;
        this.destination = destination;
        this.time = time;
    }

    public String getRideId(){return rideId;}
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getTime(){return time;}
    public void setTime(String time) {
        this.time = time;
    }

    public String getDestination(){return destination;}
    public void setDestination(String destination) {
        this.destination = destination;
    }
}
