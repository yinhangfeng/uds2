package com.mrwind.uds;

import com.mrwind.uds.stat.EvolutionResultStatistics;
import com.mrwind.uds.tsp.AntColonyTSP;
import com.mrwind.uds.tsp.DriverTimeSelector;
import com.mrwind.uds.tsp.TSPResponse;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UDS {

    private long currentTime;

    public UDS() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            currentTime = simpleDateFormat.parse("2019-05-27 08:00").getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public Response run(List<Driver> driverList, List<Shipment> shipmentList) {
        int driverCount = driverList.size();
        int shipmentCount = shipmentList.size();

        List<Shipment> allShipmentList = new ArrayList<>(shipmentList);
        for (Driver driver : driverList) {
            if (driver.getAllocatedShipments() != null) {
                allShipmentList.addAll(driver.getAllocatedShipments());
            }
        }
        Distance distance = new DistanceImpl(driverList, allShipmentList, true);

        Factory<Genotype<IntegerGene>> gtf = Genotype.of(UDSChromosome.of(0, driverCount - 1, shipmentCount));

        Engine<IntegerGene, Double> engine = Engine.builder(gt -> {
            UDSChromosome chromosome = gt.getChromosome().as(UDSChromosome.class);
            int length = chromosome.length();
            List<Response.DriverAllocation> driverAllocations = new ArrayList<>(driverCount);
            for (int i = 0; i < driverCount; ++i) {
                driverAllocations.add(new Response.DriverAllocation());
            }
            for (int i = 0; i < length; ++i) {
                List<Shipment> driverShipments = driverAllocations.get(chromosome.getGene(i).intValue()).shipmentList;
                driverShipments.add(shipmentList.get(i));
            }

            AntColonyTSP antColonyTSP;

            double result = 0;
            for (int i = 0; i < driverCount; ++i) {
                Driver driver = driverList.get(i);
                Response.DriverAllocation driverAllocation = driverAllocations.get(i);
                List<Shipment> driverShipments = driverAllocation.shipmentList;
                List<Shipment> allocatedShipments = driver.getAllocatedShipments();
                if (allocatedShipments != null) {
                    driverShipments.addAll(allocatedShipments);
                }

                if (!driverShipments.isEmpty()) {
                    TSPResponse tspResponse;
                    if (allocatedShipments != null && allocatedShipments.size() == driverShipments.size() && driver.allocatedTSPResponseCache != null) {
                        tspResponse = driver.allocatedTSPResponseCache;
                    } else {
                        antColonyTSP = AntColonyTSP.obtain(driver, driverShipments)
                                .distance(distance)
//                                .selector(new DriverTimeSelector(driver, currentTime))
                                .driverAndShipments(driver, driverShipments);
                        tspResponse = antColonyTSP.run();
                        antColonyTSP.recycle();

                        if (allocatedShipments != null && allocatedShipments.size() == driverShipments.size()) {
                            // 缓存已分配运单的计算结果
                            driver.allocatedTSPResponseCache = tspResponse;
                        }
                    }

                    driverAllocation.response = tspResponse;

                    double driverFitness = tspResponse.length;
//                    double driverFitness = tspResponse.fitness;

//                    // 超最大单量惩罚
//                    if (driver.maxLoad > 0) {
//                        if (driverShipments.size() > driver.maxLoad) {
//                            driverFitness *= driverShipments.size() / ((double) driver.maxLoad);
//
//                            System.out.println("fitness maxLoad " + driverShipments.size() / ((double) driver.maxLoad));
//                        }
//                    }
//
//                    // 超最大里程惩罚
//                    if (driver.maxMileage > 0) {
//                        if (tspResponse.length > driver.maxMileage) {
//                            driverFitness *= Math.pow(tspResponse.length / driver.maxMileage, 2);
//
//                            System.out.println("fitness maxMileage " + Math.pow(tspResponse.length / driver.maxMileage, 2));
//                        }
//                    }
//
//                    // 超工作时间惩罚
//                    if (driver.workEndTime > 0) {
//                        long workStartTime = driver.workStartTime > currentTime ? driver.workStartTime : currentTime;
//                        if (workStartTime + driverFitness > driver.workEndTime) {
//                            driverFitness += (workStartTime + driverFitness - driver.workEndTime) * 2;
//
//                            System.out.println("fitness workEndTime " + Math.pow(tspResponse.length / driver.maxMileage, 2));
//                        }
//                    }

                    result += driverFitness;
                }
            }

            Response response = new Response();
            response.driverAllocations = driverAllocations;
            response.driverList = driverList;
            chromosome.response = response;

//            System.out.println("fitness " + result + " " + Thread.currentThread().getName());

            return result;
        }, gtf)
                .populationSize(80)
                .optimize(Optimize.MINIMUM)
                .alterers(
//                    new SinglePointCrossover<>(0.2),
                    new UniformCrossover<>(0.5),
                    new Mutator<>(0.15)
                )
                // 默认的选择器 选出的结果中会包含重复的 但效果不错
//                 .selector(new TournamentSelector<>())
                // 排序之后选择前面的 在实际中不常用
//                .selector(new TruncationSelector<>())
//                .selector(new RouletteWheelSelector<>())
//                .selector(new LinearRankSelector<>())
//                .selector(new StochasticUniversalSelector<>())
//                .selector(new ExponentialRankSelector<>())
//                .selector(new BoltzmannSelector<>())
//                .selector(new EliteSelector<>())
//                .selector(new MonteCarloSelector<>())

//                .survivorsSelector(new TruncationSelector<>())
//                .offspringSelector(new TruncationSelector<>())

                // 一个司机分配到超过 30 单的概率比较低 所以一般不需要 phenotypeValidator
//                .phenotypeValidator(new PhenotypeValidator(20))
//                .maximalPhenotypeAge(20)
//                .offspringFraction(0.8)
//                .executor(Runnable::run)

                //// 完全随机
//                .alterers(new Mutator<>(0.5))
//                .offspringFraction(1)
//                .selector(new TruncationSelector<>())
                .build();

        EvolutionStatistics<Double, DoubleMomentStatistics> statistics =
                EvolutionStatistics.ofNumber();

        EvolutionResultStatistics<Double> evolutionResultStatistics = new EvolutionResultStatistics<>();

        Genotype<IntegerGene> resultGenotype = engine.stream()
                .limit(400)
                .peek(statistics)
                .peek(evolutionResultStatistics)
                .collect(EvolutionResult.toBestGenotype());

//        EvolutionResult result = engine.stream().limit(50).collect(EvolutionResult.toBestEvolutionResult());
//        Genotype resultGenotype = result.getBestPhenotype().getGenotype();
        Response response = resultGenotype.getChromosome().as(UDSChromosome.class).response;
        response.driverList = driverList;
        response.shipmentList = shipmentList;
        response.evolutionResults = evolutionResultStatistics.getSimpleEvolutionResults();

        System.out.println("result: " + resultGenotype);
        System.out.println("statistics: " + statistics);

//        System.out.println(Arrays.deepToString(((DistanceImpl) distance).used));

        return response;
    }


    public Response run1(List<Driver> driverList, List<Shipment> shipmentList, int k) {
        return null;
    }
}
