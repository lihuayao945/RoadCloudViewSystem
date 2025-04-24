package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.entity.vo.MessageExportVO;
import com.RoadCloudVisualizationSystem.entity.vo.UserExportVO;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.utils.UserInfoUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Message;
import com.RoadCloudVisualizationSystem.service.MessageService;
import com.RoadCloudVisualizationSystem.mapper.MessageMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【message(消息)】的数据库操作Service实现
* @createDate 2025-04-09 13:11:11
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserInfoUtil userInfoUtil;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public Map<String, Object> findByTimeAndTopic(String topic, String startTime, String endTime, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            if (topic != null || !topic.equals("")){
                List<Message> messageList = messageMapper.findByTimeAndTopic(topic, startTime, endTime, pageSize, pageSize * (pageNum - 1));
                Integer total = messageMapper.countTotal(topic, startTime, endTime);
                result.put("status", "success");
                result.put("total", total);
                result.put("rows", messageList);
                return result;
            }
            else {
                List<Message> messageList = messageMapper.findByTime(startTime, endTime, pageSize, pageSize * (pageNum - 1));
                Integer total = messageMapper.countTotalWithoutTopic(startTime, endTime);
                System.out.println(total);
                result.put("status", "success");
                result.put("total", total);
                result.put("rows", messageList);
                return result;
            }
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Integer insertMessage(Message message) {
        return messageMapper.insertMessage(message);
    }

    @Override
    public Map<String, Object> exportMessages(String topic, String startTime, String endTime) {
        List<Map<String, Object>> messages;
        Map<String, Object> result = new HashMap<>();
        if (topic != null && !topic.equals("")){
            messages = messageMapper.findAllByTimeAndTopic(topic, startTime, endTime);
        }
        else{
            messages = messageMapper.findAllByTime(startTime, endTime);
        }
        if (messages != null && !messages.isEmpty()) {
            // 获取当前时间戳
            long timestamp = System.currentTimeMillis();
            // 文件后缀
            String username = userInfoUtil.getCurrentUsername();
            String fileSuffix = "message_" + username + timestamp + ".xlsx";
            String filename = exportFilePath + fileSuffix;
            try {
                ExcelUtils.exportToExcel(messages, filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.put("status", "success");
            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
        } else {
            result.put("status", "fail");
        }
        return result;
    }
}




