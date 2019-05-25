package com.mrwind.uds.tsp;

import java.util.Arrays;

public class TSPResponse {

    /**
     * 总距离
     */
    public double length;
    /**
     * 路线 终点回到起点时 不会包括最后一个起点
     */
    public int[] tour;
    /**
     * 终点是否回到起点
     */
    public boolean endEqStart;
    // 最短距离的迭代次数
    public int iterationNum;

    @Override
    public String toString() {
        return "TSPResponse{" +
                "length=" + length +
                ", tour=" + Arrays.toString(tour) +
                ", endEqStart=" + endEqStart +
                ", iterationNum=" + iterationNum +
                '}';
    }
}