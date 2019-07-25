package com.mrwind.uds;

import java.util.List;
import java.util.concurrent.Callable;

public class GreedyTask implements Callable<Response> {

    private UDS uds;
    private int batchSize;

    public GreedyTask(UDS uds, int batchSize, boolean shuffle) {
        this.uds = uds;
        this.batchSize = batchSize;
        this.shuffle = shuffle;
    }

    private boolean shuffle;

    @Override
    public Response call() throws Exception {
        List<Shipment> shipments;
        if (shuffle) {
            shipments = uds.getShuffleShipments();
        } else {
            shipments = uds.shipments;
        }
        return uds.runGreedy(batchSize, shipments);
    }
}
