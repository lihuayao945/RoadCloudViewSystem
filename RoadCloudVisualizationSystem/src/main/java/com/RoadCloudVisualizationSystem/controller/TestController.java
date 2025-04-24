package com.RoadCloudVisualizationSystem.controller;

import com.RoadCloudVisualizationSystem.entity.Data;
import com.RoadCloudVisualizationSystem.service.DataService;
import com.RoadCloudVisualizationSystem.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private DataService dataService;

    @GetMapping("/getdata")
    public Result getdata() {
        String json = """
                {
                  "msgType": "spat",
                  "rsuId": "F-CP2027",
                  "msgCnt": 112,
                  "intersections": [
                    {
                      "intersectionTimestamp": 1743592399252,
                      "regionId": 3,
                      "nodeId": 1,
                      "phases": [
                        {
                          "phaseId": 1,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 2,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 303.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 3,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 303.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 11,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 10,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 9,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 19,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 303.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 18,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 303.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 503.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 25,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 27,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 26,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 3.000000000001819,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        },
                        {
                          "phaseId": 25,
                          "phaseStates": [
                            {
                              "timeChangeDetails": 1,
                              "light": 3,
                              "likelyEndTime": 203.00000000000182,
                              "nextDuration": 5700,
                              "startTime": 0
                            },
                            {
                              "timeChangeDetails": 1,
                              "light": 5,
                              "likelyEndTime": 303.0000000000018,
                              "nextDuration": 5700,
                              "startTime": 0
                            }
                          ]
                        }
                      ],
                      "status": 4
                    }
                  ],
                  "uuid": "B0F1f676e3zU2BMg5bO4x53FW06ZJH57vBei",
                  "timestamp": 1743592399252
                }
                """;
        try {
            Data data = dataService.parseAndSave(json);
            return Result.success(data);
        } catch (Exception e) {
            return Result.fail("处理数据失败: " + e.getMessage());
        }
    }
}
