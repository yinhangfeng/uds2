package com.mrwind.uds;

public interface Distance {
    /**
     * 获取两个店之间的距离
     * @param p1
     * @param p2
     * @return 距离 单位米
     */
    double getDistance(Point p1, Point p2);

    default double getDistance(TSPPoint p1, TSPPoint p2) {
        return getDistance(p1.point, p2.point);
    }
}
