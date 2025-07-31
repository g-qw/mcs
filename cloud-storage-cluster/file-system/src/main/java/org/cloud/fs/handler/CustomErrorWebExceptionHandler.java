package org.cloud.fs.handler;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

public class CustomErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public CustomErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ServerCodecConfigurer configurer,
            ApplicationContext applicationContext) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> error = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        HttpStatus status = HttpStatus.valueOf((int) error.getOrDefault("status", 500));

        String message = "服务器内部错误";
        Throwable ex = getError(request);

        if (ex instanceof MethodNotAllowedException) {
            message = "请求方法不被允许";

        } else if (ex instanceof ChangeSetPersister.NotFoundException) {
            message = "请求资源不存在";

        } else if (ex instanceof UnsupportedMediaTypeStatusException) {
            message = "不支持的媒体类型";

        } else if (ex instanceof ServerWebInputException) {
            message = "请求体格式错误或无法解析";

        } else if (ex instanceof ResponseStatusException) {
            message = ((ResponseStatusException) ex).getReason();
        } else if (ex instanceof BindingResult) {
            message = "参数校验失败";
        }

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of(
                        "code", status.value(),
                        "message", message
                )));
    }
}
