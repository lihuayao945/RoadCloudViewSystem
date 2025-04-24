package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.RoadCloudVisualizationSystem.mapper.ObjMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.RcuObjs;
import com.RoadCloudVisualizationSystem.service.RcuObjsService;
import com.RoadCloudVisualizationSystem.mapper.RcuObjsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【rcu_objs(RCU感知信息表)】的数据库操作Service实现
* @createDate 2025-04-08 10:28:35
*/
@Service
public class RcuObjsServiceImpl extends ServiceImpl<RcuObjsMapper, RcuObjs>
    implements RcuObjsService{

    @Autowired
    private RcuObjsMapper rcuObjsMapper;

    @Autowired
    private ObjMapper objMapper;

    // 获取所有rsu设备信息
    @Override
    public Map<String, Object> getRcuObjsPageByRcuId(String rcuId, Integer pageNum, Integer pageSize) {
        Map<String, Object> map = new HashMap<>();
        try{
            List<RcuObjs> rcuObjsList = rcuObjsMapper.getRcuObjsPageByRcuId(rcuId, pageSize, pageSize * (pageNum - 1));
            Integer total = rcuObjsMapper.countRcu(rcuId);
            map.put("status", "success");
            map.put("total", total);
            map.put("rows", rcuObjsList);
            return map;
        }
        catch (Exception e){
            System.out.println(e);
            map.put("status", "fail");
            return map;
        }
    }

    //  根据rcuid和时间范围分页查询信息列表
    @Override
    public Map<String, Object> getRcuObjsPageByRcuIdAndTime(String rcuId, String startTime, String endTime, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            List<RcuObjs> rcuObjsList = rcuObjsMapper.getRcuObjsPageByRcuIdAndTime(rcuId, startTime, endTime, pageSize, pageSize * (pageNum - 1));
            Integer total = rcuObjsMapper.countRcuObjsByRcuIdAndTime(rcuId, startTime, endTime);
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", rcuObjsList);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    //  根据objsflag查询对象信息列表
    @Override
    public Map<String, Object> getObjsPageByObjsflag(String objsflag, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            List<Obj> objList = rcuObjsMapper.getObjsPageByObjsflag(objsflag, pageSize, pageSize * (pageNum - 1));
            Integer total = rcuObjsMapper.countObjsByObjsflag(objsflag);
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", objList);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    // 删除rcu设备以及其所有的消息和对象
    @Override
    @Transactional
    public Map<String, Object> deleteRcuByRcuId(String rcuId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rcuObjsMapper.deleteRcuByRcuId(rcuId);
            objMapper.deleteObjsByRcuId(rcuId);
            result.put("status", "success");
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Map<String, Object> deleteObjsByObjsflag(String objsflag) {
        Map<String, Object> result = new HashMap<>();
        try {
            objMapper.deleteObjsByObjsflag(objsflag);
            result.put("status", "success");
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Map<String, Object> deleteObjByObjsflagAndUuid(String objsflag, String objId) {
        Map<String, Object> result = new HashMap<>();
        try {
            objMapper.deleteObjByObjsflagAndObjId(objsflag, objId);
            result.put("status", "success");
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

}




