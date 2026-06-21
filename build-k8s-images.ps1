# ============================================================
# 校园自习室预约系统 — Minikube 镜像一键打包脚本
# ============================================================
$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " [1/3] 配置临时编译环境与系统路径..." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 增加 Java 21, Maven 3.9, Minikube 到 Path
$JAVA_HOME = "D:\Services\Java\jdk-21.0.11+10"
$MAVEN_HOME = "D:\Tools\apache-maven-3.9.6"
$MINIKUBE_DIR = "D:\Services\Minikube"

$env:JAVA_HOME = $JAVA_HOME
$env:Path = "$JAVA_HOME\bin;$MAVEN_HOME\bin;$MINIKUBE_DIR;" + $env:Path

Write-Host "[INFO] 当前 Java 版本:"
java -version
Write-Host "[INFO] 当前 Maven 版本:"
mvn -version

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " [2/3] 执行 Maven 多模块依赖编译打包..." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Set-Location -Path "$PSScriptRoot\backend"
mvn clean package -DskipTests

Write-Host "[SUCCESS] 后端所有服务已全部打包成功！" -ForegroundColor Green

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " [3/3] 绑定 Minikube Docker 守护进程并构建镜像..." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 绑定 Minikube 内部 Docker daemon
Write-Host "[INFO] 正在获取 Minikube Docker 环境变量..." -ForegroundColor Yellow
& minikube docker-env --shell powershell | Invoke-Expression

# 依次编译后端各服务镜像
$services = @(
    "study-room-user",
    "study-room-room",
    "study-room-reservation",
    "study-room-violation",
    "study-room-ai",
    "study-room-gateway"
)

foreach ($service in $services) {
    Write-Host "`n----------------------------------------" -ForegroundColor Yellow
    Write-Host " 正在构建后端镜像: $service:1.0.0" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    
    Set-Location -Path "$PSScriptRoot\backend\$service"
    docker build -t "$($service):1.0.0" .
}

# 编译前端镜像
Write-Host "`n----------------------------------------" -ForegroundColor Yellow
Write-Host " 正在构建前端镜像: study-room-frontend:1.0.0" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow

Set-Location -Path "$PSScriptRoot\frontend"
docker build -t "study-room-frontend:1.0.0" .

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host " [SUCCESS] 所有微服务 Docker 镜像已全部成功注入 Minikube！" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
