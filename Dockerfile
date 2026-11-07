# 1. 基础镜像：使用官方的 JDK 17 轻量版
FROM openjdk:17-slim

# 2. 把本地的 Jar 包复制到容器里，并命名为 app.jar
COPY target/secondhand-platform-0.0.1-SNAPSHOT.jar /app.jar

# 3. 声明容器要暴露的端口
EXPOSE 8080

# 4. 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "/app.jar"]