# 构建容器命令，在父项目的根目录运行：docker build -t file-system -f file-system/Dockerfile .

# ==========================================
# 阶段 1: 依赖缓存层
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS deps-cache

WORKDIR /app

# 复制 Maven 配置
COPY settings.xml /root/.m2/settings.xml

# 复制父项目 pom.xml 并安装到本地仓库
COPY pom.xml .
RUN mvn install -N -DskipTests -B

# 复制并安装 dubbo-api
COPY dubbo-api/pom.xml dubbo-api/
COPY dubbo-api/src dubbo-api/src/
RUN mvn clean install -DskipTests -B -f dubbo-api/pom.xml

# 预下载 file-system 的依赖
COPY file-system/pom.xml file-system/
RUN mvn dependency:go-offline -B -f file-system/pom.xml || \
    mvn dependency:resolve -B -f file-system/pom.xml

# ==========================================
# 阶段 2: 编译构建（仅源码变更时触发）
# ==========================================
FROM deps-cache AS builder

# 复制 file-system 源码并打包
COPY file-system/src file-system/src
RUN mvn clean package -DskipTests -B -o -f file-system/pom.xml

# ==========================================
# 阶段 3: 运行时（精简镜像）
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/file-system/target/file-system-*.jar app.jar

EXPOSE 8101

HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD wget -qO- http://127.0.0.1:8101/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]