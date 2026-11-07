# 校园二手交易平台后端 (Campus Second-hand Trading Platform)

基于 Spring Boot 4 开发的前后端分离校园二手交易系统，支持商品发布、多条件搜索、实时私信、举报审核等完整闭环。

## 🛠 技术栈
- **核心框架**：Java 17, Spring Boot 4
- **数据存储**：MySQL 8.0, Redis (缓存与限流)
- **持久层**：MyBatis-Plus
- **安全鉴权**：Spring Security (BCrypt), JWT
- **实时通讯**：WebSocket (STOMP)
- **部署**：Docker, 阿里云轻量应用服务器

## ✨ 核心功能
1. **用户模块**：学号注册、加密登录、JWT 鉴权、个人资料修改。
2. **商品模块**：多图上传、动态多条件搜索（关键字/价格区间/分类）、商品详情聚合展示。
3. **私信模块**：WebSocket 实时推送，离线消息 MySQL 持久化，历史记录拉取。
4. **平台治理**：用户举报、管理员审核商品、违规商品强制下架并发送系统通知。

## 🚀 本地运行指南
1. 克隆项目：`git clone https://github.com/...`
2. 修改 `application.properties` 中的数据库和 Redis 配置。
3. 打包：`mvn clean package`
4. 运行 Jar 包：`java -jar target/secondhand-platform-0.0.1-SNAPSHOT.jar`

## 📦 部署
已全面采用 Docker 容器化部署，配置 `Dockerfile`，支持一键构建镜像与运行。