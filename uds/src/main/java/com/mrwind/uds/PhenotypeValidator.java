package com.mrwind.uds;

import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;

import java.util.Arrays;
import java.util.function.Predicate;

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
        return true;
    }
}
