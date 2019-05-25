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

        public List<Shipment> shipmentList;
        public TSPResponse response;
    }

    public Response() {

    }

    public List<Driver> driverList;
    public List<Shipment> shipmentList;
    public List<DriverAllocation> driverAllocations;
    public List<SimpleEvolutionResult> evolutionResults;

}
