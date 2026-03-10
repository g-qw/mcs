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

# 下载所有依赖和插件
RUN mvn dependency:go-offline dependency:resolve-plugins -B

# 编译 dubbo-api
COPY dubbo-api/src dubbo-api/src/
RUN mvn clean install -DskipTests -B -f dubbo-api/pom.xml

# 编译并打包 file-system
COPY file-system/src file-system/src
RUN mvn clean package -DskipTests -B -f file-system/pom.xml

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