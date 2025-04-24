package com.RoadCloudVisualizationSystem.entity;

import java.util.LinkedList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {
    //原始数据
    private String stringdata;
    //路灯
    private Frsu frsu;
    private Intersection intersection;
    private LinkedList<Phase> phase;
    private LinkedList<Phasestate> phasestate;
    //rcu
    private RcuObjs rcuObjs;
    private LinkedList<Obj> obj;
    //车辆
    private Vehicle vehicle;
}
