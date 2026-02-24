package org.cloud.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloud.gateway.exception.JwtAuthenticationException;
import org.cloud.gateway.util.JwtUtil;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GatewayFilter {
    private final JwtUtil jwtUtil;
    private final RedissonClient redissonClient;

    private final static List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/user/login",
            "/api/v1/user/register",
            "/api/v1/user/reset-pwd",
            "/api/v1/user/check-email",
            "/api/v1/user/verify"
    );
    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:"; // Token 黑名单前缀
    private static final String USER_TOKEN_VERSION_PREFIX = "jwt:version:"; // 用户Token版本前缀（用于强制下线）

    /**
     * 身份验证过滤器
     * @param exchange 请求上下文
     * @param chain 过滤器链
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 白名单接口跳过身份验证
        String path = request.getPath().toString();
        if(EXCLUDED_PATHS.contains(path))
            return chain.filter(exchange);

        // 提取 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 提取 token
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        // 验证 jwt token
        try {
            Claims claims = jwtUtil.validate(token);
            validateClaims(claims);
            String uid = claims.getSubject();

            // 设置 UID 属性
            exchange.getAttributes().put("UID", uid);

            // 将 UID 添加到请求头传递给下游服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("UID", uid)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        } catch (JwtAuthenticationException e) {
            return unauthorized(exchange, e.getExplanation());
        }
    }

    public void validateClaims(Claims claims) throws JwtAuthenticationException {
        // 检查是否在黑名单
        String jti = claims.getId();
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
        if (redissonClient.getBucket(blacklistKey).isExists()) {
            // 主动登出/Token失效
            log.info("Token has been revoked - jti: {}, userId: {}", jti, claims.getSubject());
            throw new JwtAuthenticationException("登录已失效，请重新登录");
        }

        // 检查Token版本（强制下线校验）
        String userId = claims.getSubject();
        Long tokenVersion = claims.get("version", Long.class);
        RBucket<Long> versionBucket = redissonClient.getBucket(USER_TOKEN_VERSION_PREFIX + userId);
        Long currentVersion = versionBucket.get();
        if (currentVersion != null && tokenVersion < currentVersion) {
            // 用户被强制下线/账号在其他地方登录
            log.warn("Token version outdated - userId: {}, tokenVersion: {}, currentVersion: {}",
                    userId, tokenVersion, currentVersion);
            throw new JwtAuthenticationException("会话已失效，请重新登录");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Flux.just(buffer));
    }
}
