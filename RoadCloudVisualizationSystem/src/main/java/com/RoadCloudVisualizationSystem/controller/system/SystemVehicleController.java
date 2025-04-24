package com.RoadCloudVisualizationSystem.controller.system;

import java.util.Map;

import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.RoadCloudVisualizationSystem.service.VehicleService;
@RestController
@RequestMapping("/system/vehicle")
public class SystemVehicleController {
    @Autowired
    private VehicleService vehicleService;

    @Log(title = "分页查询所有vehicle的所有数据", businessType = BusinessType.QUERY)
    @GetMapping("/list")//查询车辆(所有车辆)数据
    public Map<String, Object> getVehiclesPage( @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return vehicleService.getVehiclesPage(pageNum, pageSize);
    }

    @Log(title = "根据id分页查询vehicle的所有数据", businessType = BusinessType.QUERY)
    @GetMapping("/list/vehicleId")//根据vehicleId模糊查询车辆数据
    public Map<String, Object> getVehiclePageByvehicleId(@RequestParam String vehicleId,@RequestParam(defaultValue = "1") Integer pageNum,@RequestParam(defaultValue = "10") Integer pageSize) {
        return vehicleService.getVehiclePageByvehicleId(vehicleId,pageNum,pageSize);
    }

    //获取车辆管理界面所需数据(vehicleId、position数量、数据条数)
    @Log(title = "分页查询所有vehicle的最新一条数据", businessType = BusinessType.QUERY)
    @GetMapping("/manage")
    public Map<String,Object> getManagevehiclePage(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize){
        return vehicleService.getManagevehiclePage(pageNum,pageSize);
    }
    //根据stateId删除车辆
    @Log(title = "根据stateId删除某一条vehicle数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete/stateId")
    public Map<String, Object> deleteVehicleByStateId(@RequestParam String stateId) {
        return vehicleService.deleteVehicleByStateId(stateId);
    }

    //根据vehicleId删除车辆(多条数据请慎重)
    @Log(title = "根据vehicleId删除某一个vehicle的所有数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete/vehicleId")
    public Map<String,Object> deleteVehicleByvehicleId(@RequestParam String vehicleId){
        return vehicleService.deleteVehicleByvehicleId(vehicleId);
    }

    @Log(title = "根据vehicleIds和时间范围导出多个车辆的数据", businessType = BusinessType.OTHER)
    @GetMapping("/export")//导出数据
    public Map<String,Object> exportVehicle(@RequestParam(defaultValue = "0") String starttime, @RequestParam(defaultValue = "9223372036854775806") String endtime, @RequestParam String savedata){
        return vehicleService.exportVehicle(starttime, endtime, savedata);
    }


    //根据vehicleId和时间查询车辆数据
    @Log(title = "根据vehicleIds和时间范围分页查询该车的数据", businessType = BusinessType.QUERY)
    @GetMapping("/list/vehicleIdAndTime")
    public Map<String, Object> getVehiclePageByvehicleIdAndTime(@RequestParam String vehicleId,@RequestParam(defaultValue = "0") String starttime ,@RequestParam(defaultValue = "9223372036854775806") String endtime,@RequestParam(defaultValue = "1") Integer pageNum,@RequestParam(defaultValue = "10") Integer pageSize) {
        return vehicleService.getVehiclePageByvehicleIdAndTime(vehicleId,starttime,endtime,pageNum,pageSize);
    }

    

}
