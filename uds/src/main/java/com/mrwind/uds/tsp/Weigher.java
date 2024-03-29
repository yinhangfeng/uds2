package com.mrwind.uds.tsp;

import com.mrwind.uds.TSPPoint;

/**
 * 扩展蚁群的权值计算
 */
public interface Weigher {

    /**
     * 默认的权值计算器
     * 按距离计算权值
     */
    Weigher LENGTH_WEIGHER = new Weigher() {
        @Override
        public double processWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint) {
            return weight;
        }

        @Override
        public double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint, double distance) {
            return 0;
        }

        @Override
        public boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant) {
            return ant.length < currentBestLength;
        }

        @Override
        public double getPheromoneIncrement(Ant ant) {
            return 1 / ant.length;
        }
    };

    /**
     * 处理将要访问点的 weight
     * 在蚁群算法访问下一个点的时候调用
     * 相当于为下一个点增加启发式信息
     * @param weight
     * @param ant
     * @param currentPoint 如果是第一个点 则为 null
     * @param handlePoint
     * @return 新的 weight
     */
    double processWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint);

    /**
     * 计算合适度 越小越好
     * 每访问一个点会调用一次
     * @param ant
     * @param currentPoint 如果是第一个点 则为 null
     * @param nextPoint
     * @param distance 上一个点到这个点的距离
     */
    double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint, double distance);

    /**
     * 比较新的蚂蚁是否比原来的更好
     */
    boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant);

    /**
     * 获取一次迭代之后的信息素增量
     */
    double getPheromoneIncrement(Ant ant);
}
