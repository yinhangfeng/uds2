package com.mrwind.uds.tsp;

import com.mrwind.uds.Driver;
import com.mrwind.uds.Point;
import com.mrwind.uds.TSPPoint;

/**
 * 按司机工作时间以及取派点时间窗计算 tspResponse
 * ant.tspResponse 代表总消耗时间 单位 ms
 */
public class DriverTimeSelector implements Selector {

    private Driver driver;
    private long currentTime;

    public DriverTimeSelector(Driver driver, long currentTime) {
        this.driver = driver;
        this.currentTime = currentTime;
    }

    @Override
    public double processWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint) {
        Point point = handlePoint.point;
        if (point.startTime < 0 && point.endTime < 0) {
            // 无时间窗口限制
            return weight;
        }

        // 当前已排任务的结束时间
        long currentTaskEndTime = (long) ((driver.workStartTime > currentTime ? driver.workStartTime : currentTime) + ant.fitness);

        if (currentTaskEndTime < point.startTime) {
            if (point.startTime - currentTaskEndTime >= 3600 * 2 * 1000) {
                // 开始时间窗 2小时之前
                return weight * 0.001;
            }
            // 开始时间窗 2 小时 到开始时间均匀变化
            return weight * (0.00000013875 * (point.startTime - currentTaskEndTime) + 0.001);
        } else if (currentTaskEndTime >= point.endTime) {
            // 超过 endTime 达到最大值
            return AntColonyTSP.MAX_VALUE;
        }

        // 开始时间和结束时间之间

        long xe = Math.max(Math.min((point.endTime - point.startTime) / 4, 3600 * 1000), 30 * 60 * 1000);
        long timeX = point.endTime - xe;

        // timeX 之前比例为1
        if (currentTaskEndTime < timeX) {
            return weight;
        }

//        System.out.println("processWeight after timeX xe: " + xe + " " + (currentTaskEndTime - timeX) + " " + (AntColonyTSP.MAX_VALUE / xe * (currentTaskEndTime - timeX) + 1));

        // timeX 之后 快速变化
        return weight * (AntColonyTSP.MAX_VALUE / xe * (currentTaskEndTime - timeX) + 1);
    }

    @Override
    public double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint, double distance) {
        // 取件时间
        long pickTime = 4 * 60 * 1000;
        // 派件时间
        long deliveryTime = 8 * 60 * 1000;
        // 速度
        double speed = 30 * 1000 / 3600000d;

        // 当前已排任务的结束时间
        long currentTaskEndTime = (long) ((driver.workStartTime > currentTime ? driver.workStartTime : currentTime) + ant.fitness);

        Point point = nextPoint.point;

        long addTime = 0;

        // 行驶时间
        if (currentPoint != null) {
            addTime += distance / speed;
        }

        if (point.startTime >= 0) {
            // 太早到 惩罚到取件开始需要时间的比例
            if (currentTaskEndTime + addTime < point.startTime) {
                addTime += (point.startTime - (currentTaskEndTime + addTime)) * 0.6;
            }
        }

        if (point.endTime >= 0) {
            // 晚于截至时间到达 惩罚
            if (currentTaskEndTime + addTime > point.endTime) {
                addTime += (currentTaskEndTime + addTime - point.endTime) * 2 + 10 * 60 * 1000;
            }
        }

        // 取派件时间
        if (point.type == Point.TYPE_SENDER) {
            addTime += pickTime;
        } else if (point.type == Point.TYPE_RECEIVER) {
            addTime += deliveryTime;
        }

//        System.out.println("calcFitness oriFitness: " + ant.tspResponse + " addTime: " + addTime);

        ant.fitness += addTime;
        return ant.fitness;
    }

    @Override
    public boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant) {
        return ant.fitness < currentBestFitness;
    }
}
