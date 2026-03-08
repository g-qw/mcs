# 构建容器命令，在父项目的根目录运行：docker build -t user-service -f user-service/Dockerfile .

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY dubbo-api/pom.xml dubbo-api/
COPY dubbo-api/src dubbo-api/src/
RUN mvn clean install -DskipTests -B -f dubbo-api/pom.xml

# 复制 user-service 的 pom.xml 并解析依赖
COPY user-service/pom.xml user-service/
RUN mvn dependency:resolve -B -f user-service/pom.xml

# 复制 user-service 源码并打包
COPY user-service/src user-service/src/
RUN mvn clean package -DskipTests -B -f user-service/pom.xml

# 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/user-service/target/user-service-*.jar app.jar

EXPOSE 8100

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=30s \
            --retries=3 \
            CMD sh -c 'echo > /dev/tcp/localhost/8100' 2>/dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]