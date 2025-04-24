package com.RoadCloudVisualizationSystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@MapperScan("com.RoadCloudVisualizationSystem.mapper")
@EnableIntegration
@EnableCaching
public class RoadCloudVisualizationSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoadCloudVisualizationSystemApplication.class, args);
		System.out.print("启动完成！");
	}

}
