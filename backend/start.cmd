@echo off
set JAVA_HOME=D:\Services\Java\jdk-21.0.11+10
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_HOME=D:\Tools\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%PATH%

cd /d D:\JYU\10???????\fina_exam\code\backend

echo Building...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Starting backend on http://localhost:8080 ...
java -jar target\study-room-backend-1.0.0.jar
