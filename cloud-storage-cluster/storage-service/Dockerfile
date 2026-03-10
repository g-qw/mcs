# 构建容器命令，在父项目的根目录运行：docker build -t storage-service -f storage-service/Dockerfile .

# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 复制 Maven 配置
COPY settings.xml /root/.m2/settings.xml

# 父 pom.xml
COPY pom.xml .
RUN mvn install -N -DskipTests -B

# dubbo-api
COPY dubbo-api/pom.xml dubbo-api/
COPY dubbo-api/src dubbo-api/src/
RUN mvn clean install -DskipTests -B -f dubbo-api/pom.xml

# storage-service 依赖
COPY storage-service/pom.xml storage-service/
RUN mvn dependency:resolve -B -f storage-service/pom.xml

# storage-service 源码
COPY storage-service/src storage-service/src
RUN mvn clean package -DskipTests -B -f storage-service/pom.xml

# 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/storage-service/target/storage-service-*.jar app.jar

EXPOSE 8102

HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD wget -qO- http://127.0.0.1:8102/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]