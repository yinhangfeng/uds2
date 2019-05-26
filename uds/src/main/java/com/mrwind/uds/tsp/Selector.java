package com.mrwind.uds.tsp;

import com.mrwind.uds.TSPPoint;

public interface Selector {

    Selector LENGTH_SELECTOR = new Selector() {
        @Override
        public double handleWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint) {
            return weight;
        }

        @Override
        public double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint) {
            return 0;
        }

        @Override
        public boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant) {
            return ant.length < currentBestLength;
        }
    };

    /**
     * 处理将要访问点的 weight
     * 在蚁群算法访问下一个点的时候调用
     * @param weight
     * @param ant
     * @param currentPoint 如果是第一个点 则为 null
     * @param handlePoint
     * @return 新的 weight
     */
    double handleWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint);

    /**
     * 计算合适度 越小越好
     * 每访问一个点会调用一次
     * @param ant
     * @param currentPoint 如果是第一个点 则为 null
     * @param nextPoint
     */
    double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint);

    /**
     * 比较新的蚂蚁是否比原来的更好
     */
    boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant);
}
