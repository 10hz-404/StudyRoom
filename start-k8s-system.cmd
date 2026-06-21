@echo off
set "PATH=C:\Program Files\Docker\Docker\resources\bin;C:\PROGRA~1\Docker\Docker\resources\bin;D:\Services\Minikube;D:\Services\Java\jdk-21.0.11+10\bin;D:\Tools\apache-maven-3.9.6\bin;%SystemRoot%\System32;%SystemRoot%;%PATH%"
chcp 65001 > nul
echo ============================================================
echo  [INFO] Launching Campus Study Room K8s System...
echo ============================================================
echo.
echo [INFO] Running PowerShell launcher, please wait...
powershell -NoExit -ExecutionPolicy Bypass -File "%~dp0k8s-one-key-start.ps1" -SkipBuild
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] PowerShell script exited abnormally.
    pause
)
