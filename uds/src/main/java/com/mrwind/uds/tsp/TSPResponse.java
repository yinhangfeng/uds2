package com.mrwind.uds.tsp;

import com.mrwind.uds.Point;

import java.util.Arrays;
import java.util.List;

public class TSPResponse {

    /**
     * 总距离
     */
    public double length;
    /**
     * 合适度
     */
    public double fitness;
    /**
     * 路线 终点回到起点时 不会包括最后一个起点
     */
    public int[] tour;
    /**
     * 终点是否回到起点
     */
    public boolean endEqStart;
    /**
     * 产生最佳结果的迭代次数
     */
    public int iterationNum;
    public List<Point> originalPoints;

    @Override
    public String toString() {
        return "TSPResponse{" +
                "length=" + length +
                ", fitness=" + fitness +
                ", tour=" + Arrays.toString(tour) +
                ", endEqStart=" + endEqStart +
                ", iterationNum=" + iterationNum +
                '}';
    }
}