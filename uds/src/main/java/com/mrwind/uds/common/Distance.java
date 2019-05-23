package com.mrwind.uds.common;

public class Distance {

    /**
     * 获取直线距离
     */
    public static double getDistance(Point p1, Point p2) {
        long dx = p1.x - p2.x;
        long dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 获取直角距离
     * 距离越远应该向直线距离靠近(因为距离越远越有可能存在直线的路)
     * 需要符合两边之和大于第三边?
     */
    public static double getRightAngleDistance(Point p1, Point p2) {
        // 假设 10 公里时 直角距离等于直线距离(线性变化)
        double distance = getDistance(p1, p2);
        double ratio = distance / 10000;
        if (ratio > 0.8) {
            ratio = 0.8f;
        }
        double rightAngleDistance = Math.round(Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y));
        double result = ratio * distance + (1 - ratio) * rightAngleDistance;
//        System.out.println("getRightAngleDistance distance: " + distance + " rightAngleDistance: " + rightAngleDistance + " result: " + result + " ratio: " + ratio);
        return result;
    }

    public static int getDistanceInt(Point p1, Point p2) {
        return (int) Math.round(getDistance(p1, p2));
    }

    public static int getRightAngleDistanceInt(Point p1, Point p2) {
        return (int) Math.round(getRightAngleDistance(p1, p2));
    }
}
