package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.*;
import com.RoadCloudVisualizationSystem.mapper.*;
import com.RoadCloudVisualizationSystem.service.DataService;
import com.RoadCloudVisualizationSystem.utils.CoordinateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataServiceImpl implements DataService {
    @Autowired
    private FrsuMapper frsuMapper;
    @Autowired
    private IntersectionMapper intersectionMapper;
    @Autowired
    private PhaseMapper phaseMapper;
    @Autowired
    private PhasestateMapper phasestateMapper;
    @Autowired
    private RcuObjsMapper rcuObjsMapper;
    @Autowired
    private ObjMapper objMapper;
    @Autowired
    private VehicleMapper vehicleMapper;
    @Autowired
    private CoordinateUtil coordinateUtil;

    @Override
    @Transactional
    public Data parseAndSave(String json) throws Exception {
        Data data = new Data();
        String stringdata = initialStringdata(json);
        data.setStringdata(stringdata);
        
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(stringdata);
        
        if (rootNode.has("position")) {
            //解析车辆
            data.setVehicle(Vehicle.fromJson(stringdata));
        } else if (rootNode.has("intersections")) {
            //解析路灯
            data.setFrsu(Frsu.fromJson(stringdata));
            data.setIntersection(Intersection.fromJson(stringdata));
            data.setPhase(Phase.fromJson(stringdata));
            data.setPhasestate(Phasestate.fromJson(stringdata));
        } else if (rootNode.has("objective")) {
            //解析rcu
            data.setRcuObjs(RcuObjs.fromJson(stringdata));
            data.setObj(Obj.fromJson(stringdata));
        } else {
            throw new Exception("无法识别的数据类型");
        }

        save(data);
        return data;
    }

    @Override
    @Transactional
    public void save(Data data) throws Exception {
        // 检查并保存Frsu数据
        if (data.getFrsu() != null) {
            try {
                frsuMapper.insert(data.getFrsu());
            } catch (Exception e) {
                throw new Exception("保存Frsu数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存Intersection数据
        if (data.getIntersection() != null) {
            try {
                intersectionMapper.insert(data.getIntersection());
            } catch (Exception e) {
                throw new Exception("保存Intersection数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存Phase数据
        if (data.getPhase() != null && !data.getPhase().isEmpty()) {
            try {
                for (Phase phase : data.getPhase()) {
                    phaseMapper.insert(phase);
                }
            } catch (Exception e) {
                throw new Exception("保存Phase数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存Phasestate数据
        if (data.getPhasestate() != null && !data.getPhasestate().isEmpty()) {
            try {
                for (Phasestate phasestate : data.getPhasestate()) {
                    phasestateMapper.insert(phasestate);
                }
            } catch (Exception e) {
                throw new Exception("保存Phasestate数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存RcuObjs数据
        if (data.getRcuObjs() != null) {
            try {
                if (rcuObjsMapper.getRcuObjsByObjsflag(data.getRcuObjs().getObjsflag()) != null) {
                    throw new Exception("RcuObjs数据已存在，无法保存");
                }
                rcuObjsMapper.insertRcuObjs(data.getRcuObjs());
            } catch (Exception e) {
                throw new Exception("保存RcuObjs数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存Obj数据
        if (data.getObj() != null && !data.getObj().isEmpty()) {
            try {
                for (Obj obj : data.getObj()) {
                    double[] coordinates = coordinateUtil.convertCoordinateLocal(obj.getLongitude(), obj.getLatitude());
                    obj.setLongitude(String.valueOf(coordinates[0]));
                    obj.setLatitude(String.valueOf(coordinates[1]));
                    objMapper.insertObj(obj);
                }
            } catch (Exception e) {
                throw new Exception("保存Obj数据失败: " + e.getMessage(), e);
            }
        }
        
        // 检查并保存Vehicle数据
        if (data.getVehicle() != null) {
            try {
                Vehicle existingVehicle = vehicleMapper.selectById(data.getVehicle().getStateId());
                if (existingVehicle != null) {
                    // 如果存在，则报错
                    throw new Exception("Vehicle数据已存在，无法保存");
                } else {
                    // 如果不存在，则插入
                    double[] coordinates = coordinateUtil.convertCoordinateLocal(data.getVehicle().getLongitude(), data.getVehicle().getLatitude());
                    data.getVehicle().setLongitude(String.valueOf(coordinates[0]));
                    data.getVehicle().setLatitude(String.valueOf(coordinates[1]));
                    vehicleMapper.insert(data.getVehicle());
                }
            } catch (Exception e) {
                throw new Exception("保存Vehicle数据失败: " + e.getMessage(), e);
            }
        }
    }

    private String initialStringdata(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        if (rootNode.has("body")) {
            return rootNode.get("body").toString();
        } else {
            return json;
        }
    }
} 