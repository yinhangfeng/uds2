package com.mrwind.uds;

import com.mrwind.uds.util.DistanceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DistanceImpl implements Distance {

    // 是否使用直角距离
    private boolean rightAngleDistance = true;
    // 距离矩阵 i j 相同为 -1
    private double distance[][];
//    public boolean used[][];

    public DistanceImpl(List<Point> points, boolean rightAngleDistance) {
        initDistance(points, rightAngleDistance, 0);
    }

    public DistanceImpl(List<Driver> drivers, List<Shipment> shipments, boolean rightAngleDistance) {
        List<Point> points = new ArrayList<>();
        for (Driver driver : drivers) {
            points.add(driver.position);
        }
        int driverEndIndex = points.size();

        for (Shipment shipment : shipments) {
            points.add(shipment.sender);
            points.add(shipment.receiver);
        }

        initDistance(points, rightAngleDistance, driverEndIndex);
    }

    public DistanceImpl(Driver driver, List<Shipment> shipments, boolean rightAngleDistance) {
        this(Collections.singletonList(driver), shipments, rightAngleDistance);
    }

    private void initDistance(List<Point> points, boolean rightAngleDistance, int driverEndIndex) {
        int pointCount = points.size();
        distance = new double[pointCount][pointCount];
//        used = new boolean[pointCount][pointCount];
        double weight;
        for (int i = 0; i < pointCount; ++i) {
            distance[i][i] = -1;
            // 给所有 point 唯一的编号
            // pointIndex 的存在意味着 point 不能用于多线程环境
            // TODO
            points.get(i).pointIndex = i;
            int j;
            if (i < driverEndIndex) {
                j = driverEndIndex;
            } else {
                j = i + 1;
            }
            for (; j < pointCount; ++j) {
                if (rightAngleDistance) {
                    weight = DistanceUtils.getRightAngleDistance(points.get(i), points.get(j));
                } else {
                    weight = DistanceUtils.getDistance(points.get(i), points.get(j));
                }
                distance[i][j] = distance[j][i] = weight;
            }
        }
    }

    @Override
    public double getDistance(Point p1, Point p2) {
//        used[p1.pointIndex][p2.pointIndex] = true;
        return distance[p1.pointIndex][p2.pointIndex];
    }
}
