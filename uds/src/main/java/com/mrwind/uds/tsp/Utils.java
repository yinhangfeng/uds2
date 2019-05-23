package com.mrwind.uds.tsp;

import com.mrwind.uds.common.Point;

import java.util.List;

class Utils {

//    static void calcDistanceMatrix(List<Point> points, boolean rightAngleDistance, double[][] distanceMatrix) {
//        int pointCount = points.size();
//        double weight;
//        for (int i = 0; i < pointCount; ++i) {
//            distanceMatrix[i][i] = -1;
//            for (int j = i + 1; j < pointCount; ++j) {
//                if (rightAngleDistance) {
//                    weight = Distance.getRightAngleDistance(points.get(i), points.get(j));
//                } else {
//                    weight = Distance.getDistance(points.get(i), points.get(j));
//                }
//                distanceMatrix[i][j] = distanceMatrix[j][i] = weight;
//            }
//        }
//    }

    static void pointsToTree(List<Point> points, int[] tree) {
        int pointCount = points.size();
        for (int i = 0; i < pointCount; ++i) {
            tree[i] = points.get(i).parent;
        }
    }
}
