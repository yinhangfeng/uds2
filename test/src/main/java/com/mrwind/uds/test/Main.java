package com.mrwind.uds.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mrwind.uds.*;
import com.mrwind.uds.util.CoordinateUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static int pointId = 0;
    static int driverId = 0;
    static int shipmentId = 0;

    static Point getRandomPoint(String id) {
        float minLng = 120.07f;
        float maxLng = 120.4f;
        float minLat = 30.12f;
        float maxLat = 30.4f;
        Point point = new Point();
        String _id = String.valueOf(pointId++);
        point.id = id == null ? _id : id + "_" + _id;

        if (Math.random() > 0.5) {
            point.lng = (float) ((minLng + maxLng) / 2 + (1 - Math.random()) * (maxLng - minLng) * Math.random() * 0.4);
            point.lat = (float) ((minLat + maxLat) / 2 + (1 - Math.random()) * (maxLat - minLat) * Math.random() * 0.4);
        } else {
            point.lng = (float) (minLng + Math.random() * (maxLng - minLng));
            point.lat = (float) (minLat + Math.random() * (maxLat - minLat));
        }
        CoordinateUtils.transformPoint(point);
        return point;
    }

    static Point getRandomPoint() {
        return getRandomPoint(null);
    }

    static Driver getRandomDriver() {
        Driver driver = new Driver();
        String id = String.valueOf(driverId++);
        driver.setId(id);
        driver.setPos(getRandomPoint("ds_" + id));
        return driver;
    }

    static Shipment getRandomShipment() {
        Shipment shipment = new Shipment();
        String id = String.valueOf(shipmentId++);
        shipment.setId(id);
        shipment.setSender(getRandomPoint("ss_" + id));
        shipment.setReceiver(getRandomPoint("sr_" + id));
        shipment.setWeight((int) Math.round(Math.random() * 10));

        return shipment;
    }

    static List<Shipment> getRandomShipments(int count) {
        List<Shipment> shipments = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            shipments.add(getRandomShipment());
        }
        return shipments;
    }

    static List<Driver> getRandomDrivers(int count) {
        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            drivers.add(getRandomDriver());
        }
        return drivers;
    }

    static void output(Response response) {
        output(response, "uds.json");
    }

    static void output(Response response, String fileName) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("driverList", response.driverList);
        jsonObject.put("shipmentList", response.shipmentList);
        jsonObject.put("driverAllocations", response.driverAllocations);

        File file = new File("../outputs/" + fileName);
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(jsonObject, SerializerFeature.DisableCircularReferenceDetect), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void test1() {
        List<Driver> drivers = getRandomDrivers(3);
        List<Shipment> shipmentList = getRandomShipments(10);

        UDS uds = new UDS();

        Response response = uds.run(drivers, shipmentList);

        output(response);
    }

    public static void main(String[] args) throws Exception {

        test1();

//        TSPTest.main(args);
//        JeneticsTest.main(args);
    }
}
