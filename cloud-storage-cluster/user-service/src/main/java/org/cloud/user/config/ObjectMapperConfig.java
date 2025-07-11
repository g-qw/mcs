package org.cloud.user.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // 注册 JavaTimeModule 模块
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 禁用将日期格式化为时间戳的功能

        /*
            避免将 JSON 对象反序列化为 Map 类型, 但是序列化的JSON中会包含类型信息
            如果需要在反序列化为原对象，则需要开启此配置
            "org.cloud.fs.model.MinioDirectory",
            {
                "directoryId": "8b486bf8-fd12-4f5b-b1bb-013765a3f298",
                "parentDirectoryId": "7ee4fecd-3d42-44b7-906b-adfaa3ab3880",
                "userId": "0ba5b0a3-6b23-43d5-81ed-34149b27e067",
                "name": "dir1",
                "createdAt": "2025-07-02 13:52:20",
                "updatedAt": "2025-07-02 13:52:20"
            }
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL); // 激活默认的类型处理
         */
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);  // 设置所有属性都可见
        return objectMapper;
    }
}
