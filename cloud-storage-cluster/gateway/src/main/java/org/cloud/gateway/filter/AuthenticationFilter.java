package org.cloud.gateway.filter;

import com.alibaba.nacos.shaded.com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthenticationFilter implements GatewayFilter {
    private final static Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final static String AUTHORIZATION_TOKEN_PREFIX = "authorization:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public AuthenticationFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param exchange 请求上下文
     * @param chain 过滤器链
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String path = request.getPath().toString();

        // 排除一些接口, 此处的接口路径使用控制器的直接映射，而不是网关代理后的路径
        List<String> excludePaths = List.of(
                "/login",
                "/register",
                "/reset_pwd",
                "/verify"
        );

        if(excludePaths.contains(path))
            return chain.filter(exchange); // 跳过身份验证


        // 身份验证
        String authorization = request.getHeaders().getFirst("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);

            // 验证 token 是否有效
            String key = AUTHORIZATION_TOKEN_PREFIX + token;
            return redisTemplate.hasKey(key).flatMap(
                    exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return chain.filter(exchange);  // 身份有效
                        } else {
                            return unauthorizedResponse(exchange, "token 无效");
                        }
                    }
            );
        } else {
            // 未携带身份验证信息，请求失败
            return unauthorizedResponse(exchange, "请求未携带身份验证信息");
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();

        // 创建一个错误响应对象
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.put("message", message);

        String jsonResponse = new Gson().toJson(errorResponse);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        // 设置响应状态码和内容类型
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 写入响应
        return response.writeWith(Flux.just(buffer));
    }
}
