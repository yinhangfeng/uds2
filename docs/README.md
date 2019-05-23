## 运单分配问题
n 运单分配给 m 个配送员 求如何分配使得总里程最低

i 代表订单标号
j 代表配送员标号

任务点
```java
class Point {
  double lat;
  double lng;
  long startTime; // 任务开始时间 早于这个时间到达可能需等待
  long endTime; // 任务结束时间 晚于这个时间到达会产生额外代价 endTime > startTime
  Point parent; // 父任务点 必须先到父任务点再到当前点 parent.endTime > endTime
}
```

配送员
```java
class Driver {
  Point pos; // 配送员当前位置 posj 无 startTime endTime 限制
  Point home; // 配送员回程点 homej 无 startTime endTime 限制(?) parent 为 posj
  int maxOrderNum; // 最大单量
  int maxMileage; // 最大里程(米)
  int load; // 最大容量(计费单位)
  long workStartTime; // 今天工作起始时间
  long workEndTime; // 今天工作结束时间
}
```

运单
```java
class Shipment {
  int weight; // 重量
  Point sender; // 发送点
  Point receiver; // 收货点 receiver.parent == sender receiver.endTime > sender.endTime
}
```

## 穷举

n 单分给 m 个配送员 总共情况为 `m^n`

如果用程序实现的话 相当于遍历 n 位 m 进制数

以 `n = 100, m = 10` 为例

总共情况为 `10 ^ 100` 根本不可能穷举

#### 按单量排除

上述情况包含很多明显的不可行解 可以排除 比如一个配送员分配了太多单的情况

假设每个司机最大单量为 k

为了能分配成功 当前运力肯定是足够的有 `k >= n / m`

这种情况下分配情况有多少呢?

问题变成 n 位 m 进制数 限制相同的数字出现的总次数

相当于有 m 种数字 每种 k 个 从中选取 n 个的排列数目

mk 个数字全排列 除掉 m 种数字每种的全排列 再取 n / mk 部分的排列数

近似为

$$
\left(\frac {A_{mk}^{mk}}{(A_k^k)^m}\right)^{\frac n{mk}}
$$

如果 k = 10 则总情况约为 2.357 * 10^92 还是天文数字


