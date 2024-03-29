package com.mrwind.uds.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mrwind.uds.*;
import com.mrwind.uds.util.CoordinateUtils;
import com.mrwind.uds.util.KGrayCode;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionInit;
import io.jenetics.util.ISeq;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        driver.setPosition(getRandomPoint("ds_" + id));
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

        jsonObject.put("drivers", response.drivers);
        jsonObject.put("shipments", response.shipments);
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

    static void outputInputData(List<Driver> drivers, List<Shipment> shipments, String fileName) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("drivers", drivers);
        jsonObject.put("shipments", shipments);

        File file = new File("../outputs/" + fileName);
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(jsonObject, SerializerFeature.DisableCircularReferenceDetect), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void outputInputData(List<Driver> drivers, List<Shipment> shipments) {
        outputInputData(drivers, shipments, "input.json");
    }

    static void outputRandomInputData(int driverCount, int shipmentCount) {
        List<Driver> drivers = getRandomDrivers(driverCount);
        List<Shipment> shipments = getRandomShipments(shipmentCount);

        outputInputData(drivers, shipments);
    }

    static Response getInputDataFromFile(String fileName) throws Exception {
        ArrayList<Point> points = new ArrayList<>();
        String testPointsStr = FileUtils.readFileToString(new File("../outputs/" + fileName), "utf8");
        JSONObject testJson = (JSONObject) JSONObject.parse(testPointsStr);

        Response response = new Response();

        Type driverListType = new TypeReference<List<Driver>>() {}.getType();
        response.drivers = testJson.getObject("drivers", driverListType);
        Type shipmentListType = new TypeReference<List<Shipment>>() {}.getType();
        response.shipments = testJson.getObject("shipments", shipmentListType);

        return response;
    }

    static Response getInputDataFromFile() throws Exception {
        return getInputDataFromFile("input.json");
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

    static void test1() {
        List<Driver> drivers = getRandomDrivers(5);

        List<Shipment> shipments = getRandomShipments(50);

        UDS uds = new UDS.Builder(drivers, shipments).build();

        Response response = uds.run();

        output(response);
    }

    static void test2() {
        // 四个在地图角落的司机
        List<Driver> drivers = new ArrayList<>();
        Driver driver;
        driver = new Driver();
        driver.setPosition(getRandomPoint(null, false, 120.07f, 120.1f, 30.12f, 30.15f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPosition(getRandomPoint(null, false, 120.07f, 120.1f, 30.36f, 30.4f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPosition(getRandomPoint(null, false, 120.35f, 120.4f, 30.12f, 30.15f));
        drivers.add(driver);
        driver = new Driver();
        driver.setPosition(getRandomPoint(null, false, 120.35f, 120.4f, 30.36f, 30.4f));
        drivers.add(driver);

        List<Shipment> shipments = getRandomShipments(15);

        UDS uds = new UDS.Builder(drivers, shipments).build();

        Response response = uds.run();

        output(response);
    }

    static void test3() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.drivers;
        List<Shipment> shipments = responseInput.shipments;

        long start = System.currentTimeMillis();
        UDS uds = new UDS.Builder(drivers, shipments).build();
        Response response = uds.run();
        System.out.println("run time: " + (System.currentTimeMillis() - start));
        output(response);
    }

    static void test4() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.drivers;
        List<Shipment> shipments = responseInput.shipments;

        UDS uds = new UDS.Builder(drivers, shipments).build();
        Response response = null;
        long start = System.currentTimeMillis();
        response = uds.runGreedy(1);

        double fitness = 0;
        for (Response.DriverAllocation driverAllocation : response.driverAllocations) {
            if (driverAllocation.response != null) {
                fitness += driverAllocation.response.length;
            }
        }

        System.out.println("test4 fitness: " + fitness);
        System.out.println("run time: " + (System.currentTimeMillis() - start));


        System.out.println(response);
        System.out.println("minTspTime: " + UDS.minTspTime + " maxTspTime: " + UDS.maxTspTime + " totalTspTime: " + UDS.totalTspTime + " avgTspTime: " + (UDS.totalTspTime / (double) UDS.tspRunTimes));

        output(response, "uds1.json");
    }

    /**
     * batchSize
     * 当每个司机最大单量较少时 蚁群会使用穷举 所以结果是确定的 此时 batchSize 越大 最终结果会略微好一些
     * 但最大单量增加时 由于会执行蚁群算法 batchSize 大的执行蚁群时平均点数会增加(如果没有单量限制 算法结果比较倾向于分给一个人? 所以能证明 batchSize 越大效果越好么?)
     * 蚁群点数越多结果越不准确偏大越多 导致最终看到的数值结果反而是小 batchSize 会略微好一点?
     * 增加 MAX_EXHAUSTIVE_COUNT 之后能部分验证猜想
     */
    static void test5() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.drivers;
        List<Shipment> shipments = responseInput.shipments;

//        AntColonyTSP.MAX_EXHAUSTIVE_COUNT = 14;

        double[] fitnessTotals = new double[3];

        UDS uds = new UDS.Builder(drivers, shipments).build();
        Response response = null;
        for (int j = 0; j < 6; ++j) {
            System.out.println();
            System.out.println("test5 " + j);
            for (int i = 1; i <= 3; ++i) {
                long start = System.currentTimeMillis();
                response = uds.runGreedy(i);

                double fitness = 0;
                for (Response.DriverAllocation driverAllocation : response.driverAllocations) {
                    if (driverAllocation.response != null) {
                        fitness += driverAllocation.response.length;
                    }
                }

                System.out.println("test5 " + i + " tspResponse: " + fitness);
                System.out.println("run time: " + (System.currentTimeMillis() - start));

                fitnessTotals[i - 1] += fitness;
            }

            // XXX
            Collections.shuffle(shipments);
        }


        System.out.println("fitnessTotals " + Arrays.toString(fitnessTotals));
        System.out.println(response);
        System.out.println("minTspTime: " + UDS.minTspTime + " maxTspTime: " + UDS.maxTspTime + " totalTspTime: " + UDS.totalTspTime + " avgTspTime: " + (UDS.totalTspTime / (double) UDS.tspRunTimes));

        output(response, "uds1.json");
    }

    static void test6() throws Exception {
        Response responseInput = getInputDataFromFile();

        List<Driver> drivers = responseInput.drivers;
        List<Shipment> shipments = responseInput.shipments;

        List<Genotype<IntegerGene>> initGenotypes = new ArrayList<>();

        UDS uds = new UDS.Builder(drivers, shipments)
                .maxGreedyBatchSize(4)
                .greedyCount(1)
                .build();

        long start = System.currentTimeMillis();
        Response response = uds.run();

        double fitness = 0;
        for (Response.DriverAllocation driverAllocation : response.driverAllocations) {
            if (driverAllocation.response != null) {
                fitness += driverAllocation.response.length;
            }
        }

        System.out.println("test6 fitness: " + fitness);
        System.out.println("run time: " + (System.currentTimeMillis() - start));

        System.out.println(response);
        System.out.println("minTspTime: " + UDS.minTspTime + " maxTspTime: " + UDS.maxTspTime + " totalTspTime: " + UDS.totalTspTime + " avgTspTime: " + (UDS.totalTspTime / (double) UDS.tspRunTimes));


        output(response, "uds.json");
    }

    public static void main(String[] args) throws Exception {

//        outputRandomInputData(10, 100);

//        test1();
//        test2();
//        test3();
//        test4();
//        test5();
        test6();

//        testKGrayCode();
//        TSPTest.main(args);
//        JeneticsTest.main(args);
//        ForkJoinPoolTest.test1();
    }
}
