package com.RoadCloudVisualizationSystem.controller.system;

import java.util.HashMap;
import java.util.Map;

import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.RoadCloudVisualizationSystem.service.SysOperLogService;

@RestController
@RequestMapping("/system/log")
public class SystemLogController {
    
    
    @Autowired
    private SysOperLogService  sysOperLogService;

//    @Log(title = "分页查询日志列表", businessType = BusinessType.QUERY)
    @GetMapping("/list")
    public Map<String, Object> getLogListPage(@RequestParam(defaultValue="1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false, defaultValue = "0") Integer status,
                                              @RequestParam(required = false, defaultValue = "QUERY") String businessType,
                                              @RequestParam(required = false, defaultValue = "1") String beginTime,
                                              @RequestParam(required = false, defaultValue = "1944906420371") String endTime) {

        return sysOperLogService.getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTime(status, businessType, beginTime, endTime, pageNum, pageSize);
    }


    @Log(title = "根据id删除日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete")
    public Map<String, Object> deleteById(@RequestParam String id){
        Map<String, Object> result = new HashMap<>();
        if (id == null || id.equals("")){
            result.put("status", "fail");
            result.put("msg", "id未空");
            return result;
        }
        if (sysOperLogService.deleteById(id) == 0){
            result.put("status", "fail");
            result.put("msg", "删除失败，id不存在");
            return result;
        }
        try{
            result.put("status", "success");
            result.put("msg", "删除成功");
            return result;
        }
        catch(Exception e){
            result.put("status", "fail");
            result.put("msg", "删除失败");
            return result;
        }
    }


    @Log(title = "根据时间范围导出日志", businessType = BusinessType.OTHER)
    @GetMapping("/export")
    public Map<String, Object> exportLog(@RequestParam(defaultValue = "1") String starttime,@RequestParam(defaultValue = "9223372036854775806") String endtime){
        return sysOperLogService.exportLog(starttime,endtime);
    }

    // 清空日志
    @Log(title = "清空日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public Map<String, Object> cleanLog(){
        return sysOperLogService.cleanLog();
    }
}
