package com.mrwind.uds;

/**
 * 运单
 */
public class Shipment {
    // 重量
    public int weight;
    // 发货点
    public Point sender;
    // 收货点 receiver.dependency == sender receiver.endTime > sender.endTime
    public Point receiver;

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
    }

    public Point getReceiver() {
        return receiver;
    }

    public void setReceiver(Point receiver) {
        this.receiver = receiver;
    }
}
