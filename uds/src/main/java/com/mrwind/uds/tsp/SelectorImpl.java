package com.mrwind.uds.tsp;

import com.mrwind.uds.TSPPoint;

public class SelectorImpl implements Selector {
    @Override
    public double handleWeight(double weight, Ant ant, TSPPoint currentPoint, TSPPoint handlePoint) {
        return 0;
    }

    @Override
    public double calcFitness(Ant ant, TSPPoint currentPoint, TSPPoint nextPoint) {
        return 0;
    }

    @Override
    public boolean isBetter(double currentBestLength, double currentBestFitness, Ant ant) {
        return true;
    }
}
