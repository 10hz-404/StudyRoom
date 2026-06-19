@echo off
echo ============================================================
echo   Study Room Reservation System - One-Click Startup
echo ============================================================

cd /d "%~dp0"

:: ============================================================
:: [Optional] 如果您在 Windows 环境变量中配置的 Key 暂时未能同步刷新生效，
:: 您可以直接取消下面这行的注释（删除前面的“::”），并填入您真实的 API Key：
:: set DEEPSEEK_API_KEY=your_real_deepseek_api_key_here
:: ============================================================

echo 1. Starting Docker middleware (Nacos, MySQL, Redis, RabbitMQ)...
set PATH=C:\Program Files\Docker\Docker\resources\bin;C:\Program Files\nodejs;%PATH%
cd docker
call docker-compose up -d nacos mysql redis rabbitmq
cd ..

echo.
echo 2. Starting backend microservices...
cd backend
start "Backend Microservices" cmd /c start.cmd
cd ..

echo.
echo 3. Starting frontend Vite dev server...
cd frontend
start "Frontend Vite Dev" cmd /k npm run dev
cd ..

echo ============================================================
echo   Startup commands dispatched!
echo   - Frontend URL: http://localhost:5173
echo   - Nacos center: http://localhost:8848/nacos
echo   - Gateway port: http://localhost:8080
echo ============================================================
pause
