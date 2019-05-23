package com.mrwind.uds.tsp;

import com.mrwind.uds.common.Distance;
import com.mrwind.uds.common.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 蚁群算法求解 TSP
 * startPointIndex endPointIndex 可用于指定开始点与结束点
 * 一般下面3种组合是有意义的
 * 1. startPointIndex = RANDOM_POINT_INDEX, endPointIndex = END_EQ_START_POINT_INDEX 任意点开始并回到开始点(默认)
 * 2. startPointIndex >= 0, endPointIndex = RANDOM_POINT_INDEX 固定点开始任意点结束
 * 3. startPointIndex >=0, endPointIndex >= 0, startPointIndex != endPointIndex 固定点开始 固定点结束
 * TODO 可以先跑一遍取 1 / 最短路径 为信息素初始值
 */
public class AntColonyTSP {

    public static final int RANDOM_POINT_INDEX = -1;
    public static final int END_EQ_START_POINT_INDEX = -2;

    // 使用穷举方式计算的最大点数
    // 点数对应时间 8: 9ms, 9: 37ms, 10: 293ms, 11: 2299ms, 12: 35034ms
    public static int MAX_EXHAUSTIVE_COUNT = 8;
    private static double MAX_ETA_POWER_BETA = Integer.MAX_VALUE;

    private double alpha = 1;
    private double beta = 2;
    private double evaporation = 0.5;
    private double antFactor = 1;
    // 初始信息素 应该用平均路程距离的倒数?
    private double initialPheromone = -1;

    private final int allocPointCount;
    private int startPointIndex = RANDOM_POINT_INDEX;
    private int endPointIndex = END_EQ_START_POINT_INDEX;
    private int pointCount;
    private int antCount;
    // 是否使用直角距离
    private boolean rightAngleDistance = true;
    private Ant[] ants;
    private int[] tempTour;
    // 距离矩阵 i j 相同为 -1
    private double distance[][];
    // 信息素矩阵
    private double pheromone[][];
    // 能见度缓存 etaPowerBeta[i][j] = (1 / distance[i][j]) ^ beta
    // MAX_ETA_POWER_BETA 表示 distance[i][j] 为 0
    private double etaPowerBeta[][];
    // 权重缓存(信息素强度与能见度的幂乘积) tauPowerAlphaMultiplyEtaPowerBeta[i][j] = pheromone[i][j] ^ alpha * etaPowerBeta[i][j];
    // 每次信息素更新之后都会被清空
    private double tauPowerAlphaMultiplyEtaPowerBeta[][];
    // 下一步点的权重 所有蚂蚁公用的数组
    private double tempWeights[];
    private Random random;
    // 各个点之间的关系 tree[i] 代表 i 点的父节点索引 -1 代表没有父节点
    // 只有父节点被访问过才能访问子节点
    private int[] tree;
    private int maxIterations;

    public AntColonyTSP(int pointCount) {
        this(pointCount, pointCount <= MAX_EXHAUSTIVE_COUNT);
    }

    /**
     * @param pointCount 点的数量
     * @param exhaustive 是否使用穷举
     *                   本类的实例对于 点数 <= pointCount 可以重复使用
     */
    public AntColonyTSP(int pointCount, boolean exhaustive) {
        if (pointCount < 1) {
            throw new IllegalArgumentException("pointCount < 1");
        }
        this.allocPointCount = this.pointCount = pointCount;
        antCount = (int) (pointCount * antFactor);
        distance = new double[pointCount][pointCount];
        tree = new int[pointCount];

        if (!exhaustive) {
            pheromone = new double[pointCount][pointCount];
            etaPowerBeta = new double[pointCount][pointCount];
            tauPowerAlphaMultiplyEtaPowerBeta = new double[pointCount][pointCount];
            tempWeights = new double[pointCount];
            ants = new Ant[antCount];
            for (int i = 0; i < antCount; ++i) {
                ants[i] = new Ant(pointCount);
            }
            tempTour = new int[pointCount];
            random = new Random();
        }
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

    public AntColonyTSP pointCount(int pointCount) {
        if (this.pointCount == pointCount) {
            return this;
        }
        if (pointCount > allocPointCount) {
            throw new IllegalArgumentException("pointCount 不能大于构造函数给出的初始值");
        }
        this.pointCount = pointCount;
        antCount = (int) (pointCount * antFactor);
        return this;
    }

    public AntColonyTSP rightAngleDistance(boolean rightAngleDistance) {
        this.rightAngleDistance = rightAngleDistance;
        return this;
    }

    public AntColonyTSP initialPheromone(double initialPheromone) {
        this.initialPheromone = initialPheromone;
        return this;
    }

    public AntColonyTSP maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    private void checkParams(List<Point> points) {
        pointCount(points.size());
        if (startPointIndex >= pointCount || endPointIndex >= pointCount) {
            throw new IllegalStateException("startPointIndex >= pointCount || endPointIndex >= pointCount");
        }
    }

    public Response run(List<Point> points) {
        int pointCount = points.size();

        int maxExhaustiveCount = MAX_EXHAUSTIVE_COUNT;
        if (startPointIndex >= 0) {
            maxExhaustiveCount++;
        }
        if (endPointIndex >= 0 && maxExhaustiveCount != startPointIndex) {
            maxExhaustiveCount++;
        }

        if (pointCount > maxExhaustiveCount) {
            return runACO(points, maxIterations <= 0 ? Math.max(pointCount, 10) : maxIterations);
        }
        return runExhaustive(points);
    }

    /**
     * 执行蚁群算法
     *
     * @param maxIterations 最大迭代次数
     */
    public Response runACO(List<Point> points, int maxIterations) {
        checkParams(points);

        initTree(points);
        initDistance(points, true);
        initPheromone();
        updateTauPowerAlphaMultiplyEtaPowerBeta();

        int pointCount = points.size();
        int startPointIndex = this.startPointIndex;
        int endPointIndex = this.endPointIndex;
        Ant[] ants = this.ants;
        int antCount = this.antCount;
        int[] bestTour = tempTour;
        double bestLength = Double.MAX_VALUE;
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
        for (; ; ) {
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

                if (ant.length < bestLength) {
                    bestLength = ant.length;
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
                // 最后一次迭代不需要更新信息素
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

            if (iteration >= maxIterations) {
                break;
            }
        }

        this.tempTour = bestTour;

        // 贪心优化局部路径
        int[] tour = new int[pointCount];
        System.arraycopy(bestTour, 0, tour, 0, pointCount);
        boolean endEqStart = endPointIndex >= 0 && endPointIndex == startPointIndex || endPointIndex == END_EQ_START_POINT_INDEX;
        // 目前不支持优化终点回到起点 所以 pointCount - 1
        double newBestLength = Greedy.greedyTour(tour, bestLength, endEqStart ? pointCount - 1 : pointCount, distance, tree);

//        if (newBestLength < bestLength) {
//            System.out.println("greedyTour 得到了更优的路径: " + newBestLength + " " + Arrays.toString(tour) + " old bestTour: " + bestLength + " " + Arrays.toString(bestTour));
//        }

//        double l1 = 0;
//        double l2 = 0;
//        for (int i = 0; i < pointCount - 1; ++i) {
//            l1 += distance[bestTour[i]][bestTour[i + 1]];
//            l2 += distance[tour[i]][tour[i + 1]];
//        }
//        System.out.println("xxxx l1: " + l1 + " l2: " + l2 + " bestLength: " + bestLength + " newBestLength: " + newBestLength);
//        assert Math.abs(l1 - bestLength) < 0.0000001;
//        assert Math.abs(l2 - newBestLength) < 0.0000001;

        Response response = new Response();
        response.points = points;
        response.tour = tour;
        response.length = newBestLength;
        response.endEqStart = endEqStart;
        response.iterationNum = bestIteration;
        return response;
    }

    private void initTree(List<Point> points) {
        Utils.pointsToTree(points, tree);
    }

    private void initDistance(List<Point> points, boolean initEtaPowerBeta) {
        double weight;
        for (int i = 0; i < pointCount; ++i) {
            distance[i][i] = -1;
            for (int j = i + 1; j < pointCount; ++j) {
                if (rightAngleDistance) {
                    weight = Distance.getRightAngleDistance(points.get(i), points.get(j));
                } else {
                    weight = Distance.getDistance(points.get(i), points.get(j));
                }
                distance[i][j] = distance[j][i] = weight;
                if (initEtaPowerBeta) {
                    if (weight == 0) {
                        // 如果两个点的距离为 0 则 etaPowerBeta 用一个很大的值代替
                        etaPowerBeta[i][j] = etaPowerBeta[j][i] = MAX_ETA_POWER_BETA;
                    } else {
                        etaPowerBeta[i][j] = etaPowerBeta[j][i] = Math.pow(1 / weight, beta);
                    }
                }
            }
        }
    }

    private void initPheromone() {
        // 初始化信息素 TODO 通过贪心算法获取初始值?
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < pointCount; j++) {
                pheromone[i][j] = initialPheromone < 0 ? 1 : initialPheromone;
            }
        }
    }

    /**
     * 访问第一个点
     */
    private void visitStart(Ant ant) {
        if (startPointIndex >= 0) {
            ant.visit(0, startPointIndex);
        } else {
            double totalWeight = 0;
            double[] weights = tempWeights;
            for (int i = 0; i < pointCount; ++i) {
                // 第一个点不需要考虑父点是否被访问过
                if (ant.visited[i] == 0 && tree[i] < 0) {
                    totalWeight++;
                    weights[i] = 1;
                } else {
                    weights[i] = 0;
                }
            }
            double randomNo = random.nextInt((int) totalWeight);
            int randomStartIndex = 0;
            double w = 0;
            for (; randomStartIndex < pointCount; ++randomStartIndex) {
                w += weights[randomStartIndex];
                if (w >= randomNo) {
                    break;
                }
            }
            ant.visit(0, randomStartIndex);
        }
    }

    /**
     * 访问下一个点
     */
    private void visitNext(Ant ant, int nextIndex) {
        int pointCount = this.pointCount;
        double[][] tauPowerAlphaMultiplyEtaPowerBeta = this.tauPowerAlphaMultiplyEtaPowerBeta;
        int[] tree = this.tree;
        byte[] visited = ant.visited;
        int current = ant.tour[nextIndex - 1];
        // 各个点的权重
        double[] weights = tempWeights;
        // 权重总和
        double totalWeight = 0.0;
        double weight;
        int parent;
        for (int i = 0; i < pointCount; i++) {
            if (visited[i] == 0 && ((parent = tree[i]) < 0 || visited[parent] == Ant.VISITED)) {
                // 没被访问过 且 没有父节点或者父节点被访问过
                weight = tauPowerAlphaMultiplyEtaPowerBeta[current][i];
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
        ant.length += distance[current][select];

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
        ant.length += distance[current][endPointIndex == END_EQ_START_POINT_INDEX ? ant.tour[0] : endPointIndex];
    }

    /**
     * 更新信息素
     * TODO 第一次更新信息素可以采用重置方式
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
        double pheromoneToBeAdded;
        Ant ant;
        int curIndex;
        int nextIndex;
        for (int i = 0; i < antCount; i++) {
            ant = ants[i];
            pheromoneToBeAdded = 1 / ant.length;
            for (int j = 1; j < pointCount; j++) {
                curIndex = ant.tour[j - 1];
                nextIndex = ant.tour[j];
                pheromone[curIndex][nextIndex] += pheromoneToBeAdded;
                pheromone[nextIndex][curIndex] += pheromoneToBeAdded;
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
                tauPowerAlphaMultiplyEtaPowerBeta[i][j] = Math.pow(pheromone[i][j], this.alpha) * etaPowerBeta[i][j];
            }
        }
    }

    /**
     * 执行穷举算法
     */
    public Response runExhaustive(List<Point> points) {
        checkParams(points);

        initTree(points);
        initDistance(points, false);

        int pointCount = points.size();
        int startPointIndex = this.startPointIndex;
        int endPointIndex = this.endPointIndex;

        Response response = new Response();
        response.points = points;
        response.length = Integer.MAX_VALUE;
        response.tour = new int[pointCount];
        response.endEqStart = endPointIndex == END_EQ_START_POINT_INDEX || endPointIndex >= 0 && endPointIndex == startPointIndex;

        Ant ant = new Ant(pointCount);
        int tourIndex = 0;
        if (startPointIndex >= 0) {
            ant.visit(tourIndex++, startPointIndex);
        }
        if (endPointIndex >= 0 && endPointIndex != startPointIndex) {
            ant.visited[endPointIndex] = Ant.END_POINT;
        }
        recursiveTSP(tourIndex, ant, pointCount, distance, tree, startPointIndex, endPointIndex, response);

        return response;
    }

    /**
     * 递归穷举路径
     *
     * @param currentIndex    当前路径 index
     * @param ant             记录当前路径 路径长度 与各节点访问状态
     * @param pointCount      数据
     * @param distance        数据
     * @param tree            数据
     * @param startPointIndex
     * @param endPointIndex
     * @param bestRes         结果输出参数
     */
    private static void recursiveTSP(int currentIndex, Ant ant, int pointCount, double[][] distance, int[] tree, int startPointIndex, int endPointIndex, Response bestRes) {
        int prevIndex = currentIndex - 1;
        int prevPointIndex = prevIndex < 0 ? -1 : ant.tour[prevIndex];
        boolean isEnd = currentIndex == pointCount - 1;
        double length = ant.length;
        double prevLength = length;

        if (isEnd && endPointIndex >= 0 && endPointIndex != startPointIndex) {
            // 确定且不回起点的终点
            ant.tour[currentIndex] = endPointIndex;
            length += distance[prevPointIndex][endPointIndex];
        } else {
            byte[] visited = ant.visited;
            int parent;
            for (int pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
                if (visited[pointIndex] == 0 && ((parent = tree[pointIndex]) < 0 || visited[parent] == Ant.VISITED)) {
                    ant.visit(currentIndex, pointIndex);
                    if (prevPointIndex >= 0) {
                        length += distance[prevPointIndex][pointIndex];
                    }
                    // 如果当前路径长度已经大于当前最佳路径 就不用继续了
                    if (!isEnd && length < bestRes.length) {
                        ant.length = length;
                        recursiveTSP(currentIndex + 1, ant, pointCount, distance, tree, startPointIndex, endPointIndex, bestRes);
                        length = ant.length = prevLength;
                    }

                    // 退一步
                    visited[pointIndex] = 0;
                }
            }
        }

        if (isEnd) {
            if (endPointIndex == END_EQ_START_POINT_INDEX || endPointIndex >= 0 && endPointIndex == startPointIndex) {
                // 终点需要返回起点
                length += distance[ant.tour[currentIndex]][ant.tour[0]];
            }

            if (length < bestRes.length) {
                // 这里不能使用交换 tour 的方式 因为其它路径需要 tour 中的信息
                ant.copyTour(bestRes.tour);
                bestRes.length = length;
            }
        }
    }

    private static final List<AntColonyTSP> sCache = new ArrayList<>();

    /**
     * 获取 AntColonyTSP 对象
     */
    public static AntColonyTSP obtain(int pointCount) {
        synchronized (sCache) {
            for (int i = sCache.size() - 1; i >= 0; --i) {
                AntColonyTSP inst = sCache.get(i);
                if (inst.allocPointCount >= pointCount) {
                    sCache.remove(i);
                    return inst.startPointIndex(RANDOM_POINT_INDEX)
                            .endPointIndex(END_EQ_START_POINT_INDEX)
                            .rightAngleDistance(true);
                }
            }
        }
        int allocPointCount = pointCount;
        if (allocPointCount < 32) {
            allocPointCount = 32;
        }
        return new AntColonyTSP(allocPointCount);
    }

    /**
     * 回收 AntColonyTSP 对象
     */
    public void recycle() {
        if (allocPointCount < 32) {
            return;
        }
        if (allocPointCount > 128) {
            return;
        }
        if (distance == null) {
            return;
        }
        synchronized (sCache) {
            if (sCache.size() > 8) {
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
