package com.mrwind.uds.tsp;

import com.mrwind.uds.common.Point;

import java.util.Arrays;
import java.util.List;

public class Response {

    List<Point> points;
    public double length;
    public int[] tour;
    public boolean endEqStart;
    // 最短距离的迭代次数
    public int iterationNum;

    @Override
    public String toString() {
        return "Response{" +
                "length=" + length +
                ", tour=" + Arrays.toString(tour) +
                ", endEqStart=" + endEqStart +
                ", iterationNum=" + iterationNum +
                '}';
    }
}
