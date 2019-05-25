package com.mrwind.uds.util;

import com.mrwind.uds.Point;

import java.util.List;

/**
 * 高斯投影参考
 * https://github.com/lcj5811/JavaDemo/blob/master/src/com/lee/gis/coordinate/CoordinateConversion.java
 */
public class CoordinateUtils {

    // WGS-84坐标系
    public static final int COORDINATE_WGS84 = 0;
    // 西安-80坐标系
    public static final int COORDINATE_XIAN80 = 1;
    // 高斯平面坐标
    public static final int COORDINATE_GAUSS = 10;

    static class TuoQiuJiChun {
        long a;
        double b;

        TuoQiuJiChun(long a, double b) {
            this.a = a;
            this.b = b;
        }

        double getFirstE() {
            return Math.pow(Math.sqrt(a * a - b * b) / a, 2);
        }

        double getSecondE() {
            return Math.pow(Math.sqrt(a * a - b * b) / b, 2);
        }

        double getMLong() {
            return a;
        }

    }

    /**
     * @Description: 空间大地坐标转换高斯投影平面直角坐标
     * @param: latitude 纬度
     * @param: longitude 经度
     * @param: type 几度分带 3 或 6
     * @param: coordType 目标坐标系
     */
    public static double[] geodetic2Gauss(double latitude, double longitude, int type, int coordType) {

        TuoQiuJiChun tuoQiuJiChun;
        switch (coordType) {
            case COORDINATE_XIAN80:
                tuoQiuJiChun = new TuoQiuJiChun(6378140, 6356755.2881575287);
                break;
            case COORDINATE_WGS84:
            default:
                tuoQiuJiChun = new TuoQiuJiChun(6378137, 6356752.3142);
                break;
        }

        // 带号
        double beltNum;
        beltNum = Math.ceil((longitude - (type == 3 ? 1.5 : 0)) / type);

        if (type == 3 && beltNum * 3 == longitude - 1.5) {
            beltNum += 1;
        }

        double L0 = longitude - (beltNum * type - (type == 6 ? 3 : 0)); // 中央经线的度数
        double rB, tB, m;

        rB = latitude * Math.PI / 180;
        tB = Math.tan(rB);
        m = Math.cos(rB) * L0 * Math.PI / 180;

        double N = tuoQiuJiChun.getMLong() / Math.sqrt(1 - tuoQiuJiChun.getFirstE() * Math.sin(rB) * Math.sin(rB));

        double it2 = tuoQiuJiChun.getSecondE() * Math.pow(Math.cos(rB), 2);

        double x = 0.5 * m * m + (double) 1 / 24 * (5 - tB * tB + 9 * it2 + 4 * it2 * it2) * Math.pow(m, 4)
                + (double) 1 / 720 * (61 - 58 * tB * tB + Math.pow(tB, 4)) * Math.pow(m, 6);

        double m0 = tuoQiuJiChun.getMLong() * (1 - tuoQiuJiChun.getFirstE());
        double m2 = (double) 3 / 2 * tuoQiuJiChun.getFirstE() * m0;
        double m4 = (double) 5 / 4 * tuoQiuJiChun.getFirstE() * m2;
        double m6 = (double) 7 / 6 * tuoQiuJiChun.getFirstE() * m4;
        double m8 = (double) 9 / 8 * tuoQiuJiChun.getFirstE() * m6;
        double a0, a2, a4, a6, a8;
        a0 = m0 + (double) 1 / 2 * m2 + (double) 3 / 8 * m4 + (double) 5 / 16 * m6 + (double) 35 / 128 * m8;
        a2 = (double) 1 / 2 * m2 + (double) 1 / 2 * m4 + (double) 15 / 32 * m6 + (double) 7 / 16 * m8;
        a4 = (double) 1 / 8 * m4 + (double) 3 / 16 * m6 + (double) 7 / 32 * m8;
        a6 = (double) 1 / 32 * m6 + (double) 1 / 16 * m8;
        a8 = (double) 1 / 128 * m8;
        // 求子午线弧长
        double X1 = a0 * rB - a2 * Math.sin(2 * rB) * 0.5 + (double) 1 / 4 * a4 * Math.sin(4 * rB)
                - (double) 1 / 6 * a6 * Math.sin(6 * rB) + (double) 1 / 8 * a8 * Math.sin(8 * rB);

        double X = X1 + N * x * tB;
        double Y = N
                * (m + (double) 1 / 6 * (1 - tB * tB + it2) * Math.pow(m, 3) + (double) 1 / 720
                * (5 - 18 * tB * tB + Math.pow(tB, 4) + 14 * it2 - 58 * tB * tB * it2) * Math.pow(m, 5))
                + 500000;

        return new double[]{X, Y};
    }

    public static void transformPoints(List<Point> points, int coordType) {
        if (coordType == COORDINATE_GAUSS) {
            return;
        }
        double[] xy;
        for (Point point : points) {
            xy = CoordinateUtils.geodetic2Gauss(point.lat, point.lng, 3, COORDINATE_WGS84);
            // 不需要 double 精度 long 就行(精确到米)
            point.x = Math.round(xy[0]);
            point.y = Math.round(xy[1]);
        }
    }
}
