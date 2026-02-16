package com.example.coolingfilmmonitor;

public class Dingo {
    public double parseTemp(byte[] rawData) {
        if (rawData == null || rawData.length < 2) {
            throw new IllegalArgumentException("Invalid temperature data");
        }

        int low = rawData[0] & 0xFF;
        int high = rawData[1];

        int combined = (high << 8) | low;

        return combined / 100.0;
    }
    public static void main(String[] args) {
        Dingo parser = new Dingo();

        byte[] fakeData = new byte[] {(byte) 0xE6, 0x09};
        double temp = parser.parseTemp(fakeData);

        System.out.println(temp);

    }
}
