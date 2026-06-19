@echo off
set JAVA_HOME=D:\Services\Java\jdk-21.0.11+10
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_HOME=D:\Tools\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%PATH%

cd /d D:\JYU\10、软件架构技术\fina_exam\第4阶段\code\backend

echo ============================================================
echo  校园自习室预约系统 — 微服务集成构建编译
echo ============================================================
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo 构建失败，请检查报错！
    pause
    exit /b 1
)

echo ============================================================
echo  正在依次拉起 6 个微服务，请注意观察弹出的各控制台日志...
echo ============================================================

echo 1. 正在启动 用户与认证服务 (study-room-user) :8081 ...
start "study-room-user" java -jar study-room-user\target\study-room-user-1.0.0.jar
timeout /t 5

echo 2. 正在启动 自习室与座位服务 (study-room-room) :8082 ...
start "study-room-room" java -jar study-room-room\target\study-room-room-1.0.0.jar
timeout /t 5

echo 3. 正在启动 预约与考勤服务 (study-room-reservation) :8083 ...
start "study-room-reservation" java -jar study-room-reservation\target\study-room-reservation-1.0.0.jar
timeout /t 5

echo 4. 正在启动 违规管理服务 (study-room-violation) :8084 ...
start "study-room-violation" java -jar study-room-violation\target\study-room-violation-1.0.0.jar
timeout /t 5

echo 5. 正在启动 AI智能推荐与分析服务 (study-room-ai) :8085 ...
start "study-room-ai" java -jar study-room-ai\target\study-room-ai-1.0.0.jar
timeout /t 5

echo 6. 正在启动 统一网关服务 (study-room-gateway) :8080 ...
start "study-room-gateway" java -jar study-room-gateway\target\study-room-gateway-1.0.0.jar

echo ============================================================
echo  所有微服务拉起就绪，请使用 Nacos 控制台确认路由注册成功！
echo  网关监听端口为 :8080 (对齐前端)
echo ============================================================
pause
