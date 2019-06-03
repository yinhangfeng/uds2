package com.mrwind.uds;

import com.mrwind.uds.stat.EvolutionResultStatistics;
import com.mrwind.uds.tsp.AntColonyTSP;
import com.mrwind.uds.tsp.TSPResponse;
import com.mrwind.uds.util.KGrayCode;
import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class UDS {
    private List<Driver> driverList;
    private List<Shipment> shipmentList;
    private Distance distance;
    private long currentTime;

    public static long minTspTime = Long.MAX_VALUE;
    public static long maxTspTime = Long.MIN_VALUE;
    public static long totalTspTime = 0;
    public static long tspRunTimes = 0;
    public static long tspRunShipmentCount = 0;

    public UDS(List<Driver> driverList, List<Shipment> shipmentList) {
        this.driverList = driverList;
        this.shipmentList = shipmentList;

        int driverCount = driverList.size();
        int shipmentCount = shipmentList.size();

        List<Shipment> allShipmentList = new ArrayList<>(shipmentList);
        for (Driver driver : driverList) {
            if (driver.getAllocatedShipments() != null) {
                allShipmentList.addAll(driver.getAllocatedShipments());
            }
        }
        distance = new DistanceImpl(driverList, allShipmentList, true);

        // TODO
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            currentTime = simpleDateFormat.parse("2019-05-27 08:00").getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public Response run(EvolutionInit<IntegerGene> evolutionInit) {
        int driverCount = driverList.size();
        int shipmentCount = shipmentList.size();

        Factory<Genotype<IntegerGene>> gtf = Genotype.of(UDSChromosome.of(0, driverCount - 1, shipmentCount));

        Engine<IntegerGene, Double> engine = Engine.builder(gt -> {
            UDSChromosome chromosome = gt.getChromosome().as(UDSChromosome.class);

            if (chromosome.response != null) {
                // fitness 已经算好
                // 1. 通过 EvolutionInit 提供的初始种群
                // 2. 通过 Mutator 但未改变的 Genotype 由于 Phenotype 重新创建 导致 evaluated 标记被重置

                return chromosome.response.getFitness();
            }

            int length = chromosome.length();
            List<Response.DriverAllocation> driverAllocations = new ArrayList<>(driverCount);
            for (int i = 0; i < driverCount; ++i) {
                driverAllocations.add(new Response.DriverAllocation());
            }
            // 将当前染色体的运单分配状况放入 每个司机的运单列表
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
                // 添加当前已分配不能重分的运单
                List<Shipment> allocatedShipments = driver.getAllocatedShipments();
                if (allocatedShipments != null) {
                    driverShipments.addAll(allocatedShipments);
                }

                if (!driverShipments.isEmpty()) {
                    TSPResponse tspResponse;
                    if (allocatedShipments != null && allocatedShipments.size() == driverShipments.size() && driver.allocatedTSPResponseCache != null) {
                        // 司机身上只有原来已分配的运单
                        tspResponse = driver.allocatedTSPResponseCache;
                    } else {
                        antColonyTSP = AntColonyTSP.obtain(driver, driverShipments)
                                .distance(distance)
//                                .selector(new DriverTimeSelector(driver, currentTime))
                                .driverAndShipments(driver, driverShipments);
                        tspResponse = antColonyTSP.run();
                        antColonyTSP.recycle();

                        if (allocatedShipments != null && allocatedShipments.size() == driverShipments.size()) {
                            // 缓存司机身上原来已分配运单的计算结果
                            driver.allocatedTSPResponseCache = tspResponse;
                        }
                    }

                    driverAllocation.response = tspResponse;

                    double driverFitness = tspResponse.length;
//                    double driverFitness = tspResponse.tspResponse;

//                    // 超最大单量惩罚
//                    if (driver.maxLoad > 0) {
//                        if (driverShipments.size() > driver.maxLoad) {
//                            driverFitness *= driverShipments.size() / ((double) driver.maxLoad);
//
//                            System.out.println("tspResponse maxLoad " + driverShipments.size() / ((double) driver.maxLoad));
//                        }
//                    }
//
//                    // 超最大里程惩罚
//                    if (driver.maxMileage > 0) {
//                        if (tspResponse.length > driver.maxMileage) {
//                            driverFitness *= Math.pow(tspResponse.length / driver.maxMileage, 2);
//
//                            System.out.println("tspResponse maxMileage " + Math.pow(tspResponse.length / driver.maxMileage, 2));
//                        }
//                    }
//
//                    // 超工作时间惩罚
//                    if (driver.workEndTime > 0) {
//                        long workStartTime = driver.workStartTime > currentTime ? driver.workStartTime : currentTime;
//                        if (workStartTime + driverFitness > driver.workEndTime) {
//                            driverFitness += (workStartTime + driverFitness - driver.workEndTime) * 2;
//
//                            System.out.println("tspResponse workEndTime " + Math.pow(tspResponse.length / driver.maxMileage, 2));
//                        }
//                    }

                    result += driverFitness;
                }
            }

            Response response = new Response();
            response.driverAllocations = driverAllocations;
            response.driverList = driverList;
            chromosome.response = response;

//            System.out.println("tspResponse " + result + " " + Thread.currentThread().getName());

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

        EvolutionStream<IntegerGene, Double> stream;
        if (evolutionInit != null) {
            stream = engine.stream(evolutionInit);
        } else {
            stream = engine.stream();
        }

        Genotype<IntegerGene> resultGenotype = stream
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

    public Response run() {
        return run(null);
    }


    public Response run1(int batchSize) {
        if (batchSize > 32) {
            throw new IllegalArgumentException("batchSize must <= 32");
        }

        int shipmentCount = shipmentList.size();
//        int batchCount = (int) Math.ceil(shipmentCount / (double) batchSize);
        int driverCount = driverList.size();
        // 当前 batch 大小 最后一个 batch 可能小于 batchSize
        int currentBatchSize = Math.min(batchSize, shipmentCount);
        List<DriverBatchAllocation> driverBatchAllocations = new ArrayList<>(driverCount);
        // 当 currentBatchSize == 1 或 driverCount <= 2 时 对每个司机来说 每一次迭代分配情况都不同 所以就不需要缓存了(空分配是单独存储的)
        boolean driverBatchAllocationCache = currentBatchSize > 1 && driverCount > 2;
        for (Driver driver : driverList) {
            driverBatchAllocations.add(new DriverBatchAllocation(driver, currentBatchSize, driverBatchAllocationCache));
        }
        // 通过类似格雷码的方式迭代一个 batch 的所有分配方案
        KGrayCode kGrayCode = new KGrayCode(currentBatchSize, driverCount);

        // TODO
        AntColonyTSP antColonyTSP = AntColonyTSP.obtain(128).distance(distance);

        DriverBatchAllocation oldDriverBatchAllocation;
        DriverBatchAllocation newDriverBatchAllocation;
        // 一个 batch 的最佳分配的 tspResponse (最后一个 batch 结束之后就是 最佳分配的 tspResponse)
        double bestFitness;
        double currentFitness = 0;
        int batchStartIndex = 0;
        int[] allocation = new int[shipmentCount];

        for (; ; ) {
            bestFitness = Double.MAX_VALUE;
            // 初始本批次所有单都分给第一个司机
            if (currentBatchSize > 1) {
                driverBatchAllocations.get(0).allocation.set(1, currentBatchSize, true);
            }

            for (KGrayCode.Element change : kGrayCode) {
                if (change.oldValue >= 0) {
                    oldDriverBatchAllocation = driverBatchAllocations.get(change.oldValue);
                    assert oldDriverBatchAllocation.allocation.get(change.index);
                    oldDriverBatchAllocation.allocation.set(change.index, false);
                    currentFitness += oldDriverBatchAllocation.calcFitnessDiff(shipmentList, batchStartIndex, currentBatchSize, antColonyTSP);
                }

                newDriverBatchAllocation = driverBatchAllocations.get(change.value);
                assert !newDriverBatchAllocation.allocation.get(change.index);
                newDriverBatchAllocation.allocation.set(change.index, true);
                currentFitness += newDriverBatchAllocation.calcFitnessDiff(shipmentList, batchStartIndex, currentBatchSize, antColonyTSP);

//                System.out.println("batchStartIndex: " + batchStartIndex + " currentBatchSize: " + currentBatchSize);
//                System.out.println("allocation: " + change);
//                System.out.println(driverBatchAllocations);
//                System.out.println();

                if (currentFitness < bestFitness) {
                    bestFitness = currentFitness;

                    double bestFitnessCheck = 0;
                    // 保存最佳的分配
                    for (DriverBatchAllocation driverBatchAllocation : driverBatchAllocations) {
                        driverBatchAllocation.saveBestAllocation();

                        if (driverBatchAllocation.bestTspResponse != null) {
                            // TODO
                            bestFitnessCheck += driverBatchAllocation.bestTspResponse.length;
                        }
                    }

                    assert Math.abs(bestFitnessCheck - bestFitness) < 0.0001;

                    for (int i = 0; i < currentBatchSize; ++i) {
                        allocation[i + batchStartIndex] = change.nm[i];
                    }
                }
            }

            // DriverBatchAllocation 为下一个 batch 做准备
            for (DriverBatchAllocation driverBatchAllocation : driverBatchAllocations) {
                driverBatchAllocation.prepareNextBatch();
            }

            // 下一个 batch 的 tspResponse 初值为当前的 bestTspResponse
            currentFitness = bestFitness;

            batchStartIndex += batchSize;

            if (batchStartIndex >= shipmentCount) {
                break;
            }
            if (batchStartIndex >= shipmentCount - batchSize) {
                // 下一个 batch 为最后一个 可能长度不足 batchSize
                currentBatchSize = shipmentCount - batchStartIndex;
            }

            // 重置分配计数
            kGrayCode.reset(currentBatchSize);
        }

        List<Response.DriverAllocation> driverAllocations = new ArrayList<>(driverBatchAllocations.size());

        for (DriverBatchAllocation driverBatchAllocation : driverBatchAllocations) {
            Response.DriverAllocation driverAllocation = new Response.DriverAllocation();
            driverAllocation.shipmentList = driverBatchAllocation.allocationShipments;
            // 在迭代过程中将最佳的 tspResponse 分配情况的路线保存下来的代价太大 所以最后再重新算一次
//            antColonyTSP.driverAndShipments(driverBatchAllocation.driver, driverBatchAllocation.allocationShipments).maxIterations(-1).run();
            driverAllocation.response = driverBatchAllocation.tspResponse;
            driverAllocations.add(driverAllocation);
        }

        antColonyTSP.recycle();

        Response response = new Response();
        response.driverList = driverList;
        response.shipmentList = shipmentList;
        response.driverAllocations = driverAllocations;
        response.allocation = allocation;

        System.out.println("tspRunTimes: " + UDS.tspRunTimes + " minTspTime: " + UDS.minTspTime + " maxTspTime: " + UDS.maxTspTime + " totalTspTime: " + UDS.totalTspTime + " avgTspTime: " + (UDS.totalTspTime / (double) UDS.tspRunTimes) + " tspRunShipmentCount: " + tspRunShipmentCount);

        return response;
    }

    static class DriverBatchAllocation {

        public Driver driver;
        // 当前批次分配 tspResponse 的缓存 其中 key 为 0 的表示当前批次未分配 也就 batchAllocationStartIndex 之前的分配
        public HashMap<Long, TSPResponse> batchCache;
        // 当前批次的运单分配情况 每一 bit 代表该单是否分配给了当前司机
        public BitSet allocation;
        // 当前已分配的运单
        // 最开始一段为 driver.allocatedShipments 之后 batchAllocationStartIndex 之前为 已经分配的
        // 再之后为当前 batch 分配的
        public List<Shipment> allocationShipments;
        // 当前 batch 分配在 allocationShipments 中的开始 index
        public int batchAllocationStartIndex;
        // 当前分配的 tspResponse
        public TSPResponse tspResponse;
        // 当前分配为空的 TSPResponse (也就是 batchAllocationStartIndex 之前的分配的 TSPResponse)
        public TSPResponse batchEmptyTspResponse;

        // 最佳分配的运单列表 只存储当前批次的单
        public List<Shipment> bestBatchAllocationShipments;
        // 最佳分配的 tspResponse
        public TSPResponse bestTspResponse;

        int hitCount;
        int missCount;


        public DriverBatchAllocation(Driver driver, int batchSize, boolean cache) {
            this.driver = driver;
            allocation = new BitSet(batchSize);
            if (cache) {
                batchCache = new HashMap<>();
            }
            if (driver.allocatedShipments != null) {
                allocationShipments = new ArrayList<>(driver.allocatedShipments);
                batchAllocationStartIndex = allocationShipments.size();
            } else {
                allocationShipments = new ArrayList<>();
            }
            bestBatchAllocationShipments = new ArrayList<>();
        }

        /**
         * 计算新分配的 tspResponse 返回与上一个的差值
         * <p>
         * 如果是第一次则会返回当前分配的 tspResponse (因为之前为 0)
         *
         * @param allShipments
         * @param batchStartIndex
         * @param batchSize
         * @return
         */
        double calcFitnessDiff(List<Shipment> allShipments, int batchStartIndex, int batchSize, AntColonyTSP antColonyTSP) {

            TSPResponse tspResponse = null;
            long allocationCacheKey = -1;
            if (allocation.isEmpty()) {
                tspResponse = batchEmptyTspResponse;
            } else if (batchCache != null) {
                // driverBatchAllocation.allocation 的长度不可能超过 32 所以取第一个 long 就可以
                allocationCacheKey = (int) allocation.toLongArray()[0];
                tspResponse = batchCache.get(allocationCacheKey);
                if (tspResponse != null) {
                    hitCount++;
//                    System.out.println("calcFitnessDiff cache hit " + allocation + " " + allocationCacheKey + " hitCount: " + hitCount + " " + (hitCount / (float) (hitCount + missCount)) + " " + (hitCount + missCount) + " " + driver);
                } else {
                    missCount++;
//                    System.out.println("calcFitnessDiff cache miss " + allocationCacheKey + " missCount: " + missCount + " " + (hitCount / (float) (hitCount + missCount)) + " " + driver);
                }
            }

            // 清掉当前 batch 分配的
            while (allocationShipments.size() > batchAllocationStartIndex) {
                allocationShipments.remove(allocationShipments.size() - 1);
            }
            // 添加新的分配
            for (int i = 0; i < batchSize; ++i) {
                if (allocation.get(i)) {
                    allocationShipments.add(allShipments.get(batchStartIndex + i));
                }
            }

            // 计算当前分配的 tspResponse
            if (tspResponse == null) {
                if (allocationShipments.isEmpty()) {
                    tspResponse = TSPResponse.EMPTY_RESPONSE;
                } else {
                    // TODO 为了测试暂时限制一下
                    if (allocationShipments.size() > 15) {
//                        System.out.println("xxxx " + allocationShipments.size());
                        tspResponse = new TSPResponse();
                        tspResponse.length = 999999999;
                    } else {
                        // TODO
                        long start = System.currentTimeMillis();
                        tspResponse = antColonyTSP.driverAndShipments(driver, allocationShipments).maxIterations(-1).run();
                        long time = System.currentTimeMillis() - start;

                        if (time < minTspTime) {
                            minTspTime = time;
                        } else if (time > maxTspTime) {
                            maxTspTime = time;
                        }
                        tspRunTimes++;
                        totalTspTime += time;
                        tspRunShipmentCount += allocationShipments.size();
                    }
                }

                if (allocation.isEmpty()) {
                    batchEmptyTspResponse = tspResponse;
                } else if (batchCache != null) {
                    // TODO batchSize 比较大时 缓存代价太大 可以考虑去掉一些缓存数据
//                    tspResponse.tour = null;
//                    tspResponse.originalPoints = null;
                    batchCache.put(allocationCacheKey, tspResponse);
                }
            }

            TSPResponse oldTspResponse = this.tspResponse;
            this.tspResponse = tspResponse;
            // TODO
            return tspResponse.length - (oldTspResponse == null ? 0 : oldTspResponse.length);
        }

        /**
         * 将当前分配保存为最佳分配
         */
        public void saveBestAllocation() {
            bestBatchAllocationShipments.clear();
            for (int i = batchAllocationStartIndex; i < allocationShipments.size(); ++i) {
                bestBatchAllocationShipments.add(allocationShipments.get(i));
            }

            bestTspResponse = tspResponse;
        }

        /**
         * 为开始下一个 batch 做准备
         */
        public void prepareNextBatch() {
            // 将最佳分配作为之后 batch 的初始分配(也就是 batchAllocationStartIndex 后移)
            while (allocationShipments.size() > batchAllocationStartIndex) {
                allocationShipments.remove(allocationShipments.size() - 1);
            }
            allocationShipments.addAll(bestBatchAllocationShipments);
            batchAllocationStartIndex = allocationShipments.size();
            bestBatchAllocationShipments.clear();

            tspResponse = bestTspResponse;

            allocation.clear();

            if (batchCache != null) {
                batchCache.clear();
            }
            batchEmptyTspResponse = bestTspResponse;
        }

        @Override
        public String toString() {
            return "DriverBatchAllocation{" +
                    "allocation=" + allocation +
                    ", allocationShipments=" + allocationShipments +
                    ", batchAllocationStartIndex=" + batchAllocationStartIndex +
                    ", tspResponse=" + tspResponse +
                    ", bestBatchAllocationShipments=" + bestBatchAllocationShipments +
                    ", bestTspResponse=" + bestTspResponse +
                    ", batchCache=" + (batchCache == null ? batchCache : batchCache.size()) +
                    '}';
        }
    }


}
