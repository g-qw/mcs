# 构建阶段
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制 Maven 构建文件（利用 Docker 缓存层）
COPY pom.xml .
COPY src ./src

# 打包（跳过测试加速构建）
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# 运行阶段（使用更小的 JRE 镜像）
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

# 从构建阶段复制 JAR
COPY --from=builder /app/target/gateway-*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai

# 暴露端口
EXPOSE 75

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:75/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]