package org.cloud.fs.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.failOnUnknownProperties(false);  // 忽略未知属性
        builder.indentOutput(true);  // 格式化输出
        builder.modules(new JavaTimeModule());  // 注册 JavaTimeModule 模块
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 禁用将日期格式化为时间戳的功能
        builder.visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);  // 设置所有属性都可见
        return builder;
    }
}
