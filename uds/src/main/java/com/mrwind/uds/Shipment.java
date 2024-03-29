package com.mrwind.uds;

/**
 * 运单
 * TODO 支持已取件运单 也就是不需要去 sender
 */
public class Shipment {
    public String id;
    // 重量
    public int weight;
    // 发货点
    public Point sender;
    // 收货点 receiver.dependency == sender receiver.endTime > sender.endTime
    public Point receiver;
    // 如果运单已经分配给配送员 是否可以重新分配 TODO 目前没用
    public boolean canRedistribution;
    // 内部使用 运单的索引 与运单原始顺序对应 用于打乱运单之后关联原索引
    int index;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCanRedistribution() {
        return canRedistribution;
    }

    public void setCanRedistribution(boolean canRedistribution) {
        this.canRedistribution = canRedistribution;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Point getSender() {
        return sender;
    }

    public void setSender(Point sender) {
        this.sender = sender;
        sender.type = Point.TYPE_SENDER;
    }

    public Point getReceiver() {
        return receiver;
    }

    public void setReceiver(Point receiver) {
        this.receiver = receiver;
        receiver.type = Point.TYPE_RECEIVER;
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "id='" + id + '\'' +
                '}';
    }
}
