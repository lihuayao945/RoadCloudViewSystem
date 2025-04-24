package com.RoadCloudVisualizationSystem.service;

import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.SysOperLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【sys_oper_log(操作日志记录)】的数据库操作Service
* @createDate 2025-04-10 21:54:17
*/
public interface SysOperLogService extends IService<SysOperLog> {


    public Map<String, Object> getLogListPage(Integer pageNum, Integer pageSize) ;

    public int deleteById(String id);

    public Map<String, Object> exportLog(String starttime, String endtime);

    // 清空日志
    Map<String, Object> cleanLog();

    // 根据status，bussnessType，title，beginTime和endTime分页查询日志
    Map<String, Object> getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTime(Integer status,
                                                                                            String bussnessType,
                                                                                            String beginTime,
                                                                                            String endTime,
                                                                                            Integer pageNum,
                                                                                            Integer pageSize);

}
