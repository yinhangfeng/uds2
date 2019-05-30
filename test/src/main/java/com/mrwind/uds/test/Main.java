package com.mrwind.uds.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mrwind.uds.*;
import com.mrwind.uds.util.CoordinateUtils;
import com.mrwind.uds.util.KGrayCode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static int pointId = 0;
    static int driverId = 0;
    static int shipmentId = 0;

    static Point getRandomPoint(String id, boolean r, float minLng, float maxLng, float minLat, float maxLat) {
        Point point = new Point();
        String _id = String.valueOf(pointId++);
        point.id = id == null ? _id : id + "_" + _id;

        if (r && Math.random() > 0.5) {
//         非均匀分布
            point.lng = (float) ((minLng + maxLng) / 2 + (1 - Math.random()) * (maxLng - minLng) * Math.random() * 0.4);
            point.lat = (float) ((minLat + maxLat) / 2 + (1 - Math.random()) * (maxLat - minLat) * Math.random() * 0.4);
        } else {
            point.lng = (float) (minLng + Math.random() * (maxLng - minLng));
            point.lat = (float) (minLat + Math.random() * (maxLat - minLat));
        }
        CoordinateUtils.transformPoint(point);
        return point;
    }

    static Point getRandomPoint(String id, boolean r) {
        return getRandomPoint(id, r, 120.07f, 120.4f, 30.12f, 30.4f);
    }

    static Point getRandomPoint(boolean r) {
        return getRandomPoint(null, r);
    }

    static Point getRandomPoint(String id) {
        return getRandomPoint(id, false);
    }

    static Point getRandomPoint() {
        return getRandomPoint(false);
    }

    static Driver getRandomDriver() {
        Driver driver = new Driver();
        String id = String.valueOf(driverId++);
        driver.setId(id);
        driver.setPos(getRandomPoint("ds_" + id));
        driver.setHome(getRandomPoint("dh_" + id));
        driver.setMaxLoad(12);
        driver.setMaxMileage(120_000);
        return driver;
    }

    static Shipment getRandomShipment() {
        Shipment shipment = new Shipment();
        String id = String.valueOf(shipmentId++);
        shipment.setId(id);
        shipment.setSender(getRandomPoint("ss_" + id));
        // 收件人 集中一点
        shipment.setReceiver(getRandomPoint("sr_" + id, true));
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

        JSONObject statistics = new JSONObject();
        statistics.put("evolutionResults", response.evolutionResults);
        jsonObject.put("statistics", statistics);

        File file = new File("../outputs/" + fileName);
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(jsonObject, SerializerFeature.DisableCircularReferenceDetect), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void outputInputData(List<Driver> driverList, List<Shipment> shipmentList, String fileName) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("driverList", driverList);
        jsonObject.put("shipmentList", shipmentList);

        File file = new File("../outputs/" + fileName);
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(jsonObject, SerializerFeature.DisableCircularReferenceDetect), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void outputInputData(List<Driver> driverList, List<Shipment> shipmentList) {
        outputInputData(driverList, shipmentList, "input.json");
    }

    static void outputRandomInputData(int driverCount, int shipmentCount) {
        List<Driver> drivers = getRandomDrivers(driverCount);
        List<Shipment> shipmentList = getRandomShipments(shipmentCount);

        outputInputData(drivers, shipmentList);
    }

    static Response getInputDataFromFile(String fileName) throws Exception {
        ArrayList<Point> points = new ArrayList<>();
        String testPointsStr = FileUtils.readFileToString(new File("../outputs/" + fileName), "utf8");
        JSONObject testJson = (JSONObject) JSONObject.parse(testPointsStr);

        Response response = new Response();

        Type driverListType = new TypeReference<List<Driver>>() {}.getType();
        response.driverList = testJson.getObject("driverList", driverListType);
        Type shipmentListType = new TypeReference<List<Shipment>>() {}.getType();
        response.shipmentList = testJson.getObject("shipmentList", shipmentListType);

        return response;
    }

    static Response getInputDataFromFile() throws Exception {
        return getInputDataFromFile("input.json");
    }

    static void test1() {
        List<Driver> drivers = getRandomDrivers(5);

        List<Shipment> shipmentList = getRandomShipments(50);

        UDS uds = new UDS(drivers, shipmentList);

        Response response = uds.run();

        output(response);
    }

    static void test2() {
        // 四个在地图角落的司机
        List<Driver> drivers = new ArrayList<>();
        Driver driver;
        driver = new Driver();
        driver.setPos(getRandomPoint(null, false, 120.07f, 120.1f, 30.12f, 30.15f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPos(getRandomPoint(null, false, 120.07f, 120.1f, 30.36f, 30.4f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPos(getRandomPoint(null, false, 120.35f, 120.4f, 30.12f, 30.15f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPos(getRandomPoint(null, false, 120.35f, 120.4f, 30.36f, 30.4f));
        drivers.add(driver);

        List<Shipment> shipmentList = getRandomShipments(15);

        UDS uds = new UDS(drivers, shipmentList);

        Response response = uds.run();

        output(response);
    }

    static void test3() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.driverList;
        List<Shipment> shipmentList = responseInput.shipmentList;

        UDS uds = new UDS(drivers, shipmentList);
        Response response = uds.run();
        output(response);
    }

    static void test4() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.driverList;
        List<Shipment> shipmentList = responseInput.shipmentList;

        UDS uds = new UDS(drivers, shipmentList);
        Response response = uds.run1( 20);
        output(response, "uds1.json");
    }

    static void testKGrayCode() {
        int n = 3;
        int m = 8;
        KGrayCode kGrayCode = new KGrayCode(n, m);

        for (com.mrwind.uds.util.KGrayCode.Element e : kGrayCode) {
            System.out.println(e + " " + kGrayCode.hasNext() + " " + (Integer.toBinaryString(e.nm.length > 1 ? e.nm[0] + e.nm[1] * m : e.nm[0])));
        }

        System.out.println("reset");

        n = 2;
        kGrayCode.reset(n);
        for (com.mrwind.uds.util.KGrayCode.Element e : kGrayCode) {
            System.out.println(e + " " + kGrayCode.hasNext() + " " + (Integer.toBinaryString(e.nm.length > 1 ? e.nm[0] + e.nm[1] * m : e.nm[0])));
        }
    }

    public static void main(String[] args) throws Exception {

        outputRandomInputData(2, 20);

//        test1();
//        test2();
//        test3();
        test4();

//        testKGrayCode();
//        TSPTest.main(args);
//        JeneticsTest.main(args);
    }
}
