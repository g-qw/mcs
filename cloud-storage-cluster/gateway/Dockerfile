# 构建容器命令，在父项目的根目录运行：docker build -t gateway -f gateway/Dockerfile .

# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY gateway/pom.xml gateway/
RUN mvn dependency:resolve -B -f gateway/pom.xml

# 复制源码并打包
COPY gateway/src gateway/src/
RUN mvn clean package -DskipTests -B -f gateway/pom.xml

# 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/gateway/target/gateway-*.jar app.jar

EXPOSE 75

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=30s \
            --retries=3 \
            CMD sh -c 'echo > /dev/tcp/localhost/75' 2>/dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]