package com.mrwind.uds;

public class Driver {
    // 配送员当前位置 无 startTime endTime 限制
    public Point pos;
    // 配送员回程点 无 startTime endTime 限制(?) dependency 为 posj
    // TODO 暂不支持
    public Point home;
    // 最大单量
    public int maxShipmentNum;
    // 最大里程(米)
    public int maxMileage;
    // 最大容量(计费单位)
    public int maxLoad;
    // 今天工作起始时间
    public long workStartTime;
    // 今天工作结束时间
    public long workEndTime;

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public Point getHome() {
        return home;
    }

    public void setHome(Point home) {
        this.home = home;
    }

    public int getMaxShipmentNum() {
        return maxShipmentNum;
    }

    public void setMaxShipmentNum(int maxShipmentNum) {
        this.maxShipmentNum = maxShipmentNum;
    }

    public int getMaxMileage() {
        return maxMileage;
    }

    public void setMaxMileage(int maxMileage) {
        this.maxMileage = maxMileage;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(int maxLoad) {
        this.maxLoad = maxLoad;
    }

    public long getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(long workStartTime) {
        this.workStartTime = workStartTime;
    }

    public long getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(long workEndTime) {
        this.workEndTime = workEndTime;
    }
}
