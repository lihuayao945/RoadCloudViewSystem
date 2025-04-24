package com.RoadCloudVisualizationSystem.utils;

import java.io.*;
import java.util.*;
import org.locationtech.proj4j.*;

/**
 * 地理坐标到高斯-克吕格坐标转换器
 * 将地理坐标(经度,纬度,高程)转换为场景坐标(x,y,z)
 * 高程单位为分米(dm)，角度单位为度，输出角度单位为弧度
 */
public class GaussKrugerConverter {
    
    // 单位转换常量
    private static final double DM_TO_M = 0.1;  // 分米转米：1dm = 0.1m
    private static final double DEG_TO_RAD = Math.PI / 180.0; // 角度转弧度
    
    /**
     * 将地理坐标转换为高斯-克吕格坐标
     * 输入参数为标准度数格式，高程单位为分米(dm)
     */
    public static double[] convertGeoToGK(double lat, double lon, double heightDm, double centralMeridian) 
        throws Exception {
        // 创建转换器
        CRSFactory crsFactory = new CRSFactory();
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        
        CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326");
        String gkParams = "+proj=tmerc +lat_0=0 +lon_0=" + centralMeridian + 
                         " +k=1 +x_0=500000 +y_0=0 +ellps=GRS80 +units=m +no_defs";
        CoordinateReferenceSystem gk = crsFactory.createFromParameters("GK", gkParams);
        CoordinateTransform transform = ctFactory.createTransform(wgs84, gk);
        
        // 执行转换
        ProjCoordinate result = new ProjCoordinate();
        transform.transform(new ProjCoordinate(lon, lat), result);
        
        // 将高程从分米转换为米
        double heightMeters = heightDm * DM_TO_M;
        
        return new double[]{result.x, result.y, heightMeters};
    }
    
    /**
     * 将高斯-克吕格坐标转换为场景坐标
     * 输入参数为标准度数格式，高程已转换为米
     */
    public static double[] convertGKToScene(double x, double y, double z, 
                                          double intersectionX, double intersectionY, double intersectionZ,
                                          double headingDegrees, double sceneScaleFactor) {
        // 计算相对于路口中心的偏移
        double dx = x - intersectionX;
        double dy = y - intersectionY;
        double dz = z - intersectionZ;
        
        // 计算方位角（相对于北方，顺时针为正）
        double bearing = Math.atan2(dx, dy); // 已经是弧度
        
        // 转换为场景坐标
        double xScene = dx * sceneScaleFactor;
        double yScene = dy * sceneScaleFactor;
        double zScene = dz * sceneScaleFactor;
        
        // 将航向角从角度转换为弧度
        double headingRadians = headingDegrees * DEG_TO_RAD;
        
        return new double[]{xScene, yScene, zScene, headingRadians};
    }
    
    /**
     * 将地理坐标直接转换为场景坐标
     * 输入参数为标准度数格式，高程单位为分米(dm)
     */
    public static double[] geoToSceneCoords(double lon, double lat, double heightDm, 
                                          double headingDegrees, double sceneScaleFactor,
                                          double centralMeridian) throws Exception {
        // 转换为高斯-克吕格坐标
        double[] gkCoords = convertGeoToGK(lat, lon, heightDm, centralMeridian);
        
        // 转换为场景坐标（以原点为参考点）
        return convertGKToScene(gkCoords[0], gkCoords[1], gkCoords[2], 
                              0, 0, 0, 
                              headingDegrees, sceneScaleFactor);
    }
    
    /**
     * 处理输入文件并生成输出文件
     * 输入文件中的经纬度为标准度数格式，高程单位为分米(dm)，角度为标准度数
     */
    public static void processFile(String inputFile, String outputFile, 
                                 double intersectionLat, double intersectionLon, double intersectionHeightDm,
                                 double sceneScaleFactor, double centralMeridian) throws Exception {
        System.out.println("处理文件参数：");
        System.out.println("路口纬度：" + intersectionLat + "°");
        System.out.println("路口经度：" + intersectionLon + "°");
        System.out.println("路口高程：" + intersectionHeightDm + "dm");
        System.out.println("缩放因子：" + sceneScaleFactor);
        System.out.println("中央经线：" + centralMeridian + "°");
        
        // 读取输入文件
        List<Map<String, Object>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String header = reader.readLine();
            if (header == null) {
                throw new Exception("输入文件为空或格式错误");
            }
            
            System.out.println("文件头：" + header);
            String[] headers = header.split(",");
            
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                System.out.println("读取第" + lineCount + "行：" + line);
                
                String[] values = line.split(",");
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String headerName = headers[i].trim();
                    String value = values[i].trim();
                    
                    // 根据字段类型进行解析
                    if (headerName.equals("times") || headerName.equals("series") || 
                        headerName.equals("type") || headerName.equals("obj_id") || 
                        headerName.equals("color")) {
                        // 字符串类型
                        row.put(headerName, value);
                    } else {
                        // 数值类型
                        try {
                            row.put(headerName, Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            System.err.println("警告：无法解析值: " + value + " 在列: " + headerName);
                        }
                    }
                }
                if (!row.isEmpty()) {
                    data.add(row);
                    System.out.println("解析数据：" + row);
                }
            }
        }
        
        System.out.println("读取了" + data.size() + "行数据");
        
        // 转换路口中心点坐标
        System.out.println("计算路口中心点的场景坐标...");
        double[] intersectionCoords = geoToSceneCoords(intersectionLon, intersectionLat, 
                                                    intersectionHeightDm, 0.0, 
                                                    sceneScaleFactor, centralMeridian);
        System.out.println("路口中心点场景坐标：" + Arrays.toString(intersectionCoords));
        
        // 准备输出
        List<String> outputLines = new ArrayList<>();
        outputLines.add("times,series,type,obj_id,color,x,y,z,angle");
        
        // 转换所有点的坐标
        System.out.println("开始转换各点坐标...");
        for (Map<String, Object> row : data) {
            // 获取字符串字段
            String times = (String) row.getOrDefault("times", "");
            String series = (String) row.getOrDefault("series", "");
            String type = (String) row.getOrDefault("type", "");
            String objId = (String) row.getOrDefault("obj_id", "");
            String color = (String) row.getOrDefault("color", "");
            
            // 获取数值字段
            double lon = 0.0;
            double lat = 0.0;
            double heightDm = 0.0;
            double heading = 0.0;

            // 安全地获取数值字段
            Object lonObj = row.get("longitude");
            Object latObj = row.get("latitude");
            Object heightObj = row.get("elevation");
            Object headingObj = row.get("heading");

            if (lonObj instanceof Number) {
                lon = ((Number) lonObj).doubleValue();
            } else if (lonObj instanceof String) {
                try {
                    lon = Double.parseDouble((String) lonObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析经度值: " + lonObj);
                }
            }

            if (latObj instanceof Number) {
                lat = ((Number) latObj).doubleValue();
            } else if (latObj instanceof String) {
                try {
                    lat = Double.parseDouble((String) latObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析纬度值: " + latObj);
                }
            }

            if (heightObj instanceof Number) {
                heightDm = ((Number) heightObj).doubleValue();
            } else if (heightObj instanceof String) {
                try {
                    heightDm = Double.parseDouble((String) heightObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析高程值: " + heightObj);
                }
            }

            if (headingObj instanceof Number) {
                heading = ((Number) headingObj).doubleValue();
            } else if (headingObj instanceof String) {
                try {
                    heading = Double.parseDouble((String) headingObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析航向值: " + headingObj);
                }
            }
            
            System.out.println("转换点(" + lon + "°, " + lat + "°, " + heightDm + "dm, " + heading + "°)");
            double[] sceneCoords = geoToSceneCoords(lon, lat, heightDm, heading, 
                                                  sceneScaleFactor, centralMeridian);
            System.out.println("转换结果：" + Arrays.toString(sceneCoords));
            
            // 计算相对于路口中心的偏移
            double x = sceneCoords[0] - intersectionCoords[0];
            double y = sceneCoords[1] - intersectionCoords[1];
            double z = sceneCoords[2] - intersectionCoords[2];
            double angle = sceneCoords[3]; // 已经是弧度
            
            String outputLine = String.format("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.6f",
                                            times, series, type, objId, color, x, y, z, angle);
            System.out.println("输出行：" + outputLine);
            outputLines.add(outputLine);
        }
        
        // 写入输出文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : outputLines) {
                writer.write(line);
                writer.newLine();
            }
        }
        
        System.out.println("处理完成，结果已保存到 " + outputFile);
    }
    
    /**
     * 处理输入列表并生成输出列表
     * 输入列表中的经纬度为标准度数格式，高程单位为分米(dm)，角度为标准度数
     * @param inputData 输入数据列表，每个元素是一个Map，包含times,series,type,obj_id,color,longitude,latitude,elevation,heading字段
     * @param intersectionLat 路口中心纬度
     * @param intersectionLon 路口中心经度
     * @param intersectionHeightDm 路口中心高程(dm)
     * @param sceneScaleFactor 场景缩放因子
     * @param centralMeridian 中央经线
     * @return 转换后的数据列表，每个元素是一个Map，包含times,series,type,obj_id,color,x,y,z,angle字段
     */
    public static List<Map<String, Object>> processList(List<Map<String, Object>> inputData,
                                                      double intersectionLat, double intersectionLon, 
                                                      double intersectionHeightDm,
                                                      double sceneScaleFactor, double centralMeridian) 
        throws Exception {
        System.out.println("处理列表参数：");
        System.out.println("路口纬度：" + intersectionLat + "°");
        System.out.println("路口经度：" + intersectionLon + "°");
        System.out.println("路口高程：" + intersectionHeightDm + "dm");
        System.out.println("缩放因子：" + sceneScaleFactor);
        System.out.println("中央经线：" + centralMeridian + "°");
        System.out.println("输入数据条数：" + inputData.size());
        
        // 转换路口中心点坐标
        System.out.println("计算路口中心点的场景坐标...");
        double[] intersectionCoords = geoToSceneCoords(intersectionLon, intersectionLat, 
                                                    intersectionHeightDm, 0.0, 
                                                    sceneScaleFactor, centralMeridian);
        System.out.println("路口中心点场景坐标：" + Arrays.toString(intersectionCoords));
        
        // 准备输出
        List<Map<String, Object>> outputData = new ArrayList<>();
        
        // 转换所有点的坐标
        System.out.println("开始转换各点坐标...");
        for (Map<String, Object> row : inputData) {
            // 获取字符串字段
            String times = (String) row.getOrDefault("times", "");
            String series = (String) row.getOrDefault("series", "");
            String type = (String) row.getOrDefault("type", "");
            String objId = (String) row.getOrDefault("obj_id", "");
            String color = (String) row.getOrDefault("color", "");
            
            // 获取数值字段
            double lon = 0.0;
            double lat = 0.0;
            double heightDm = 0.0;
            double heading = 0.0;

            // 安全地获取数值字段
            Object lonObj = row.get("longitude");
            Object latObj = row.get("latitude");
            Object heightObj = row.get("elevation");
            Object headingObj = row.get("heading");

            if (lonObj instanceof Number) {
                lon = ((Number) lonObj).doubleValue();
            } else if (lonObj instanceof String) {
                try {
                    lon = Double.parseDouble((String) lonObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析经度值: " + lonObj);
                }
            }

            if (latObj instanceof Number) {
                lat = ((Number) latObj).doubleValue();
            } else if (latObj instanceof String) {
                try {
                    lat = Double.parseDouble((String) latObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析纬度值: " + latObj);
                }
            }

            if (heightObj instanceof Number) {
                heightDm = ((Number) heightObj).doubleValue();
            } else if (heightObj instanceof String) {
                try {
                    heightDm = Double.parseDouble((String) heightObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析高程值: " + heightObj);
                }
            }

            if (headingObj instanceof Number) {
                heading = ((Number) headingObj).doubleValue();
            } else if (headingObj instanceof String) {
                try {
                    heading = Double.parseDouble((String) headingObj);
                } catch (NumberFormatException e) {
                    System.err.println("警告：无法解析航向值: " + headingObj);
                }
            }
            
            System.out.println("转换点(" + lon + "°, " + lat + "°, " + heightDm + "dm, " + heading + "°)");
            double[] sceneCoords = geoToSceneCoords(lon, lat, heightDm, heading, 
                                                  sceneScaleFactor, centralMeridian);
            System.out.println("转换结果：" + Arrays.toString(sceneCoords));
            
            // 计算相对于路口中心的偏移
            double x = sceneCoords[0] - intersectionCoords[0];
            double y = sceneCoords[1] - intersectionCoords[1];
            double z = sceneCoords[2] - intersectionCoords[2];
            double angle = sceneCoords[3]; // 已经是弧度
            
            // 创建输出行
            Map<String, Object> outputRow = new HashMap<>();
            outputRow.put("times", times);
            outputRow.put("series", series);
            outputRow.put("type", type);
            outputRow.put("obj_id", objId);
            outputRow.put("color", color);
            outputRow.put("x", x);
            outputRow.put("y", y);
            outputRow.put("z", z);
            outputRow.put("angle", angle);
            
            System.out.println("输出行：" + outputRow);
            outputData.add(outputRow);
        }
        
        System.out.println("处理完成，共转换 " + outputData.size() + " 条数据");
        return outputData;
    }
    
    /**
     * 将输出列表保存到文件
     * @param outputData 输出数据列表
     * @param outputFile 输出文件路径
     */
    public static void saveToFile(List<Map<String, Object>> outputData, String outputFile) 
        throws IOException {
        // 准备输出
        List<String> outputLines = new ArrayList<>();
        outputLines.add("times,series,type,obj_id,color,x,y,z,angle");
        
        // 转换所有点的坐标
        for (Map<String, Object> row : outputData) {
            // 获取字符串字段
            String times = (String) row.getOrDefault("times", "");
            String series = (String) row.getOrDefault("series", "");
            String type = (String) row.getOrDefault("type", "");
            String objId = (String) row.getOrDefault("obj_id", "");
            String color = (String) row.getOrDefault("color", "");
            
            // 获取数值字段
            double x = ((Number) row.getOrDefault("x", 0.0)).doubleValue();
            double y = ((Number) row.getOrDefault("y", 0.0)).doubleValue();
            double z = ((Number) row.getOrDefault("z", 0.0)).doubleValue();
            double angle = ((Number) row.getOrDefault("angle", 0.0)).doubleValue();
            
            String outputLine = String.format("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.6f",
                                            times, series, type, objId, color, x, y, z, angle);
            outputLines.add(outputLine);
        }
        
        // 写入输出文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : outputLines) {
                writer.write(line);
                writer.newLine();
            }
        }
        
        System.out.println("结果已保存到 " + outputFile);
    }
    

    
} 