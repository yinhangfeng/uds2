package com.mrwind.uds;

import com.mrwind.uds.tsp.AntColonyTSP;
import com.mrwind.uds.tsp.TSPResponse;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class DriverBatchAllocation {

    public Driver driver;
    // 当前批次分配 tspResponse 的缓存 其中 key 为 0 的表示当前批次未分配 也就 batchAllocationStartIndex 之前的分配
    private HashMap<Long, TSPResponse> allocationCache;
    // 当前批次的运单分配情况 每一 bit 代表当前 batch 该单是否分配给了当前司机
    public BitSet allocation;
    // 当前已分配的运单
    // 最开始一段为 driver.allocatedShipments 之后 batchAllocationStartIndex 之前为 已经分配的
    // 再之后为当前 batch 分配的
    public List<Shipment> allocationShipments;
    // 当前 batch 分配在 allocationShipments 中的开始 index
    private int batchAllocationStartIndex;
    // 当前分配的 tspResponse
    public TSPResponse tspResponse;
    // 当前分配为空的 TSPResponse (也就是 batchAllocationStartIndex 之前的分配的 TSPResponse)
    public TSPResponse emptyTspResponse;
    // 最佳分配的运单列表 只存储当前批次的单
    public List<Shipment> bestAllocationShipments;
    // 当前 batch 最佳分配的 tspResponse
    public TSPResponse bestTspResponse;

//    private int hitCount;
//    private int missCount;

    public DriverBatchAllocation(Driver driver, int batchSize, boolean cache) {
        this.driver = driver;
        allocation = new BitSet(batchSize);
        if (cache) {
            this.allocationCache = new HashMap<>();
        }
        if (driver.allocatedShipments != null) {
            allocationShipments = new ArrayList<>(driver.allocatedShipments);
            batchAllocationStartIndex = allocationShipments.size();
        } else {
            allocationShipments = new ArrayList<>();
        }
        bestAllocationShipments = new ArrayList<>();
    }

    /**
     * 计算新分配的 tspResponse 返回与上一个的差值
     * <p>
     * 如果是第一次则会返回当前分配的 tspResponse (因为之前为 0)
     *
     * @param allShipments
     * @param batchStartIndex
     * @param batchSize
     * @return 与上一个分配的差值
     */
    public double calcFitnessDiff(List<Shipment> allShipments, int batchStartIndex, int batchSize, AntColonyTSP antColonyTSP) {

        TSPResponse tspResponse = null;
        long allocationCacheKey = -1;
        if (allocation.isEmpty()) {
            tspResponse = emptyTspResponse;
        } else if (allocationCache != null) {
            // driverBatchAllocation.allocation 的长度不可能超过 32 所以取第一个 long 就可以
            allocationCacheKey = allocation.toLongArray()[0];
            tspResponse = allocationCache.get(allocationCacheKey);
//            if (tspResponse != null) {
//                hitCount++;
////                    System.out.println("calcFitnessDiff allocationCache hit " + allocation + " " + allocationCacheKey + " hitCount: " + hitCount + " " + (hitCount / (float) (hitCount + missCount)) + " " + (hitCount + missCount) + " " + driver);
//            } else {
//                missCount++;
////                    System.out.println("calcFitnessDiff allocationCache miss " + allocationCacheKey + " missCount: " + missCount + " " + (hitCount / (float) (hitCount + missCount)) + " " + driver);
//            }
        }

        // TODO 有缓存时可延迟到 saveBestAllocation 处理
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

                    if (time < UDS.minTspTime) {
                        UDS.minTspTime = time;
                    } else if (time > UDS.maxTspTime) {
                        UDS.maxTspTime = time;
                    }
                    UDS.tspRunTimes++;
                    UDS.totalTspTime += time;
                    UDS.tspRunShipmentCount += allocationShipments.size();
                }
            }

            if (allocation.isEmpty()) {
                emptyTspResponse = tspResponse;
            } else if (allocationCache != null) {
                // TODO batchSize 比较大时 缓存代价太大 可以考虑去掉一些缓存数据
//                    tspResponse.tour = null;
//                    tspResponse.originalPoints = null;
                allocationCache.put(allocationCacheKey, tspResponse);
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
        bestAllocationShipments.clear();
        for (int i = batchAllocationStartIndex; i < allocationShipments.size(); ++i) {
            bestAllocationShipments.add(allocationShipments.get(i));
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
        allocationShipments.addAll(bestAllocationShipments);
        batchAllocationStartIndex = allocationShipments.size();
        bestAllocationShipments.clear();

        tspResponse = bestTspResponse;

        allocation.clear();

        if (allocationCache != null) {
            allocationCache.clear();
        }
        emptyTspResponse = bestTspResponse;
        bestTspResponse = null;
    }

    @Override
    public String toString() {
        return "DriverBatchAllocation{" +
                "allocation=" + allocation +
                ", allocationShipments=" + allocationShipments +
                ", batchAllocationStartIndex=" + batchAllocationStartIndex +
                ", tspResponse=" + tspResponse +
                ", bestAllocationShipments=" + bestAllocationShipments +
                ", bestTspResponse=" + bestTspResponse +
                ", allocationCache=" + (allocationCache == null ? allocationCache : allocationCache.size()) +
                '}';
    }
}
