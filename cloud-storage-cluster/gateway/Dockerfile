# 构建命令

# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 先复制 pom.xml 单独下载依赖（利用 Docker 缓存层）
COPY pom.xml .
COPY gateway/pom.xml gateway/

# 下载依赖到本地仓库
RUN mvn clean package -DskipTests -B -pl gateway -am

# 复制源码
COPY gateway/src gateway/src/

# 打包，-o 离线模式
RUN mvn clean package -DskipTests -B -o -pl gateway -am

# 运行阶段（使用更小的 JRE 镜像）
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /build/gateway/target/gateway-*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai

# 暴露端口
EXPOSE 75

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD sh -c 'echo > /dev/tcp/localhost/75' 2>/dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profile.active=prod", \
    "-jar", "app.jar"]