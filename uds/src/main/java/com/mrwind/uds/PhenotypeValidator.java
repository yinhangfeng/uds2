package com.mrwind.uds;

import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * TODO 使用最小生成树 快速估算是否有效
 */
public class PhenotypeValidator implements Predicate<Phenotype<IntegerGene, Double>> {

    private static ThreadLocal<int[]> counterThreadLocal = new ThreadLocal<>();

    private int max;

    public PhenotypeValidator(int max) {
        this.max = max;
    }

    @Override
    public boolean test(Phenotype<IntegerGene, Double> integerGeneDoublePhenotype) {
        UDSChromosome chromosome = integerGeneDoublePhenotype.getGenotype().getChromosome().as(UDSChromosome.class);
        int length = chromosome.length();

        int[] counter = counterThreadLocal.get();
        if (counter == null || counter.length < length) {
            counter = new int[length];
            counterThreadLocal.set(counter);
        } else {
            Arrays.fill(counter, 0);
        }

        int maxValue = 0;
        int value;
        for (int i = 0; i < length; ++i) {
            value = ++counter[chromosome.getGene(i).intValue()];

            if (value > maxValue) {
                maxValue = value;
            }
        }

        if (maxValue > max) {
            System.out.println("PhenotypeValidator invalid maxValue: " + maxValue + " " + chromosome);
            return false;
        }

        integerGeneDoublePhenotype.getFitness();

        List<Driver> drivers = chromosome.response.drivers;
        List<Response.DriverAllocation> driverAllocations = chromosome.response.driverAllocations;
        for (int i = 0; i < drivers.size(); ++i) {
            Driver driver = drivers.get(i);
            if (driver.maxMileage > 0) {
                if (driverAllocations.get(i).response.length > driver.maxMileage * 2) {
                    System.out.println("xxxx" + driverAllocations.get(i).response.length + " " + driver.maxMileage);
                    return false;
                }
            }
        }


        return true;
    }
}
