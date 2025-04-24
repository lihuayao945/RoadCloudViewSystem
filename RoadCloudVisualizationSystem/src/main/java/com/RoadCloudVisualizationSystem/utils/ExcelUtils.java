package com.RoadCloudVisualizationSystem.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出工具类
 * 提供将数据导出为 Excel 文件的功能
 */
public class ExcelUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 将数据导出为 Excel 文件
     * @param data     Java List<Map> 数据
     * @param filePath 文件路径
     * @throws Exception 异常信息
     */
    public static void exportToExcel(List<Map<String, Object>> data, String filePath) throws Exception {
        // 创建目录路径
        createDirectories(filePath);

        // 转换为 JSON 字符串并包裹为 {"data": [...]}
        String jsonString = mapper.writeValueAsString(Map.of("data", data));
        JsonNode rootNode = mapper.readTree(jsonString);
        JsonNode dataArray = rootNode.get("data");

        if (dataArray == null || !dataArray.isArray()) {
            throw new IllegalArgumentException("JSON中未包含有效的data数组");
        }

        // 使用 try-with-resources 自动关闭资源
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            // 写入表头
            Row headerRow = sheet.createRow(0);
            Iterator<String> fieldNames = dataArray.get(0).fieldNames();
            int headerColIndex = 0;
            while (fieldNames.hasNext()) {
                headerRow.createCell(headerColIndex++).setCellValue(fieldNames.next());
            }

            // 写入数据
            for (int i = 0; i < dataArray.size(); i++) {
                JsonNode item = dataArray.get(i);
                Row row = sheet.createRow(i + 1);
                int colIndex = 0;
                for (Iterator<String> it = item.fieldNames(); it.hasNext(); ) {
                    String field = it.next();
                    row.createCell(colIndex++).setCellValue(item.get(field).asText());
                }
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 创建文件路径中的目录（如果不存在的话）
     * @param filePath 文件路径
     */
    private static void createDirectories(String filePath) {
        File file = new File(filePath);
        // 获取文件的父目录
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            // 创建父目录
            if (!parentDir.mkdirs()) {
                throw new IllegalStateException("无法创建目录：" + parentDir.getAbsolutePath());
            }
        }
    }

    public static void exportToCsv(List<Map<String,Object>> data, String filePath) {
        createDirectories(filePath);
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据列表为空或无效");
        }
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            // 写入表头
            Map<String, Object> firstRow = data.get(0);
            boolean isFirstColumn = true;
            for (String header : firstRow.keySet()) {
                if (!isFirstColumn) {
                    writer.write(",");
                }
                writer.write("\"" + header + "\"");
                isFirstColumn = false;
            }
            writer.newLine();
            
            // 写入数据行
            for (Map<String, Object> row : data) {
                isFirstColumn = true;
                for (Object value : row.values()) {
                    if (!isFirstColumn) {
                        writer.write(",");
                    }
                    String cellValue = value != null ? value.toString() : "";
                    // 如果值包含逗号、引号或换行符，需要用引号包围并处理内部引号
                    if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                        cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                    }
                    writer.write(cellValue);
                    isFirstColumn = false;
                }
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("导出CSV文件失败: " + e.getMessage(), e);
        }
    }
}
