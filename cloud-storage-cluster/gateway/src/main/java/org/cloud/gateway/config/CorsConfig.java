package org.cloud.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173"));  // 允许所有域名跨域
        corsConfiguration.addAllowedMethod("*");  // 允许所有请求方法
        corsConfiguration.addAllowedHeader("*"); // 允许所有请求头
        corsConfiguration.setAllowCredentials(true);  // 允许携带 cookie 的请求
        corsConfiguration.setMaxAge(3600L);  // 预检请求的有效期为 60 分钟，单位：秒

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }
}
