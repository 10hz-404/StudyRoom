@echo off
chcp 65001 >nul
set JAVA_HOME=D:\Services\Java\jdk-21.0.11+10

cd /d "%~dp0"

echo ============================================================
echo  Study Room System - Building Maven Modules
echo ============================================================
call "D:\Tools\apache-maven-3.9.6\bin\mvn.cmd" clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Maven build failed!
    pause
    exit /b 1
)

echo ============================================================
echo  Starting 6 microservices staggeredly...
echo ============================================================

echo 1. Starting user service (study-room-user) :8081 ...
start "study-room-user" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-user\target\study-room-user-1.0.0.jar"
timeout /t 5

echo 2. Starting room service (study-room-room) :8082 ...
start "study-room-room" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-room\target\study-room-room-1.0.0.jar"
timeout /t 5

echo 3. Starting reservation service (study-room-reservation) :8083 ...
start "study-room-reservation" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-reservation\target\study-room-reservation-1.0.0.jar"
timeout /t 5

echo 4. Starting violation service (study-room-violation) :8084 ...
start "study-room-violation" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-violation\target\study-room-violation-1.0.0.jar"
timeout /t 5

echo 5. Starting AI service (study-room-ai) :8085 ...
start "study-room-ai" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-ai\target\study-room-ai-1.0.0.jar"
timeout /t 5

echo 6. Starting gateway service (study-room-gateway) :8080 ...
start "study-room-gateway" cmd /k "chcp 65001 >nul && D:\Services\Java\jdk-21.0.11+10\bin\java.exe -Xms48m -Xmx80m -XX:+UseG1GC -jar study-room-gateway\target\study-room-gateway-1.0.0.jar"

echo ============================================================
echo  All microservices initiated!
echo  Gateway listening port: :8080
echo ============================================================
pause
