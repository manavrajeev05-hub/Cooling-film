package com.example.coolingfilmmonitor;

public class AlertManager {
    private double dangerThreshold = 30.0; // default

    public void setDangerThreshold(double value) {
        dangerThreshold = value;
    }

    public boolean isDanger(double temperature) {
        return temperature >= dangerThreshold;
    }
}
