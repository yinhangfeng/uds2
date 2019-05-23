package com.mrwind.uds.tsp;

import java.util.Arrays;

class Ant {

    static final int VISITED = 1;
    static final int END_POINT = 2;

    // 路径 对于终点等于起点的 终点不会放入 tour 但会计入 length
    int tour[];
    // 路径长度
    double length;
    // 访问数组 0 代表未访问 1 代表已访问 2 代表终点
    byte visited[];

    public Ant(int pointCount) {
        this.tour = new int[pointCount];
        this.visited = new byte[pointCount];
    }

    void visit(int tourIndex, int pointIndex) {
        assert visited[pointIndex] != VISITED;
        tour[tourIndex] = pointIndex;
        visited[pointIndex] = VISITED;
    }

    void clear() {
        Arrays.fill(visited, (byte) 0);
        length = 0;
    }

    void copyTour(int[] out) {
        System.arraycopy(tour, 0, out, 0, out.length);
    }

    @Override
    public String toString() {
        return "Ant{" +
                "length=" + length +
                ", tour=" + Arrays.toString(tour) +
                ", visited=" + Arrays.toString(visited) +
                '}';
    }
}
