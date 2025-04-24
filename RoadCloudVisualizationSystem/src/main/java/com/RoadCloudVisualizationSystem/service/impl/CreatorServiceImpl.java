package com.RoadCloudVisualizationSystem.service.impl;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.RoadCloudVisualizationSystem.mapper.CreatorMapper;
import com.RoadCloudVisualizationSystem.service.CreatorService;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.utils.GaussKrugerConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class CreatorServiceImpl implements CreatorService {

    @Autowired
    private CreatorMapper creatorMapper;
    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public Map<String, Object> getCreatorListPage(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        List<Map<String,Object>> creatorslList = creatorMapper.getCreatorListPage(offset, pageSize);
        if (creatorslList.size() > 0) {
            result.put("status", "success");
            result.put("data", creatorslList);
        } else {
            result.put("status", "fail");
        }
        return result;
    }


    public Map<String, Object> getCreatorListPageByTime(String starttime, String endtime){
        Map<String, Object> result = new HashMap<>();
        List<Map<String,Object>> creatorslList = creatorMapper.getCreatorListPageByTime(starttime, endtime);
        if (creatorslList.size() > 0) { 
            result.put("status", "success");
            //result.put("data", creatorslList);
            long timestamp = System.currentTimeMillis();
            // 创建csv文件
            try {
                
                String filePath = exportFilePath + timestamp + ".csv";
                ExcelUtils.exportToCsv(creatorslList, filePath);
                result.put("filepath", "/exports/" + timestamp + ".csv");

            } catch (Exception e) {
                System.out.println("导出失败");
                e.printStackTrace();
                result.put("status", "fail");
            }
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    public Map<String, Object> exportglb(String starttime, String endtime){
        Map<String, Object> result = new HashMap<>();
        List<Map<String,Object>> creatorslList = creatorMapper.getCreatorListPageByTime(starttime, endtime);
        if (creatorslList.size() > 0) { 
            //result.put("status", "success");
            //result.put("data", creatorslList);
            long timestamp = System.currentTimeMillis();
            // 创建csv文件
            try {

                String filePath = exportFilePath + timestamp + ".csv";

                List<Map<String,Object>> resultList = GaussKrugerConverter.processList(creatorslList, 29.5099608, 106.4685861, 0.0, 0.5, 106.5);
                //result.put("filepath", "/exports/" + timestamp + ".csv");
                ExcelUtils.exportToCsv(resultList, filePath);
                System.out.println(filePath);

                //String uploadFilePath = "C:/Users/Yuer/Desktop/LoadView/Blender/data.csv";
                File file = new File(filePath);

                if (!file.exists()) {
                    result.put("status", "fail");
                    result.put("message", "文件不存在");
                    return result;
                }

                OkHttpClient client = new OkHttpClient.Builder()
                        .readTimeout(600, TimeUnit.SECONDS)  // 设置读取超时
                        .connectTimeout(600, TimeUnit.SECONDS)  // 可选：设置连接超时
                        .writeTimeout(600, TimeUnit.SECONDS)  // 可选：设置写入超时
                        .build(); // 如果是大文件下载，可能需要更长时间;

                RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.getName(),
                        RequestBody.create(
                            MediaType.parse("text/csv"),
                            file
                        )
                    )
                    .build();

                Request request = new Request.Builder()
                    .url("http://localhost:5147")
                    .post(body)
                    .build();

                System.out.println("iamhertyuer");
                try (Response response = client.newCall(request).execute()) {
                    //System.out.println(response.body().string());
                    if (!response.isSuccessful()) {
                        result.put("status", "fail");
                        result.put("message", "上传失败，状态码: " + response.code());
                        return result;
                    }

                    String responseBody = response.body().string();
                    // 将响应内容转换为Map<String, Object>
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                        // 将响应Map合并到结果Map中
                        result.putAll(responseMap);
                    } catch (Exception ex) {
                        result.put("status", "fail");
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                result.put("status", "fail");
            }
        } else {
            result.put("status", "fail");
        }
        return result;
    }




    
}
