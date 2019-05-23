package com.mrwind.uds.common;

public class Point implements Cloneable {
    public long x;
    public long y;
    public float lng;
    public float lat;
    // 点的权重
    public float weight;
    // 父 Point 索引
    public int parent = -1;
    public String id;

    public Point() {

    }

    public Point(float lng, float lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public Point(float lng, float lat, String id) {
        this.lng = lng;
        this.lat = lat;
        this.id = id;
    }

    public Point(float lng, float lat, float weight, String id) {
        this.lng = lng;
        this.lat = lat;
        this.weight = weight;
        this.id = id;
    }

    @Override
    public Point clone() {
        try {
            return (Point) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "x:" + x +
                ", y:" + y +
                ", lng:" + lng +
                ", lat:" + lat +
                ", weight: " + weight +
                ", id:'" + id + '\'' +
                '}';
    }
}
