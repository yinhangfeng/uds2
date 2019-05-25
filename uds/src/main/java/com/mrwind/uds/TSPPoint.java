package com.mrwind.uds;

public class TSPPoint {
    public Point point;
    public int index;
    // 依赖的任务点索引 只有依赖的任务点完成了 才能开始这个任务点 dependency.endTime > endTime
    // -1 代表没有依赖
    // TODO 支持多依赖
    public int dependency = -1;

    public TSPPoint(Point point, int index, int dependency) {
        this.point = point;
        this.index = index;
        this.dependency = dependency;
    }
}
