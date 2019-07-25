package com.mrwind.uds;

/**
 * 任务点
 * TODO 关联 Driver Shipment ?
 */
public class Point implements Cloneable {

    public static final int TYPE_START = 0;
    public static final int TYPE_SENDER = 1;
    public static final int TYPE_RECEIVER = 2;
    public static final int TYPE_END = 3;

    public long x;
    public long y;
    public float lng;
    public float lat;
    // 任务开始时间 早于这个时间到达可能需等待
    // unix 时间戳毫秒
    public long startTime = -1;
    // 任务结束时间 晚于这个时间到达会产生额外代价 endTime > startTime
    public long endTime = -1;
    // 内部使用 点的索引 在一次分配中 所有点都有唯一索引 主要方便计算两个点之间距离时做距离矩阵缓存
    // TODO pointIndex 的存在意味着 point 不能用于多线程个环境
    int pointIndex = -1;
    // 依赖的任务点索引 只有依赖的任务点完成了 才能开始这个任务点 dependency.endTime > endTime
    // -1 代表没有依赖
    // TODO 支持多依赖
    public int dependency = -1;
    public String id;
    // 点的类型
    public int type = -1;

    public Point() {

    }

    public Point(float lng, float lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public Point(float lng, float lat, String id) {
        this.lng = lng;
        this.lat = lat;
        this.id = id;
    }

    @Override
    public Point clone() {
        try {
            return (Point) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{lng:" + lng +
                ", lat:" + lat +
                ", type:" + type +
                ", id:'" + id + '\'' +
                '}';
    }
}
