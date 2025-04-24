package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.SysOperLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.lettuce.core.dynamic.annotation.Param;

/**
* @author Administrator
* @description 针对表【sys_oper_log(操作日志记录)】的数据库操作Mapper
* @createDate 2025-04-10 21:54:17
* @Entity com.RoadCloudVisualizationSystem.entity.SysOperLog
*/
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    int getLogDataCount();
    List<Map<String, Object>> getLogListPage(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    int deleteById(@Param("id") Integer id);

    int getexportlogCount();
    List<Map<String, Object>> getexportlog(@Param("starttime") String starttime, @Param("endtime") String endtime);

    // 清空日志
    int cleanLog();

    // 根据status，bussnessType，title，beginTime和endTime分页查询日志
    List<SysOperLog> getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTime(@Param("status") Integer status,
                                                                                                  @Param("bussnessType") String bussnessType,
                                                                                                  @Param("beginTime") String beginTime,
                                                                                                  @Param("endTime") String endTime,
                                                                                                  @Param("limit") Integer limit,
                                                                                                  @Param("offset") Integer offset);
    // 根据status，bussnessType，title，beginTime和endTime计数
    int getLogListPageByStatusAndBussnessTypeAndTitleAndBeginTimeAndEndTimeCount(@Param("status") Integer status,
                                                                                              @Param("bussnessType") String bussnessType,
                                                                                              @Param("beginTime") String beginTime,
                                                                                              @Param("endTime") String endTime);

}




