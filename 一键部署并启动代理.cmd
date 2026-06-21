@echo off
set "PATH=C:\Program Files\Docker\Docker\resources\bin;C:\PROGRA~1\Docker\Docker\resources\bin;D:\Services\Minikube;%SystemRoot%\System32;%SystemRoot%;%PATH%"
chcp 65001 > nul
echo ============================================================
echo  [INFO] Launching Campus Study Room K8s One-Key Start Script...
echo ============================================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0k8s-one-key-start.ps1"
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Deployment failed! Stopping process.
    pause
    exit /b %errorlevel%
)
echo.
echo ============================================================
echo  [INFO] Establishing local service port-forward proxies...
echo ============================================================
set KUBECTL=C:\PROGRA~1\Docker\Docker\resources\bin\kubectl.exe
taskkill /f /im kubectl.exe >nul 2>&1
start "K8s Gateway Proxy [Port: 30080]" cmd /k %KUBECTL% port-forward service/gateway-service 30080:8080 -n study-room --address=0.0.0.0
start "K8s Frontend Web [Port: 30088]" cmd /k %KUBECTL% port-forward service/frontend-service 30088:80 -n study-room --address=0.0.0.0
start "K8s SkyWalking UI [Port: 31800]" cmd /k %KUBECTL% port-forward service/skywalking-ui 31800:8080 -n study-room --address=0.0.0.0
start "K8s Kibana Logs [Port: 35601]" cmd /k %KUBECTL% port-forward service/kibana-service 35601:5601 -n study-room --address=0.0.0.0
start "K8s Prometheus [Port: 30090]" cmd /k %KUBECTL% port-forward service/prometheus-service 30090:9090 -n study-room --address=0.0.0.0
start "K8s Grafana Panel [Port: 32000]" cmd /k %KUBECTL% port-forward service/grafana-service 32000:3000 -n study-room --address=0.0.0.0
start "K8s Nacos Registry [Port: 30848]" cmd /k %KUBECTL% port-forward service/nacos-service 30848:8848 -n study-room --address=0.0.0.0
echo.
echo ============================================================
echo  [SUCCESS] Campus Study Room system and monitoring suite deployed!
echo  ----------------------------------------------------------
echo  👉 Frontend Web UI:  http://localhost:30088
echo  👉 API Gateway:      http://localhost:30080
echo  👉 Nacos Console:    http://localhost:30848/nacos
echo  👉 SkyWalking UI:    http://localhost:31800
echo  👉 Kibana (ELK):     http://localhost:35601
echo  👉 Prometheus:       http://localhost:30090
echo  👉 Grafana (admin):  http://localhost:32000
echo ============================================================
echo.
echo Please keep the popped-up cmd proxy windows running.
echo Press any key to exit this script.
pause
