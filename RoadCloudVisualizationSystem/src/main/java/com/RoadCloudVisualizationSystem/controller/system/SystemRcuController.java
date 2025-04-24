package com.RoadCloudVisualizationSystem.controller.system;

import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import com.RoadCloudVisualizationSystem.service.ObjService;
import com.RoadCloudVisualizationSystem.service.RcuObjsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/rcu")
public class SystemRcuController {
    @Autowired
    private RcuObjsService rcuObjsService;

    @Autowired
    private ObjService objService;

    // 分页查询rcu设备
    @Log(title = "分页查询rcu设备列表", businessType = BusinessType.QUERY)
    @GetMapping("/list")
    public Map<String, Object> getRcuList(@RequestParam(required = false) String rcuId,
                                           @RequestParam Integer pageNum,
                                           @RequestParam Integer pageSize) {
        if (rcuId == null)
            rcuId = "%";
        return rcuObjsService.getRcuObjsPageByRcuId(rcuId, pageNum, pageSize);
    }

    // 根据时间范围和rcuid分页查询发送来的信息
    @Log(title = "根据id和时间范围分页查询rcu发来的消息列表", businessType = BusinessType.QUERY)
    @GetMapping("/info_list")
    public Map<String, Object> getRcuObjsPageByRcuIdAndTime(@RequestParam String rcuId,
                                                            @RequestParam String startTime,
                                                            @RequestParam String endTime,
                                                            @RequestParam Integer pageNum,
                                                            @RequestParam Integer pageSize) {
        return rcuObjsService.getRcuObjsPageByRcuIdAndTime(rcuId, startTime, endTime, pageNum, pageSize);
    }

    // 根据objsflag分页查询对象的信息
    @Log(title = "根据objsflag分页查询rcu发来的某条消息的对象列表", businessType = BusinessType.QUERY)
    @GetMapping("/obj_list")
    public Map<String, Object> getObjsPageByObjsflag(@RequestParam String objsflag,
                                                     @RequestParam Integer pageNum,
                                                     @RequestParam Integer pageSize) {
        return rcuObjsService.getObjsPageByObjsflag(objsflag, pageNum, pageSize);
    }

    // 删除设备以及其所有相关的消息
    @Log(title = "根据rcuid删除某个设备以及其所有信息", businessType = BusinessType.DELETE)
    @DeleteMapping
    public Map<String, Object> deleteRcuByRcuId(@RequestParam String rcuId) {
        return rcuObjsService.deleteRcuByRcuId(rcuId);
    }

    @Log(title = "根据objsflag删除某个rcu的某个消息", businessType = BusinessType.DELETE)
    @DeleteMapping("/objs")
    public Map<String, Object> deleteObjsByObjsflag(@RequestParam String objsflag) {
        return rcuObjsService.deleteObjsByObjsflag(objsflag);
    }

    @Log(title = "根据objsflag和objId删除某个rcu的某个消息的某个对象", businessType = BusinessType.DELETE)
    @DeleteMapping("/obj")
    public Map<String, Object> deleteObjByObjsflagAndUuid(@RequestParam String objsflag,
                                                          @RequestParam String objId) {
        return rcuObjsService.deleteObjByObjsflagAndUuid(objsflag, objId);
    }

    // 根据RcuId导出数据
    @Log(title = "根据rcuId和时间范围导出数据", businessType = BusinessType.OTHER)
    @GetMapping("/exportByRcuId")//导出数据
    public Map<String,Object> exportByRcuId(@RequestParam String rcuIds, @RequestParam String startTime, @RequestParam String endTime){
        return objService.exportRcuAndObjs(rcuIds, startTime, endTime);
    }

    // 根据Objsflag导出数据
    @Log(title = "根据objsflags和时间范围导出某个rcu消息数据", businessType = BusinessType.OTHER)
    @GetMapping("/exportByobjsflag")//导出数据
    public Map<String,Object> exportByobjsflag(@RequestParam String objsflags, @RequestParam String startTime, @RequestParam String endTime){
        return objService.exportObjs(objsflags, startTime, endTime);
    }

}
