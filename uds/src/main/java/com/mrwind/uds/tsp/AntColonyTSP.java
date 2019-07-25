package com.mrwind.uds.tsp;

import com.mrwind.uds.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 蚁群算法求解 TSP
 * startPointIndex endPointIndex 可用于指定开始点与结束点
 * 一般下面3种组合是有意义的
 * 1. startPointIndex = RANDOM_POINT_INDEX, endPointIndex = END_EQ_START_POINT_INDEX 任意点开始并回到开始点(默认)
 * 2. startPointIndex >= 0, endPointIndex = RANDOM_POINT_INDEX 固定点开始任意点结束
 * 3. startPointIndex >=0, endPointIndex >= 0, startPointIndex != endPointIndex 固定点开始 固定点结束
 */
public class AntColonyTSP {

    public static final int RANDOM_POINT_INDEX = -1;
    public static final int END_EQ_START_POINT_INDEX = -2;

    // 使用穷举方式计算的最大点数
    // 点数对应时间 8: 9ms, 9: 37ms, 10: 293ms, 11: 2299ms, 12: 35034ms
    public static int MAX_EXHAUSTIVE_COUNT = 8;
    // 最大的数值 防止计算中出现数值上溢
    public static final double MAX_VALUE = ((double) Integer.MAX_VALUE) * 1000;
    private static double MAX_ETA_POWER_BETA = MAX_VALUE;

    private double alpha = 1;
    private double beta = 2;
    private double evaporation = 0.5;
    private float antFactor = 1;
    // 初始信息素 应该用平均路程距离的倒数?
    private double initialPheromone = -1;

    private int pointCount = -1;
    private int startPointIndex = RANDOM_POINT_INDEX;
    private int endPointIndex = END_EQ_START_POINT_INDEX;
    private Distance distance;
    private int maxIterations;
    private Weigher weigher = Weigher.LENGTH_WEIGHER;
    private List<Point> originalPoints;

    private final int allocPointCount;
    private TSPPoint[] points;
    private int antCount;
    private Ant[] ants;
    private int[] tempTour;
    // 信息素矩阵
    private double[][] pheromone;
    // 能见度缓存 etaPowerBeta[i][j] = (1 / distance[i][j]) ^ beta
    // MAX_ETA_POWER_BETA 表示 distance[i][j] 为 0
    private double[][] etaPowerBeta;
    // 权重缓存(信息素强度与能见度的幂乘积) tauPowerAlphaMultiplyEtaPowerBeta[i][j] = pheromone[i][j] ^ alpha * etaPowerBeta[i][j];
    // 每次信息素更新之后都会被清空
    private double[][] tauPowerAlphaMultiplyEtaPowerBeta;
    // 下一步点的权重 所有蚂蚁公用的数组
    private double[] tempWeights;
    private Random random;

    public AntColonyTSP(int pointCount) {
        this(pointCount, pointCount <= MAX_EXHAUSTIVE_COUNT);
    }

    /**
     * @param allocPointCount 点的数量
     * @param exhaustive      是否使用穷举
     *                        <p>
     *                        本类的实例对于 点数 <= allocPointCount 可以重复使用
     */
    public AntColonyTSP(int allocPointCount, boolean exhaustive) {
        if (allocPointCount < 1) {
            throw new IllegalArgumentException("allocPointCount < 1");
        }
        this.allocPointCount = allocPointCount;
        antCount = allocPointCount;
        points = new TSPPoint[allocPointCount];

        if (!exhaustive) {
            pheromone = new double[allocPointCount][allocPointCount];
            etaPowerBeta = new double[allocPointCount][allocPointCount];
            tauPowerAlphaMultiplyEtaPowerBeta = new double[allocPointCount][allocPointCount];
            tempWeights = new double[allocPointCount];
            ants = new Ant[antCount];
            for (int i = 0; i < antCount; ++i) {
                ants[i] = new Ant(allocPointCount);
            }
            tempTour = new int[allocPointCount];
            random = new Random();
        }
    }

    /**
     * 获取蚁群运算的 pointCount
     * TODO 解耦 这里不应该与运单司机有关联
     */
    public static int getDriverAndShipmentsPointCount(Driver driver, List<Shipment> shipments) {
        return shipments.size() * 2 + 1;
    }

    /**
     * 蚁群算法的 alpha beta evaporation 设置
     */
    public AntColonyTSP hyperParameters(double alpha, double beta, double evaporation) {
        this.alpha = alpha;
        this.beta = beta;
        this.evaporation = evaporation;
        return this;
    }

    /**
     * 蚂蚁数量对点数的比例
     * 默认为1 必须 <= 1
     */
    public AntColonyTSP antFactor(float antFactor) {
        if (antFactor > 1) {
            throw new IllegalArgumentException("antFactor > 1");
        }
        this.antFactor = antFactor;
        if (pointCount > 0) {
            antCount = (int) Math.ceil(pointCount * antFactor);
        }
        return this;
    }

    /**
     * 起点 index
     * 如果为 RANDOM_POINT_INDEX 则默认会随机选择起点
     */
    public AntColonyTSP startPointIndex(int startPointIndex) {
        if (startPointIndex < RANDOM_POINT_INDEX) {
            throw new IllegalArgumentException("startPointIndex < RANDOM_POINT_INDEX");
        }
        this.startPointIndex = startPointIndex;
        return this;
    }

    /**
     * 终点 index
     * 如果为 RANDOM_POINT_INDEX 则默认会随机选择终点
     * 如果为 END_EQ_START_POINT_INDEX 则代表回到起点
     */
    public AntColonyTSP endPointIndex(int endPointIndex) {
        if (endPointIndex < END_EQ_START_POINT_INDEX) {
            throw new IllegalArgumentException("endPointIndex < END_EQ_START_POINT_INDEX");
        }
        this.endPointIndex = endPointIndex;
        return this;
    }

    /**
     * 设置距离计算器
     */
    public AntColonyTSP distance(Distance distance) {
        this.distance = distance;
        return this;
    }

    /**
     * 设置权值计算器
     */
    public AntColonyTSP weigher(Weigher weigher) {
        this.weigher = weigher;
        return this;
    }

    /**
     * 设置任务点列表
     */
    public AntColonyTSP points(List<Point> points) {
        pointCount(points.size());

        Point point;
        for (int i = 0; i < pointCount; ++i) {
            point = points.get(i);
            if (point.dependency >= pointCount) {
                throw new IllegalArgumentException("point.dependency >= pointCount point: " + point + " pointCount: " + pointCount);
            }
            addTSPPoint(point, i, point.dependency);
        }

        originalPoints = points;
        return this;
    }

    /**
     * 设置司机和运单 自动转换为任务点列表
     * 会同时设置 startIndex endIndex
     */
    public AntColonyTSP driverAndShipments(Driver driver, List<Shipment> shipments) {
        pointCount(getDriverAndShipmentsPointCount(driver, shipments));

        addTSPPoint(driver.position, 0, -1);
        int tspPointIndex = 1;
        Shipment shipment;
        for (Shipment value : shipments) {
            shipment = value;
            addTSPPoint(shipment.sender, tspPointIndex, -1);
            addTSPPoint(shipment.receiver, tspPointIndex + 1, tspPointIndex);
            tspPointIndex += 2;
        }

        startPointIndex(0);
        // TODO
        endPointIndex(RANDOM_POINT_INDEX);

        originalPoints = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; ++i) {
            originalPoints.add(points[i].point);
        }

        return this;
    }

    private void addTSPPoint(Point point, int index, int dependency) {
        TSPPoint tspPoint = points[index];
        if (tspPoint == null) {
            points[index] = new TSPPoint(point, index, dependency);
        } else {
            tspPoint.point = point;
            // TODO
//            tspPoint.dependency = dependency;
            tspPoint.index = index;
        }
    }

    private void pointCount(int pointCount) {
        if (pointCount > allocPointCount) {
            throw new IllegalArgumentException("pointCount 不能大于构造函数给出的初始值");
        }
        this.pointCount = pointCount;
        antCount = (int) Math.ceil(pointCount * antFactor);
    }

    public AntColonyTSP initialPheromone(double initialPheromone) {
        this.initialPheromone = initialPheromone;
        return this;
    }

    public AntColonyTSP maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public TSPResponse run() {
        if (pointCount < 0) {
            throw new IllegalStateException("points not set");
        }
        if (distance == null) {
            throw new IllegalStateException("distance not set");
        }

        if (pointCount < 2) {
            TSPResponse response = new TSPResponse();
            if (pointCount == 1) {
                response.tour = new int[]{0};
            }
            response.originalPoints = originalPoints;
            return response;
        }

        int maxExhaustiveCount = MAX_EXHAUSTIVE_COUNT;
        if (startPointIndex >= 0) {
            maxExhaustiveCount++;
        }
        if (endPointIndex >= 0 && maxExhaustiveCount != startPointIndex) {
            maxExhaustiveCount++;
        }

        if (pointCount > maxExhaustiveCount) {
            return runACO();
        }
        return runExhaustive();
    }

    /**
     * 执行蚁群算法
     *
     * @param maxIterations 最大迭代次数
     */
    public TSPResponse runACO(int maxIterations) {
        initPheromone();
        initEtaPowerBeta();
        updateTauPowerAlphaMultiplyEtaPowerBeta();

        int pointCount = this.pointCount;
        int startPointIndex = this.startPointIndex;
        int endPointIndex = this.endPointIndex;
        Ant[] ants = this.ants;
        int antCount = this.antCount;
        int[] bestTour = tempTour;
        double bestLength = Double.MAX_VALUE;
        double bestFitness = Double.MAX_VALUE;
        Ant ant;
        Ant bestAnt = null;

        int tourEndIndex = pointCount - 1;
        // 是否需要标记最后一个点
        boolean needMarkEnd = false;
        // 如果终点已经确定且不是起点 则要少走一步
        if (endPointIndex >= 0 && endPointIndex != startPointIndex) {
            tourEndIndex--;
            needMarkEnd = true;
        }

        int iteration = 0;
        int antIndex;
        int tourIndex;
        // 获取到最佳路径的迭代次数
        int bestIteration = 0;
        do {
            // TEST 本次迭代最佳长度
//            double iterationBestLength = Double.MAX_VALUE;

            for (antIndex = 0; antIndex < antCount; ++antIndex) {
                ant = ants[antIndex];
                ant.clear();
                if (needMarkEnd) {
                    ant.visited[endPointIndex] = Ant.END_POINT;
                }

                visitStart(ant);
                for (tourIndex = 1; tourIndex <= tourEndIndex; ++tourIndex) {
                    visitNext(ant, tourIndex);
                }
                if (endPointIndex != RANDOM_POINT_INDEX) {
                    // endPointIndex 确定或者与起点相同
                    visitEnd(ant, tourIndex);
                }

                if (weigher.isBetter(bestLength, bestFitness, ant)) {
                    bestLength = ant.length;
                    bestFitness = ant.fitness;
                    bestAnt = ant;
                    bestIteration = iteration;
                }

                // TEST
//                if (ant.length < iterationBestLength) {
//                    iterationBestLength = ant.length;
//                }
            }

//            if (bestIteration == iteration) {
//                System.out.println("iteration: " + iteration + " bestAnt: " + bestAnt + " bestAnt update: " + (bestIteration == iteration));
//            }

            if (++iteration < maxIterations) {
                // 更新信息素
                updatePheromone();
                updateTauPowerAlphaMultiplyEtaPowerBeta();
            }

            if (bestAnt != null) {
                // 交换 ant.tour 与 bestTour 省去一次拷贝
                // 因为更新信息素的时候需要用到 ant.tour 所以放在更新信息素之后
                int[] temp = bestAnt.tour;
                bestAnt.tour = bestTour;
                bestTour = temp;
                bestAnt = null;
            }

        } while (iteration < maxIterations);

        this.tempTour = bestTour;

        int[] tour = new int[pointCount];
        System.arraycopy(bestTour, 0, tour, 0, pointCount);
        boolean endEqStart = endPointIndex >= 0 && endPointIndex == startPointIndex || endPointIndex == END_EQ_START_POINT_INDEX;

        if (weigher == Weigher.LENGTH_WEIGHER && endPointIndex == RANDOM_POINT_INDEX) {
            // 贪心优化局部路径
            // 目前不支持优化终点回到起点 所以 pointCount - 1
            // TODO 暂不支持基于 tspResponse 比较时的交换
            // TODO 对起点返回终点的交换有 BUG
            // TODO 暂不支持指定终点的交换
            double newBestLength = Greedy.greedyTour(tour, bestLength, endEqStart ? pointCount - 1 : pointCount, distance, points);

//            if (newBestLength < bestLength) {
//                System.out.println("greedyTour 得到了更优的路径: " + newBestLength + " " + Arrays.toString(tour) + " old bestTour: " + bestLength + " " + Arrays.toString(bestTour));
//            }
//
//            double l1 = 0;
//            double l2 = 0;
//            for (int i = 0; i < pointCount - 1; ++i) {
//                l1 += getDistance(bestTour[i], bestTour[i + 1]);
//                l2 += getDistance(tour[i], tour[i + 1]);
//            }
//            System.out.println("xxxx l1: " + l1 + " l2: " + l2 + " bestLength: " + bestLength + " newBestLength: " + newBestLength);
//            assert Math.abs(l1 - bestLength) < 0.0000001;
//            assert Math.abs(l2 - newBestLength) < 0.0000001;

            bestLength = newBestLength;
        }

        TSPResponse response = new TSPResponse();
        response.tour = tour;
        response.length = bestLength;
        response.fitness = bestFitness;
        response.endEqStart = endEqStart;
        response.iterationNum = bestIteration;
        response.originalPoints = originalPoints;
        return response;
    }

    public TSPResponse runACO() {
        return runACO(maxIterations <= 0 ? Math.min(Math.max(pointCount, 10), 100) : maxIterations);
    }

    private double getDistance(int startIndex, int endIndex) {
        return distance.getDistance(points[startIndex].point, points[endIndex].point);
    }

    private void initPheromone() {
        // 初始化信息素 TODO 通过贪心算法获取初始值?
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < pointCount; j++) {
                pheromone[i][j] = initialPheromone < 0 ? 0.1 : initialPheromone;
            }
        }
    }

    private void initEtaPowerBeta() {
        double weight;
        for (int i = 0; i < pointCount; i++) {
            for (int j = i + 1; j < pointCount; j++) {
                weight = getDistance(i, j);
                if (weight < 1 / MAX_ETA_POWER_BETA) {
                    // 防止数值上溢
                    etaPowerBeta[i][j] = etaPowerBeta[j][i] = MAX_ETA_POWER_BETA;
                } else {
                    etaPowerBeta[i][j] = etaPowerBeta[j][i] = Math.pow(1 / weight, beta);
                }
            }
        }
    }

    /**
     * 访问第一个点
     */
    private void visitStart(Ant ant) {
        int visitIndex;
        if (startPointIndex >= 0) {
            visitIndex = startPointIndex;
        } else {
            double totalWeight = 0;
            double[] weights = tempWeights;
            for (int i = 0; i < pointCount; ++i) {
                // 第一个点不需要考虑父点是否被访问过
                if (ant.visited[i] == 0 && points[i].dependency < 0) {
                    totalWeight++;
                    weights[i] = 1;
                    weigher.processWeight(1, ant, null, points[i]);
                } else {
                    weights[i] = 0;
                    weigher.processWeight(0, ant, null, points[i]);
                }
            }
            double randomNo = random.nextInt((int) totalWeight);
            visitIndex = 0;
            double w = 0;
            for (; visitIndex < pointCount; ++visitIndex) {
                w += weights[visitIndex];
                if (w >= randomNo) {
                    break;
                }
            }
        }
        ant.visit(0, visitIndex);
        weigher.calcFitness(ant, null, points[visitIndex], 0);
    }

    /**
     * 访问下一个点
     */
    private void visitNext(Ant ant, int nextIndex) {
        int pointCount = this.pointCount;
        double[][] tauPowerAlphaMultiplyEtaPowerBeta = this.tauPowerAlphaMultiplyEtaPowerBeta;
        TSPPoint[] points = this.points;
        byte[] visited = ant.visited;
        Weigher weigher = this.weigher;
        int current = ant.tour[nextIndex - 1];
        TSPPoint currentPoint = points[nextIndex - 1];
        // 各个点的权重
        double[] weights = tempWeights;
        // 权重总和
        double totalWeight = 0.0;
        double weight;
        int dependency;
        for (int i = 0; i < pointCount; i++) {
            if (visited[i] == 0 && ((dependency = points[i].dependency) < 0 || visited[dependency] == Ant.VISITED)) {
                // 没被访问过 且 没有父节点或者父节点被访问过
                weight = tauPowerAlphaMultiplyEtaPowerBeta[current][i];
                // 修改权重
                weight = weigher.processWeight(weight, ant, currentPoint, points[i]);
                weights[i] = weight;
                totalWeight += weight;
            } else {
                weights[i] = 0;
            }
        }
        // TODO totalWeight 有可能为 0 ?
        assert totalWeight > 0;

        int select;
        double randomNo = random.nextDouble() * totalWeight;
        double w = 0;
//        System.out.println("visitNext randomNo: " + randomNo + " totalWeight: " + totalWeight + " weights: " + Arrays.toString(weights) + " visited: " + Arrays.toString(visited));
        for (select = 0; ; ++select) {
            w += weights[select];
            if (w >= randomNo) {
                break;
            }
        }

        ant.visit(nextIndex, select);
        double distance = getDistance(current, select);
        ant.length += distance;
        // TODO
        weigher.calcFitness(ant, currentPoint, points[select], distance);

//        System.out.println("visitNext select: " + select);
    }

    /**
     * 访问终点
     */
    private void visitEnd(Ant ant, int endIndex) {
        int current = ant.tour[endIndex - 1];
        if (endPointIndex != startPointIndex && endPointIndex != END_EQ_START_POINT_INDEX) {
            // 起点与终点不同才需要计入 tour
            ant.tour[endIndex] = endPointIndex;
        }
        int endPointIdx = endPointIndex == END_EQ_START_POINT_INDEX ? ant.tour[0] : endPointIndex;
        double distance = getDistance(current, endPointIdx);
        ant.length += distance;
        weigher.calcFitness(ant, points[current], points[endPointIdx], distance);
    }

    /**
     * 更新信息素
     */
    private void updatePheromone() {
//        System.out.println("updatePheromone:\n" + Arrays.deepToString(pheromone));

        int pointCount = this.pointCount;
        double[][] pheromone = this.pheromone;

        // 挥发
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < pointCount; j++) {
                pheromone[i][j] *= evaporation;
            }
        }

        // 更新每只蚂蚁路径的信息素
        double pheromoneIncrement;
        Ant ant;
        int curIndex;
        int nextIndex;
        for (int i = 0; i < antCount; i++) {
            ant = ants[i];
            pheromoneIncrement = weigher.getPheromoneIncrement(ant);
            // TODO END_EQ_START_POINT_INDEX
            for (int j = 1; j < pointCount; j++) {
                curIndex = ant.tour[j - 1];
                nextIndex = ant.tour[j];
                pheromone[curIndex][nextIndex] += pheromoneIncrement;
                pheromone[nextIndex][curIndex] += pheromoneIncrement;
            }
        }
    }

    /**
     * 更新权重缓存 需在信息素更新之后更新
     */
    private void updateTauPowerAlphaMultiplyEtaPowerBeta() {
//        System.out.println("updateTauPowerAlphaMultiplyEtaPowerBeta:\n" + Arrays.deepToString(tauPowerAlphaMultiplyEtaPowerBeta));

        int pointCount = this.pointCount;
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < pointCount; j++) {
                tauPowerAlphaMultiplyEtaPowerBeta[i][j] = Math.pow(pheromone[i][j], alpha) * etaPowerBeta[i][j];
            }
        }
    }

    /**
     * 执行穷举算法
     */
    public TSPResponse runExhaustive() {
        int pointCount = this.pointCount;
        int startPointIndex = this.startPointIndex;
        int endPointIndex = this.endPointIndex;

        TSPResponse response = new TSPResponse();
        response.length = Double.MAX_VALUE;
        response.fitness = Double.MAX_VALUE;
        response.tour = new int[pointCount];
        response.endEqStart = endPointIndex == END_EQ_START_POINT_INDEX || endPointIndex >= 0 && endPointIndex == startPointIndex;
        response.originalPoints = originalPoints;

        Ant ant = new Ant(pointCount);
        int tourIndex = 0;
        if (startPointIndex >= 0) {
            ant.visit(tourIndex++, startPointIndex);
        }
        if (endPointIndex >= 0 && endPointIndex != startPointIndex) {
            ant.visited[endPointIndex] = Ant.END_POINT;
        }
        recursiveTSP(tourIndex, ant, response);

        return response;
    }

    /**
     * 递归穷举路径
     *
     * @param currentIndex 当前路径 index
     * @param ant          记录当前路径 路径长度 与各节点访问状态
     * @param bestRes      结果输出参数
     */
    private void recursiveTSP(int currentIndex, Ant ant, TSPResponse bestRes) {
        int prevIndex = currentIndex - 1;
        int prevPointIndex = prevIndex < 0 ? -1 : ant.tour[prevIndex];
        boolean isEnd = currentIndex == pointCount - 1;
        double prevLength = ant.length;
        double prevFitness = ant.fitness;

        if (isEnd && endPointIndex >= 0 && endPointIndex != startPointIndex) {
            // 确定且不回起点的终点
            ant.tour[currentIndex] = endPointIndex;
            double distance = getDistance(prevPointIndex, endPointIndex);
            ant.length += distance;
            weigher.calcFitness(ant, points[prevPointIndex], points[endPointIndex], distance);
        } else {
            byte[] visited = ant.visited;
            int dependency;
            for (int pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
                if (visited[pointIndex] == 0 && ((dependency = points[pointIndex].dependency) < 0 || visited[dependency] == Ant.VISITED)) {
                    // 尝试访问 pointIndex
                    ant.visit(currentIndex, pointIndex);
                    double distance = 0;
                    if (prevPointIndex >= 0) {
                        distance = getDistance(prevPointIndex, pointIndex);
                        ant.length += distance;
                    }
                    weigher.calcFitness(ant, prevPointIndex >= 0 ? points[prevPointIndex] : null, points[pointIndex], distance);

                    if (!isEnd) {
                        // 当前长度小于最佳长度 则递归访问
                        if (weigher.isBetter(bestRes.length, bestRes.fitness, ant)) {
                            recursiveTSP(currentIndex + 1, ant, bestRes);
                        }

                        // 非最后一步才需要恢复 length 和 tspResponse
                        ant.length = prevLength;
                        ant.fitness = prevFitness;
                    }

                    // 退一步
                    visited[pointIndex] = 0;
                }
            }
        }

        if (isEnd) {
            if (endPointIndex == END_EQ_START_POINT_INDEX || endPointIndex >= 0 && endPointIndex == startPointIndex) {
                // 终点需要返回起点
                double distance = getDistance(ant.tour[currentIndex], ant.tour[0]);
                ant.length += distance;
                weigher.calcFitness(ant, points[ant.tour[currentIndex]], points[ant.tour[0]], distance);
            }

            if (weigher.isBetter(bestRes.length, bestRes.fitness, ant)) {
                // 这里不能使用交换 tour 的方式 因为其它路径需要 tour 中的信息
                ant.copyTour(bestRes.tour);
                bestRes.length = ant.length;
                bestRes.fitness = ant.fitness;
            }
        }
    }

    private static final List<AntColonyTSP> sCache = new ArrayList<>();

    public static AntColonyTSP obtain(Driver driver, List<Shipment> shipments) {
        int pointCount = getDriverAndShipmentsPointCount(driver, shipments);
        AntColonyTSP antColonyTSP = obtain(pointCount);
        antColonyTSP.driverAndShipments(driver, shipments);
        return antColonyTSP;
    }

    /**
     * 获取 AntColonyTSP 对象
     */
    public static AntColonyTSP obtain(int pointCount) {
        synchronized (sCache) {
            for (int i = sCache.size() - 1; i >= 0; --i) {
                AntColonyTSP inst = sCache.get(i);
                if (inst.allocPointCount >= pointCount) {
                    sCache.remove(i);
                    return inst;
                }
            }
        }
        int allocPointCount = pointCount;
        if (allocPointCount < 16) {
            allocPointCount = 16;
        }
        return new AntColonyTSP(allocPointCount);
    }

    /**
     * 回收 AntColonyTSP 对象
     */
    public void recycle() {
        if (allocPointCount < 16) {
            return;
        }
        if (allocPointCount > 128) {
            return;
        }
        alpha = 1;
        beta = 2;
        evaporation = 0.5;
        antFactor = 1;
        initialPheromone = -1;
        startPointIndex = RANDOM_POINT_INDEX;
        endPointIndex = END_EQ_START_POINT_INDEX;
        maxIterations = -1;
        distance = null;
        pointCount = -1;
        for (TSPPoint point : points) {
            if (point != null) {
                point.point = null;
            }
        }
        weigher = Weigher.LENGTH_WEIGHER;
        originalPoints = null;
        synchronized (sCache) {
            if (sCache.size() > 16) {
                return;
            }
            int i = sCache.size() - 1;
            for (; i >= 0; --i) {
                AntColonyTSP inst = sCache.get(i);
                if (inst.allocPointCount >= allocPointCount) {
                    break;
                }
            }
            sCache.add(i + 1, this);
        }
    }
}
