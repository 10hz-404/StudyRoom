@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ============================================================
echo [校园自习室预约系统] 后端微服务编译与一键 stagger 启动脚本
echo ============================================================
echo.

:: 1. 检测 Java 运行环境
where java >nul 2>&1
if %errorlevel% equ 0 (
    set "JAVA_EXEC=java"
    echo [INFO] 检测到系统环境变量中已配置 Java，将使用系统默认 Java。
) else (
    if exist "D:\Services\Java\jdk-21.0.11+10\bin\java.exe" (
        set "JAVA_EXEC=D:\Services\Java\jdk-21.0.11+10\bin\java.exe"
        echo [INFO] 系统变量未检测到 Java，将使用内置物理路径 Java 21。
    ) else (
        echo [ERROR] 未能定位 Java 运行环境，请确保已安装 JDK 21 并加入系统环境变量 PATH！
        pause
        exit /b 1
    )
)

:: 2. 检测 Maven 编译环境
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    set "MVN_EXEC=mvn"
    echo [INFO] 检测到系统环境变量中已配置 Maven，将使用系统默认 Maven。
) else (
    if exist "D:\Tools\apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_EXEC=D:\Tools\apache-maven-3.9.6\bin\mvn.cmd"
        echo [INFO] 系统变量未检测到 Maven，将使用内置物理路径 Maven 3.9.6。
    ) else (
        echo [ERROR] 未能定位 Maven 编译环境，请确保已安装 Maven 并加入系统环境变量 PATH！
        pause
        exit /b 1
    )
)

echo.
echo [1/2] 正在执行多模块 Maven 依赖打包...
call "%MVN_EXEC%" clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven 编译失败，请检查编译日志及依赖链！
    pause
    exit /b 1
)
echo [SUCCESS] 后端微服务全部模块打包成功！

echo.
echo [2/2] 正在错开时间（Stagger）依次拉起 6 个微服务容器...
echo ============================================================

echo 1. 启动 用户微服务 (study-room-user) :8081 ...
start "study-room-user" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-user\target\study-room-user-1.0.0.jar"
timeout /t 4 >nul

echo 2. 启动 自习室微服务 (study-room-room) :8082 ...
start "study-room-room" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-room\target\study-room-room-1.0.0.jar"
timeout /t 4 >nul

echo 3. 启动 预约考勤微服务 (study-room-reservation) :8083 ...
start "study-room-reservation" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-reservation\target\study-room-reservation-1.0.0.jar"
timeout /t 4 >nul

echo 4. 启动 违规处理微服务 (study-room-violation) :8084 ...
start "study-room-violation" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-violation\target\study-room-violation-1.0.0.jar"
timeout /t 4 >nul

echo 5. 启动 AI推荐分析微服务 (study-room-ai) :8085 ...
start "study-room-ai" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-ai\target\study-room-ai-1.0.0.jar"
timeout /t 4 >nul

echo 6. 启动 统一路由网关 (study-room-gateway) :8080 ...
start "study-room-gateway" cmd /k "chcp 65001 >nul && "!JAVA_EXEC!" -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-gateway\target\study-room-gateway-1.0.0.jar"

echo.
echo ============================================================
echo [ALL SUCCESS] 后端微服务集群启动命令已全部下发！
echo 请稍等 10-20 秒使服务在 Nacos 中注册生效。
echo 统一网关监听端口: http://localhost:8080
echo ============================================================
pause
