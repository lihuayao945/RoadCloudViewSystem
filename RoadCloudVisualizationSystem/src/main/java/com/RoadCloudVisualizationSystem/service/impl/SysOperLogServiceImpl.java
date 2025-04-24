package com.RoadCloudVisualizationSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.SysOperLog;
import com.RoadCloudVisualizationSystem.service.SysOperLogService;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.utils.UserInfoUtil;
import com.RoadCloudVisualizationSystem.mapper.SysOperLogMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【sys_oper_log(操作日志记录)】的数据库操作Service实现
* @createDate 2025-04-10 21:54:17
*/
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>implements SysOperLogService{

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    @Autowired
    private UserInfoUtil userInfoUtil;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public Map<String, Object> getLogListPage(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        try{
            List<Map<String, Object>> logList = new ArrayList<>();
            logList = sysOperLogMapper.getLogListPage(pageSize, offset);
            result.put("status", "success");
            result.put("total", sysOperLogMapper.getLogDataCount());
            result.put("rows", logList);

        }catch (Exception e){
            result.put("status", "fail");
        }
        return result;
    }

    @Override
    public int deleteById(String id){
        return sysOperLogMapper.deleteById(Integer.parseInt(id));
    }

    @Override
    public Map<String, Object> exportLog(String starttime, String endtime){
        Map<String, Object>  result = new HashMap<>();
        List<Map<String, Object>> logList = sysOperLogMapper.getexportlog(starttime,endtime);
        if (logList.size() > 0) {
            // 获取当前时间戳
            long timestamp = System.currentTimeMillis();
            // 文件后缀
            String username = userInfoUtil.getCurrentUsername();
            String fileSuffix = "Log_" + username + timestamp + ".xlsx";
            String filename = exportFilePath + fileSuffix;
            try {
                ExcelUtils.exportToExcel(logList, filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.put("status", "success");
            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
        }
        else{
            result.put("status","fail");
        }
        return result;
    }

    @Override
    public Map<String, Object> cleanLog() {
        Map<String, Object> result = new HashMap<>();
        int cleanLog = sysOperLogMapper.cleanLog();
        if (cleanLog > 0) {
            result.put("status", "success");
        }
        else{
            result.put("status", "fail");
        }
        return result;
    }

    @Override
    public Map<String, Object> getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTime(Integer status, String bussnessType, String beginTime,
                                                                                                   String endTime, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            List<SysOperLog> logList = sysOperLogMapper.getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTime(status, bussnessType, beginTime, endTime, pageSize, (pageNum - 1) * pageSize);
            result.put("status", "success");
            result.put("total", sysOperLogMapper.getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTimeCount(status, bussnessType, beginTime, endTime));
            result.put("rows", logList);
        }
        catch (Exception e){
            result.put("status", "fail");
        }
        return result;
    }
}




