# 构建容器命令，在父项目的根目录运行：docker build -t gateway -f gateway/Dockerfile .

# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 下载依赖到本地仓库，-f 指定文件，不检查 reactor
COPY gateway/settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY gateway/pom.xml gateway/
RUN mvn dependency:resolve -B -f gateway/pom.xml

# 复制源码并打包
COPY gateway/src gateway/src/
RUN mvn clean package -DskipTests -B -f gateway/pom.xml

# 运行阶段（使用更小的 JRE 镜像）
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /app/gateway/target/gateway-*.jar app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60.0 \
    -XX:MaxMetaspaceSize=64m \
    -Xss256k"

EXPOSE 75

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD sh -c 'echo > /dev/tcp/localhost/75' 2>/dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]