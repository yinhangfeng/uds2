package com.mrwind.uds.test;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;

public class JeneticsTest {

    // 2.) Definition of the tspResponse function.
    private static Integer eval(Genotype<BitGene> gt) {
        return gt.getChromosome()
                .as(BitChromosome.class)
                .bitCount();
    }

    static void test0() {
        // 1.) Define the genotype (factory) suitable
        //     for the problem.
        Factory<Genotype<BitGene>> gtf =
                Genotype.of(BitChromosome.of(20, 0.5));

        // 3.) Create the execution environment.
        Engine<BitGene, Integer> engine = Engine
                .builder(JeneticsTest::eval, gtf)
                .build();

        // 4.) Start the execution (evolution) and
        //     collect the result.
        Genotype<BitGene> result = engine.stream()
                .limit(100)
                .collect(EvolutionResult.toBestGenotype());

        System.out.println("Hello World:\n" + result);
    }

    static Double test1Fitness(Genotype<IntegerGene> gt) {
        IntegerChromosome chromosome = gt.getChromosome().as(IntegerChromosome.class);

        int length = chromosome.length();
        double sum = 0;
        for (int i = 0; i < length; ++i) {
            sum += chromosome.getGene(i).intValue();
        }
        double avg = sum / length;
        double a = 0;
        for (int i = 0; i < length; ++i) {
            a += Math.pow(chromosome.getGene(i).intValue() - avg, 2);
        }
        double v = a / length;

        System.out.println("tspResponse thread:" + Thread.currentThread().getName() + " sum: " + sum + " avg: " + avg + " v: " + v);

        // 目标是使总和最大 也就是最好所有基因都为 9999
        // 如果只是求和的话 在基因数量比较多时 最终结果没比随机好多少

//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return sum;
    }

    static void test1() {
        Factory<Genotype<IntegerGene>> gtf = Genotype.of(IntegerChromosome.of(0, 9999, 2000));

        // 对于低代价的 tspResponse 函数 使用多线程反而会减速
        Engine<IntegerGene, Double> engine = Engine.builder(JeneticsTest::test1Fitness, gtf)
                .populationSize(1000)
                .offspringFraction(0.95)
                .executor(Runnable::run)
                .build();

        Genotype<IntegerGene> result = engine.stream().limit(100).collect(EvolutionResult.toBestGenotype());

        System.out.println("result:\n" + result);
        test1Fitness(result);
    }

    public static void main(String[] args) throws Exception {

//        test1();

        TSPTest.main(args);
    }
}

