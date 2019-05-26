package com.mrwind.uds;

import com.mrwind.uds.stat.EvolutionResultStatistics;
import com.mrwind.uds.tsp.AntColonyTSP;
import com.mrwind.uds.tsp.TSPResponse;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.Optimize;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;

import java.util.ArrayList;
import java.util.List;

public class UDS {

    public UDS() {

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
                        antColonyTSP = AntColonyTSP.obtain(AntColonyTSP.getDriverAndShipmentsPointCount(driver, driverShipments))
                                .distance(distance)
                                .driverAndShipments(driver, driverShipments);
                        tspResponse = antColonyTSP.run();
                        antColonyTSP.recycle();
                    }

                    driverAllocation.response = tspResponse;
                    result += tspResponse.length;
                }
            }

            Response response = new Response();
            response.driverAllocations = driverAllocations;
            chromosome.response = response;

//            System.out.println("fitness " + result + " " + Thread.currentThread().getName());

            return result;
        }, gtf)
                .populationSize(80)
                .optimize(Optimize.MINIMUM)
                // 一个司机分配到超过 30 单的概率比较低 所以一般不需要 phenotypeValidator
//                .phenotypeValidator(new PhenotypeValidator(30))
//                .maximalPhenotypeAge(20)
//                .offspringFraction(0.9)
//                .executor(Runnable::run)
                .build();

        EvolutionStatistics<Double, DoubleMomentStatistics> statistics =
                EvolutionStatistics.ofNumber();

        EvolutionResultStatistics<Double> evolutionResultStatistics = new EvolutionResultStatistics<>();

        Genotype<IntegerGene> resultGenotype = engine.stream()
                .limit(50)
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
}
