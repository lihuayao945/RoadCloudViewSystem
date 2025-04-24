package com.RoadCloudVisualizationSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.xlsxssavepath}")
    private String xlsxssavepath;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // xlsx文件路径映射
        // registry.addResourceHandler("/xlsxs/**.xlsx").addResourceLocations("file:" + xlsxssavepath);

        //exports文件路径映射
        registry.addResourceHandler("/exports/**.xlsx").addResourceLocations("file:" + exportFilePath);
        registry.addResourceHandler("/exports/**.csv").addResourceLocations("file:" + exportFilePath);
    }
}
