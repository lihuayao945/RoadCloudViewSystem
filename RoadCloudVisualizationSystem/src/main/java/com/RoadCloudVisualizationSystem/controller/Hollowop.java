package com.RoadCloudVisualizationSystem.controller;

import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.RoadCloudVisualizationSystem.utils.CoordinateUtil;
import com.RoadCloudVisualizationSystem.utils.RcuparseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;


@RestController
public class Hollowop {
	@Autowired
	private RcuparseUtil rcuparseUtil;

	@Autowired
	private CoordinateUtil coordinateUtil;

	@GetMapping("/hello")
	public String getMethodName() {
		System.out.println(Arrays.toString(coordinateUtil.convertCoordinateLocal("106.3169045", "29.5203387")));
		return "你好世界！";
	}

	// 测试插入rcu设备
	@PostMapping("/insertRcu")
	public Rcu insertRcu(@RequestBody String rcuJson) {
		try {
			Rcu rcu = rcuparseUtil.extractRcuFromJson(rcuJson);
			rcu.setReceiveTime(System.currentTimeMillis() + "");
			rcuparseUtil.insertRcu(rcu);
			return rcu;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
