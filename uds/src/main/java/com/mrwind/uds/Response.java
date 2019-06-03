package com.mrwind.uds;

import com.mrwind.uds.stat.SimpleEvolutionResult;
import com.mrwind.uds.tsp.TSPResponse;

import java.util.ArrayList;
import java.util.List;

public class Response {

    public static class DriverAllocation {

        public DriverAllocation() {
            shipments = new ArrayList<>();
        }

        /**
         * 司机分配的运单 包括司机身上原来已分配不可重分的部分
         */
        public List<Shipment> shipments;
        /**
         * 分配运单的路线 为空代表未分配运单
         */
        public TSPResponse response;

        @Override
        public String toString() {
            return "DriverAllocation{" +
                    "shipments=" + shipments +
                    '}';
        }
    }

    public Response() {

    }

    public List<Driver> drivers;
    public List<Shipment> shipments;
    // 每个司机的分配情况 与 drivers 对应
    public List<DriverAllocation> driverAllocations;
    // 遗传算法统计
    public List<SimpleEvolutionResult> evolutionResults;
    // 分配方案 需要代表运单编号与 shipments 对应 值代表司机 与 drivers 对应
    public int[] allocation;

    public double getFitness() {
        double fitness = 0;
        for (Response.DriverAllocation driverAllocation : driverAllocations) {
            if (driverAllocation.response != null) {
                // TODO
                fitness += driverAllocation.response.length;
            }
        }

        return fitness;
    }

    @Override
    public String toString() {
        return "Response{" +
                "driverAllocations=" + driverAllocations +
                '}';
    }
}
