package com.RoadCloudVisualizationSystem.controller.system;

import com.RoadCloudVisualizationSystem.entity.Parameter;
import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import com.RoadCloudVisualizationSystem.service.ParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/system/parameter")
public class SystemParameterController {

    @Autowired
    private ParameterService systemParameterService;

    // 分页查询参数列表
    @Log(title = "分页查询参数列表", businessType = BusinessType.QUERY)
    @GetMapping("/list")
    public Map<String, Object> getParameterListPage(@RequestParam(required = false) String group,
                                                    @RequestParam(defaultValue="1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        if (group == null)
            group = "%";
        return systemParameterService.getParameterListPageByGroup(group, pageNum, pageSize);
    }

    // 新增参数
    @Log(title = "插入参数", businessType = BusinessType.INSERT)
    @PostMapping
    public Map<String, Object> insertParameter(@RequestBody Parameter parameter) {
        return systemParameterService.insertParameter(parameter);
    }

    // 更新参数
    @Log(title = "更新参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public Map<String, Object> updateParameter(@RequestBody Parameter parameter) {
        return systemParameterService.updateParameter(parameter);
    }

    // 删除参数
    @Log(title = "删除参数", businessType = BusinessType.DELETE)
    @DeleteMapping
    public Map<String, Object> deleteParameter(@RequestParam Integer id) {
        return systemParameterService.deleteParameter(id);
    }
}
