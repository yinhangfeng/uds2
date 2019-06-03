package com.mrwind.uds.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mrwind.uds.*;
import com.mrwind.uds.tsp.AntColonyTSP;
import com.mrwind.uds.tsp.DriverTimeSelector;
import com.mrwind.uds.tsp.TSPResponse;
import com.mrwind.uds.util.CoordinateUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TSPTest {

    static List<Point> getRandomPoints(int count) {
        return getRandomPoints(count, -1);
    }

    static List<Point> getRandomPoints(int count, int dependency) {
        ArrayList<Point> points = new ArrayList<>();

        float minLng = 120.07f;
        float maxLng = 120.4f;
        float minLat = 30.12f;
        float maxLat = 30.4f;
        for (int i = 0; i < count; ++i) {
            Point point = new Point();

            if (Math.random() > 0.5) {
                point.lng = (float) ((minLng + maxLng) / 2 + (1 - Math.random()) * (maxLng - minLng) * Math.random() * 0.4);
                point.lat = (float) ((minLat + maxLat) / 2 + (1 - Math.random()) * (maxLat - minLat) * Math.random() * 0.4);
            } else {
                point.lng = (float) (minLng + Math.random() * (maxLng - minLng));
                point.lat = (float) (minLat + Math.random() * (maxLat - minLat));
            }
            point.dependency = dependency;

            points.add(point);
        }
        CoordinateUtils.transformPoints(points, CoordinateUtils.COORDINATE_WGS84);
        return points;
    }

    static List<Point> getPointsFromFile(String path) throws Exception {
        ArrayList<Point> points = new ArrayList<>();
        String testPointsStr = FileUtils.readFileToString(new File(path), "utf8");
        JSONObject testJson = (JSONObject) JSONObject.parse(testPointsStr);
        JSONArray arr = testJson.getJSONArray("tour");
        for (int i = 0; i < arr.size(); ++i) {
            JSONObject jsonObject = arr.getJSONObject(i);
            int dependency = jsonObject.getIntValue("dependency");
            String id = jsonObject.getString("id");
            Point point = new Point(jsonObject.getFloat("lng"), jsonObject.getFloat("lat"));
            point.dependency = dependency;
            point.id = id;
            points.add(point);
        }
        CoordinateUtils.transformPoints(points, CoordinateUtils.COORDINATE_WGS84);
        return points;
    }

    static List<List<Point>> getAllPointsFromFile(String path) throws Exception {
        ArrayList<List<Point>> allPoints = new ArrayList<>();
        String testPointsStr = FileUtils.readFileToString(new File(path), "utf8");
        JSONArray testJsonArr = (JSONArray) JSONArray.parse(testPointsStr);

        for (int j = 0; j < testJsonArr.size(); ++j) {
            JSONArray arr = testJsonArr.getJSONArray(j);

            ArrayList<Point> points = new ArrayList<>();
            for (int i = 0; i < arr.size(); ++i) {
                JSONObject jsonObject = arr.getJSONObject(i);
                int dependency = jsonObject.getIntValue("dependency");
                String id = jsonObject.getString("id");
                Point point = new Point(jsonObject.getFloat("lng"), jsonObject.getFloat("lat"));
                point.dependency = dependency;
                point.id = id;
                points.add(point);
            }
            CoordinateUtils.transformPoints(points, CoordinateUtils.COORDINATE_WGS84);
            allPoints.add(points);
        }

        return allPoints;
    }

    static void output(TSPResponse response, List<Point> points) {
        output(response, points, "tsp.json");
    }

    static void output(TSPResponse response, List<Point> points, String fileName) {
        JSONObject jsonObject = new JSONObject();

        List<Point> tour = new ArrayList<>(points.size());
        JSONArray tourArray = new JSONArray(points.size());
        Point point;
        Point dependency;
        for (int i = 0; i < response.tour.length; ++i) {
            point = points.get(response.tour[i]);
            tour.add(point);
        }
        for (int i = 0; i < tour.size(); ++i) {
            point = tour.get(i);

            // 由于 tour 的顺序与原来不同所以需要修改 dependency
            if (point.dependency >= 0) {
                point = point.clone();
                dependency = points.get(point.dependency);
                for (int j = 0; j < tour.size(); ++j) {
                    if (dependency == tour.get(j)) {
                        point.dependency = j;
                        break;
                    }
                }
            }

            tourArray.add(point);
        }
        jsonObject.put("tour", tourArray);
        jsonObject.put("length", response.length);

        File file = new File("../outputs/" + fileName);
        try {
            FileUtils.writeStringToFile(file, jsonObject.toJSONString(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static TSPResponse run(AntColonyTSP antColonyTSP, List<Point> points, int startPointIndex, int endPointIndex, int maxIterations) {
        long start = System.currentTimeMillis();
        TSPResponse response = antColonyTSP.startPointIndex(startPointIndex).endPointIndex(endPointIndex).maxIterations(maxIterations).points(points).distance(new DistanceImpl(points, true)).run();
        System.out.println("run time: " + (System.currentTimeMillis() - start));
        System.out.println("run res: " + response);

        return response;
    }

    static TSPResponse runAco(AntColonyTSP antColonyTSP, List<Point> points, int startPointIndex, int endPointIndex, int maxIterations) {
        long start = System.currentTimeMillis();
        TSPResponse response = antColonyTSP.startPointIndex(startPointIndex).endPointIndex(endPointIndex).points(points).distance(new DistanceImpl(points, true)).runACO(maxIterations);
        System.out.println("runAco time: " + (System.currentTimeMillis() - start));
        System.out.println("runAco res: " + response);

        return response;
    }

    static TSPResponse runExhaustive(AntColonyTSP antColonyTSP, List<Point> points, int startPointIndex, int endPointIndex) {
        long start = System.currentTimeMillis();
        TSPResponse response = antColonyTSP.startPointIndex(startPointIndex).endPointIndex(endPointIndex).points(points).distance(new DistanceImpl(points, true)).runExhaustive();
        System.out.println("runExhaustive time: " + (System.currentTimeMillis() - start));
        System.out.println("runExhaustive res: " + response);

        return response;
    }

    static void testObtain() {
        AntColonyTSP inst1 = AntColonyTSP.obtain(10);
        AntColonyTSP inst2 = AntColonyTSP.obtain(12);
        AntColonyTSP inst3 = AntColonyTSP.obtain(66);
        AntColonyTSP inst4 = AntColonyTSP.obtain(44);
        AntColonyTSP inst5 = AntColonyTSP.obtain(55);

        inst1.recycle();
        inst2.recycle();
        inst3.recycle();
        inst4.recycle();
        inst5.recycle();

        inst1 = AntColonyTSP.obtain(10);
        inst2 = AntColonyTSP.obtain(12);
        inst3 = AntColonyTSP.obtain(66);
        inst4 = AntColonyTSP.obtain(44);
        inst5 = AntColonyTSP.obtain(55);

        inst3.recycle();
        inst4.recycle();
        inst5.recycle();

        inst1 = AntColonyTSP.obtain(10);
        inst2 = AntColonyTSP.obtain(12);
        inst3 = AntColonyTSP.obtain(66);
        inst4 = AntColonyTSP.obtain(44);
        inst5 = AntColonyTSP.obtain(55);
    }

    // 初始信息素对结果的影响
    static void test1() {
        double sumRatio = 0;
        float res1SumIt = 0;
        float res2SumIt = 0;

        int testCount1 = 50;
        int testCount2 = 20;
        int testCount = testCount1 * testCount2;
        int maxIterations = 20;
        int pointCount = 30;

        double bestLength = Integer.MAX_VALUE;
        TSPResponse bestResponse = null;

        AntColonyTSP antColony = AntColonyTSP.obtain(pointCount);
        AntColonyTSP antColonyTSP1 = antColony;
        AntColonyTSP antColonyTSP2 = antColony;
        for (int i = 0; i < testCount1; ++i) {
            List<Point> points = getRandomPoints(pointCount);

            for (int j = 0; j < testCount2; ++j) {
//                antColonyTSP1 = AntColonyTSP.obtain(pointCount);
                antColonyTSP1.initialPheromone(1);
                TSPResponse response1 = runAco(antColonyTSP1, points, 0, -1, maxIterations);
                res1SumIt += response1.iterationNum;
                if (response1.length < bestLength) {
                    bestResponse = response1;
                }

//                antColonyTSP2 = AntColonyTSP.obtain(pointCount);
                antColonyTSP2.initialPheromone(1 / response1.length);
                TSPResponse response2 = runAco(antColonyTSP2, points, 0, -1, maxIterations);
                res2SumIt += response2.iterationNum;
                if (response2.length < bestLength) {
                    bestResponse = response2;
                }

                sumRatio += response1.length / response2.length;

                antColonyTSP1.recycle();
                antColonyTSP2.recycle();
            }
        }

        System.out.println("res1SumIt: " + (res1SumIt / testCount) + " res2SumIt: " + (res2SumIt / testCount) + " " + (sumRatio / testCount)
                + " bestResponse: " + bestResponse);

    }

    static void test2() {
        int maxIterations = 20;
        int pointCount = 10;
        List<Point> points = getRandomPoints(pointCount);
        AntColonyTSP antColonyTSP = new AntColonyTSP(pointCount);
        TSPResponse res = run(antColonyTSP, points, 0, -1, maxIterations);

        output(res, points);
    }

    // 大量点测试
    static void test3() {
        int pointCount = 200;
        int maxIterations = Math.min(Math.max(pointCount, 20), 100);
        List<Point> points = getRandomPoints(pointCount);
        points.set(1, points.get(0).clone());
        points.set(4, points.get(0).clone());
        points.set(3, points.get(2).clone());
        AntColonyTSP antColonyTSP = new AntColonyTSP(pointCount);
        TSPResponse res = runAco(antColonyTSP, points, 0, -1, maxIterations);
        output(res, points);
    }

    // 穷举与蚁群的比较
    static void test4() throws Exception {
        // 穷举 12 个点就要 30s
        int pointCount = 14;
        int startPointIndex = 0;
        int endPointIndex = 1;
        int maxIterations = Math.min(Math.max(pointCount, 20), 80);
//        List<Point> points = getRandomPoints(pointCount);

        List<Point> points = getPointsFromFile("../outputs/tspInput.json");
//        points.set(1, points.get(0).clone());
//        points.set(4, points.get(0).clone());
//        points.set(3, points.get(2).clone());

        // 依赖
//        points.addAll(getRandomPoints(1, 1));
//        pointCount++;

        AntColonyTSP antColonyTSP = AntColonyTSP.obtain(pointCount);

        TSPResponse res1 = runExhaustive(antColonyTSP, points, startPointIndex, endPointIndex);

        output(res1, points);

        TSPResponse res2 = runAco(antColonyTSP, points, startPointIndex, endPointIndex, maxIterations);

        antColonyTSP.recycle();

        output(res2, points, "tsp1.json");

        System.out.println("res2.length / res1.length: " + (res2.length / res1.length));
    }

    // 贪心优化
//    static void test5() {
//        int maxIterations = 30;
//        int pointCount = 30;
//        List<Point> points = getRandomPoints(pointCount);
//        AntColonyTSP antColonyTSP = new AntColonyTSP(pointCount);
//        TSPResponse res = run(antColonyTSP, points, 0, -1, maxIterations);
//
//        output(res);
//
//        double newLength = Greedy.greedyTour(res.tour, res.length, pointCount, antColonyTSP.distance);
//        res.length = newLength;
//        System.out.println("test5 greedyTour " + res);
//
//        output(res, "tsp_greedy.json");
//    }

    // 取派
    static void test() throws Exception {
        int maxIterations = 50;
        List<Point> points = getRandomPoints(5);
        points.addAll(getRandomPoints(5, 1));
        points.addAll(getRandomPoints(5, 2));
        points.addAll(getRandomPoints(5, 3));
        points.addAll(getRandomPoints(5, 4));

//        List<Point> points = getPointsFromFile("../outputs/testTsp1.json");

        int pointCount = points.size();
        AntColonyTSP antColonyTSP = AntColonyTSP.obtain(pointCount);
//        antColonyTSP.rightAngleDistance(false);
        TSPResponse res = run(antColonyTSP, points, 0, -1, maxIterations);

        TSPResponse bestRes = res;

//        for (int i = 0; i < 10; ++i) {
//            antColonyTSP.initialPheromone(1 / bestRes.length);
//            res = runAco(antColonyTSP, points, -1, -1, maxIterations);
//            if (res.length < bestRes.length) {
//                bestRes = res;
//            }
//        }

        System.out.println("bestRes " + bestRes);


        output(bestRes, points);
    }

    static void test6() throws Exception {
        List<List<Point>> allPoints = getAllPointsFromFile("../outputs/tsp3.json");
        System.out.println("test6 allPoints.size() " + allPoints.size());
        for (int i = 0; i < allPoints.size(); ++i) {
            System.out.println("test6 " + i);
            List<Point> points = allPoints.get(i);
            AntColonyTSP antColonyTSP = AntColonyTSP.obtain(points.size());
            TSPResponse response = antColonyTSP.startPointIndex(points.size() - 1)
                    .endPointIndex(AntColonyTSP.RANDOM_POINT_INDEX)
                    .initialPheromone(0.001)
                    .maxIterations(Math.min(Math.max(points.size(), 20), 128))
                    .points(points)
                    .distance(new DistanceImpl(points, true))
                    .run();
            antColonyTSP.recycle();

            System.out.println("res " + response);
        }


//        output(res);
    }

    static void numberTest() {
        double a = 1;
        double b = a / 0;

        double c = Double.MAX_VALUE;
        double d = AntColonyTSP.MAX_VALUE;

        double e = d * 100000000;
        double f = Double.MAX_VALUE + Double.MAX_VALUE;
        double g = Double.MAX_VALUE * 10;
        double h = 1 / d;

        System.out.println(" " + a + " " + b + "");
    }

    static void test7() throws Exception {
        Driver driver = Main.getRandomDriver();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        driver.workStartTime = simpleDateFormat.parse("2019-05-26 08:00").getTime();
        List<Shipment> shipments = Main.getRandomShipments(8);

        shipments.get(0).getSender().startTime = simpleDateFormat.parse("2019-05-26 09:00").getTime();
        shipments.get(0).getSender().endTime = simpleDateFormat.parse("2019-05-26 10:00").getTime();
        shipments.get(0).getReceiver().startTime = simpleDateFormat.parse("2019-05-26 10:00").getTime();
        shipments.get(0).getReceiver().endTime = simpleDateFormat.parse("2019-05-26 14:00").getTime();

        shipments.get(1).getSender().startTime = simpleDateFormat.parse("2019-05-26 09:00").getTime();
        shipments.get(1).getSender().endTime = simpleDateFormat.parse("2019-05-26 11:00").getTime();
        shipments.get(1).getReceiver().startTime = simpleDateFormat.parse("2019-05-26 13:00").getTime();
        shipments.get(1).getReceiver().endTime = simpleDateFormat.parse("2019-05-26 14:10").getTime();

        shipments.get(2).getSender().startTime = simpleDateFormat.parse("2019-05-26 14:00").getTime();
        shipments.get(2).getSender().endTime = simpleDateFormat.parse("2019-05-26 15:00").getTime();
        shipments.get(2).getReceiver().startTime = simpleDateFormat.parse("2019-05-26 15:00").getTime();
        shipments.get(2).getReceiver().endTime = simpleDateFormat.parse("2019-05-26 17:00").getTime();

        Main.outputInputData(Collections.singletonList(driver), shipments, "tspInput1.json");

        AntColonyTSP antColonyTSP = AntColonyTSP.obtain(driver, shipments);

        long currentTime = simpleDateFormat.parse("2019-05-26 7:00").getTime();

        TSPResponse response = antColonyTSP
//                .startPointIndex(0)
//                .endPointIndex(AntColonyTSP.RANDOM_POINT_INDEX)
                .distance(new DistanceImpl(driver, shipments, true))
                .selector(new DriverTimeSelector(driver, currentTime))
                .run();

        antColonyTSP.recycle();

        System.out.println(response);

        output(response, response.originalPoints);
    }

    static void test8() throws Exception {
        Response data = Main.getInputDataFromFile("tspInput1.json");

        Driver driver = data.drivers.get(0);
        List<Shipment> shipments = data.shipments;

        AntColonyTSP antColonyTSP = AntColonyTSP.obtain(driver, shipments);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long currentTime = simpleDateFormat.parse("2019-05-26 7:00").getTime();

        TSPResponse response = antColonyTSP
                .startPointIndex(0)
                .endPointIndex(AntColonyTSP.RANDOM_POINT_INDEX)
                .distance(new DistanceImpl(driver, shipments, true))
                .selector(new DriverTimeSelector(driver, currentTime))
                .run();

        antColonyTSP.recycle();

        System.out.println(response);
//        System.out.println(response.originalPoints);

        output(response, response.originalPoints);
    }

    public static void main(String[] args) throws Exception {

//        test();
//        test1();
//        test2();
//        test3();
//        test4();
//        test5();
//        test6();
//        testObtain();

        test7();
//        test8();
    }
}
