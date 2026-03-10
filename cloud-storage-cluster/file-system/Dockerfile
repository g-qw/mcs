# 构建容器命令，在父项目的根目录运行：docker build -t file-system -f file-system/Dockerfile .

# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 复制 Maven 配置
COPY settings.xml /root/.m2/settings.xml

# 下载依赖
COPY pom.xml .
COPY dubbo-api/pom.xml dubbo-api/
COPY file-system/pom.xml file-system/
RUN mvn install -N -DskipTests -B && \
    mvn dependency:go-offline -B -pl dubbo-api,file-system -am

# 复制源码
COPY dubbo-api/src dubbo-api/src/
COPY file-system/src file-system/src

# 编译打包
RUN mvn clean package -DskipTests -B -pl dubbo-api,file-system -am -o

# 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/file-system/target/file-system-*.jar app.jar

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]

EXPOSE 8101

HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD wget -qO- http://127.0.0.1:8101/actuator/health > /dev/null || exit 1