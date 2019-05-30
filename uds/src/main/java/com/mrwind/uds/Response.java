package com.mrwind.uds;

import com.mrwind.uds.stat.SimpleEvolutionResult;
import com.mrwind.uds.tsp.TSPResponse;

import java.util.ArrayList;
import java.util.List;

public class Response {

    public static class DriverAllocation {

        public DriverAllocation() {
            shipmentList = new ArrayList<>();
        }

        /**
         * 司机分配的运单 包括司机身上原来已分配不可重分的部分
         */
        public List<Shipment> shipmentList;
        public TSPResponse response;
    }

    public Response() {

    }

    public List<Driver> driverList;
    public List<Shipment> shipmentList;
    // 每个司机的分配情况 与 driverList 对应
    public List<DriverAllocation> driverAllocations;
    // 遗传算法统计
    public List<SimpleEvolutionResult> evolutionResults;

}
