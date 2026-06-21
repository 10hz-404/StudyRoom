@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================================
echo   [校园自习室预约系统] 裸机/容器混合开发环境一键启动脚本
echo ============================================================
echo.

cd /d "%~dp0"

:: ============================================================
:: [API Key 说明] 
:: 如果您在 Windows 环境变量中配置的 Key 暂时未能同步刷新生效，
:: 您可以直接取消下面这行的注释（删除前面的“::”），并填入您真实的 API Key：
:: set DEEPSEEK_API_KEY=your_real_deepseek_api_key_here
:: ============================================================

:: 1. 确保 Docker 运行路径与 Node 路径存在于临时 PATH 中
set "PATH=C:\Program Files\Docker\Docker\resources\bin;C:\Program Files\nodejs;%PATH%"

echo [1/3] 正在启动底层 Docker 中间件 (Nacos, MySQL, Redis, RabbitMQ)...
cd docker
call docker-compose up -d nacos mysql redis rabbitmq
if %errorlevel% neq 0 (
    echo [WARNING] Docker 容器启动失败，请检查 Docker Desktop 是否已开启！
)
cd ..

echo.
echo [2/3] 正在编译并启动后端微服务集群...
cd backend
start "后端微服务集群 - 启动中" cmd /c start.cmd
cd ..

echo.
echo [3/3] 正在启动前端开发服务器...
cd frontend
where npm >nul 2>&1
if %errorlevel% equ 0 (
    if not exist "node_modules" (
        echo [INFO] 检测到 node_modules 不存在，正在为您自动执行 npm install...
        call npm install
    )
    start "前端 Web 客户端 - 启动中" cmd /k "chcp 65001 >nul && npm run dev"
) else (
    echo [ERROR] 未检测到 Node.js (npm) 运行环境，无法自动启动前端！
    echo 请在系统安装 Node.js 并在环境变量中配置，或者手动在 frontend 目录下启动。
)
cd ..

echo.
echo ============================================================
echo   一键启动指令已下发完毕！
echo   - 前端访问入口：🔗 http://localhost:5173
echo   - 统一网关入口：🔗 http://localhost:8080
echo   - Nacos 控制台：🔗 http://localhost:8848/nacos
echo ============================================================
echo.
pause
