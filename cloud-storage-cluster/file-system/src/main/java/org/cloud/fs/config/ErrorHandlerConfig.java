package org.cloud.fs.config;

import org.cloud.fs.handler.CustomErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

@Configuration
public class ErrorHandlerConfig {

    @Bean
    @Order(-1) // 必须设置为最高优先级，覆盖默认的 DefaultErrorWebExceptionHandler
    public ErrorWebExceptionHandler customErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ServerCodecConfigurer configurer,
            ApplicationContext applicationContext) {

        return new CustomErrorWebExceptionHandler(
                errorAttributes,
                webProperties,
                configurer,
                applicationContext
        );
    }
}
