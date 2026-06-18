# 校园自习室预约系统 - 后端服务 (Backend)

基于 **Spring Boot 3** 和 **Java 21** 构建的校园自习室预约系统后端服务，集成了 MyBatis-Plus、MySQL、达梦数据库（国产双通道适配）、Redis 缓存、RabbitMQ 消息队列以及 DeepSeek AI 智能处理模块。

## 🛠️ 环境要求

- **操作系统**: Windows / macOS / Linux
- **Java 开发包**: JDK 21+
- **构建工具**: Maven 3.6+
- **物理环境依赖**:
  - MySQL 8.0+（主数据库）
  - 达梦数据库 DM8（可选，国产化适配双数据源运行）
  - Redis 7.0+（缓存、接口限流与 AI 熔断降级状态机维护）
  - RabbitMQ 3.11+（用于 AI 任务异步计算与违约消息延迟判定检测）

> [!TIP]
> **推荐使用 Docker Compose**。项目根目录下提供了完整的 `docker/docker-compose.yml`，可以在新环境下一键拉起并配置好所有依赖。

---

## 🚀 快速启动指南

### 第一步：启动基础设施环境
如果您安装了 Docker 与 Docker Compose，请在项目根目录（第4阶段下）执行：
```bash
docker-compose -f docker/docker-compose.yml up -d
```
这会一键启动并自动完成：
1. **MySQL 8.0**：端口 `3306`，自动导入建表 `01-schema.sql`、索引 `02-indexes.sql` 与初始测试数据 `03-test-data.sql`。
2. **达梦数据库 (dm8)**：端口 `5236`，并执行适配初始化。
3. **Redis**：端口 `6379`，配置密码 `redis123`。
4. **RabbitMQ**：端口 `5672`（连接口）与 `15672`（Web控制台，账号 `admin`/`admin123`）。

### 第二步：配置项目属性
编辑 `src/main/resources/application.yml` 配置文件：
1. **数据库、Redis、RabbitMQ 链接密码**：默认已与 Docker 环境完美对齐，如无需更改可直接运行。
2. **DeepSeek AI 配置**：
   ```yaml
   deepseek:
     api-key: ${DEEPSEEK_API_KEY:your_api_key_here} # 请配置您的真实 API 密钥
     base-url: https://api.deepseek.com
     model: deepseek-chat
   ```
   *您也可以选择不修改文件，直接配置系统环境变量 `DEEPSEEK_API_KEY`，项目会自动读取。*

### 第三步：构建与启动
在当前 `backend` 目录下打开终端，执行以下命令：
```bash
# 编译并打包
mvn clean package

# 启动后端服务
java -jar target/study-room-backend-1.0.0.jar
```
或者在 IDE（如 IntelliJ IDEA / VS Code）中打开，直接运行主启动类 `com.studyroom.StudyRoomApplication`。

---

## 🔑 默认测试账号

系统初始化了以下几组默认数据（所有账号的明文密码均为 `123456`）：

| 身份 | 学号 / 工号 | 姓名 | 默认密码 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **管理员** | `A001` | 张管理 | `123456` | 违规审核、自习室管理等权限 |
| **管理员** | `A002` | 李管理 | `123456` | 管理端测试账号 |
| **学生** | `S2024001` | 王同学 | `123456` | 正常可用，有初始推荐偏好数据 |
| **学生** | `S2024002` | 陈同学 | `123456` | 正常可用学生账号 |
| **学生** | `S2024003` | 刘同学 | `123456` | 正常可用学生账号 |

---

## 📖 在线 API 文档

后端集成了 Knife4j (Swagger 3)，启动成功后可以在浏览器访问以下网址查看和调试接口：
🔗 **接口文档地址**: [http://localhost:8080/doc.html](http://localhost:8080/doc.html)

## 🌟 核心架构增强特性
1. **AI 智能自习室推荐**：采用策略模式解耦算法（`AIDeepSeekStrategy` 和 `RuleBasedStrategy`），通过 `AiLimiter` 控制流控（每日限额 50 次，连续失败 5 次熔断降级），使用 RabbitMQ 消息队列实现推荐任务异步计算与前端轮询。
2. **考勤违规判级联动**：管理员点击“驳回”将级联自动把预约记录还原为“已取消（`CANCELLED`）”；点击“确认”则变更为“未签到违约（`NO_SHOW`）”。
3. **软删除重命名去冲突**：删除座位时后端使用软删除，同时重命名该座位编号（为 `seatNo_{id}` 结构），释放原始编号以防同名重建时数据库联合唯一索引冲突。
