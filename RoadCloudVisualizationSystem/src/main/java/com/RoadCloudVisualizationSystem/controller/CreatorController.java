package com.RoadCloudVisualizationSystem.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.service.CreatorService;


@RestController
@RequestMapping("/system/creat")
public class CreatorController {

    @Autowired
    private CreatorService   creatorService;
    
    @GetMapping("/list")
    public Map<String, Object> getCreatorListPage(@RequestParam(defaultValue="1") Integer pageNum, @RequestParam(defaultValue = "922337203685477580") Integer pageSize) {
        return creatorService.getCreatorListPage(pageNum, pageSize);
    }
    

    @GetMapping("/export")
    public Map<String, Object> getCreatorListPageByTime(@RequestParam(defaultValue="1") String starttime, @RequestParam(defaultValue = "922337203685477580") String endtime) {
        return creatorService.getCreatorListPageByTime(starttime, endtime);
    }


    @GetMapping("/exportglb")
    public Map<String, Object> exportglb(@RequestParam(defaultValue="1") String starttime, @RequestParam(defaultValue = "922337203685477580") String endtime) {
        return creatorService.exportglb(starttime, endtime);
    }

}
