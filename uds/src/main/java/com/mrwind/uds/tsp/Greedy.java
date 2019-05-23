package com.mrwind.uds.tsp;

import java.util.Arrays;

class Greedy {
    /**
     * 使用贪心算法尝试翻转子路径 看是否能得到更好的结果
     *
     * @param tour       原始路径 如果找到更优的路劲会被修改
     * @param length     原路径长度
     * @param pointCount 点数
     * @param distance   距离矩阵
     * @param tree       点之间关系数组
     * @return 新的路劲长度 比原来小说明有子路径翻转了
     * <p>
     * 暂不支持终点返回起点
     */
    static double greedyTour(int[] tour, double length, int pointCount, double[][] distance, int[] tree) {
//        System.out.println("greedyTour length: " + length + " pointCount: " + pointCount + " distance: " + Arrays.deepToString(distance) + " tree: " + Arrays.toString(tree));

        int i;
        int j;
        int k;
        boolean localMaxima = false;
        double newLength;
        double iBeforeDistance;
        boolean jIsLast;
        int parent;

        int iteration = 0;
        while (!localMaxima && iteration++ < 16) {
            localMaxima = true;
            for (i = 1; i < pointCount - 1; ++i) {
                iBeforeDistance = distance[tour[i - 1]][tour[i]];
                loopJ:
                for (j = i + 1; j < pointCount; ++j) {
                    parent = tree[tour[j]];
                    if (parent >= 0) {
                        for (k = i; k < j; ++k) {
                            // i 到 j 之间有点依赖关系 不能翻转
                            if (tour[k] == parent) {
                                break loopJ;
                            }
                        }
                    }
                    jIsLast = j + 1 == pointCount;
                    // 暂不支持终点返回起点 所以最后一个点与后一个点的距离为 0
                    newLength = length - iBeforeDistance - (jIsLast ? 0 : distance[tour[j]][tour[j + 1]]) + distance[tour[i - 1]][tour[j]] + (jIsLast ? 0 : distance[tour[i]][tour[j + 1]]);
                    if (newLength < length) {
//                        System.out.println("greedyTour newLength: " + newLength + " length: " + length + " i: " + i + " j: " + j
//                                + " i - 1 -> i: " + iBeforeDistance + " j -> j + 1: " + (jIsLast ? 0 : distance[tour[j]][tour[j + 1]])
//                                + " i - 1 -> j: " + distance[tour[i - 1]][tour[j]] + " i -> j + 1: " + (jIsLast ? 0 : distance[tour[i]][tour[j + 1]])
//                        );
                        localMaxima = false;
                        reverseSubTour(tour, i, j);
                        length = newLength;
                        break;
                    }
                }
            }
        }

        return length;
    }

    private static void reverseSubTour(int[] tour, int i, int j) {
        for (; i < j; ++i, --j) {
            tour[i] ^= tour[j];
            tour[j] ^= tour[i];
            tour[i] ^= tour[j];
        }
    }
}
