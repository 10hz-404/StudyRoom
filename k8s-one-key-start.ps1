# ============================================================
# Campus Study Room - Minikube K8s One-Key Start Script
# ============================================================
param (
    [switch]$SkipBuild
)
$ErrorActionPreference = "Stop"
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 1. Environment variables and paths
$JAVA_HOME = "D:\Services\Java\jdk-21.0.11+10"
$MAVEN_HOME = "D:\Tools\apache-maven-3.9.6"
$MINIKUBE_DIR = "D:\Services\Minikube"
$NODE_DIR = "C:\Program Files\nodejs"

$env:JAVA_HOME = $JAVA_HOME
$env:Path = "$JAVA_HOME\bin;$MAVEN_HOME\bin;$MINIKUBE_DIR;$NODE_DIR;" + $env:Path

Write-Host "============================================================ " -ForegroundColor Cyan
Write-Host " [INFO] Starting Campus Study Room K8s deployment process... " -ForegroundColor Cyan
Write-Host "============================================================ " -ForegroundColor Cyan

# 2. Check Minikube status
Write-Host "`n[1/5] Checking Minikube status... " -ForegroundColor Yellow
$status = ""
$oldPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $status = & minikube status --format "{{.Host}}" 2>$null
} catch {
    $status = "Stopped"
}
$ErrorActionPreference = $oldPreference

if ($status -ne "Running") {
    Write-Host "[WARNING] Minikube is not running or needs refresh! Starting Minikube... " -ForegroundColor Magenta
    & minikube start --driver=docker --memory=6144 --cpus=4
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Minikube failed to start! Exiting script." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[SUCCESS] Minikube is running normally! " -ForegroundColor Green
}

# Resolve paths used for deployment
$BACKEND_DIR = "$PSScriptRoot\backend"
$FRONTEND_DIR = "$PSScriptRoot\frontend"
$parentDir = Get-Item "$PSScriptRoot\..\.."
$phase5Dir = Get-ChildItem -Path $parentDir.FullName -Directory | Where-Object { $_.Name -like "*5*" } | Select-Object -First 1
$DOCKER_DIR = "$($phase5Dir.FullName)\docker"
$K8S_DIR = "$($phase5Dir.FullName)\k8s"

if (-not $SkipBuild) {
    # 3. Compile and build backend and frontend
    Write-Host "`n[2/5] Compiling and packaging backend and frontend... " -ForegroundColor Yellow

    # Backend build
    Write-Host "[INFO] Compiling backend Java microservices... " -ForegroundColor Cyan
    Set-Location -Path $BACKEND_DIR
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Maven build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[SUCCESS] Backend microservices compiled successfully! " -ForegroundColor Green

    # Frontend build
    Write-Host "[INFO] Compiling frontend Vue3 project... " -ForegroundColor Cyan
    Set-Location -Path $FRONTEND_DIR
    if (-not (Test-Path "node_modules")) {
        Write-Host "[INFO] node_modules not found, executing npm install... " -ForegroundColor Cyan
        npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] npm install failed!" -ForegroundColor Red
            exit 1
        }
    }
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Frontend compilation failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[SUCCESS] Frontend compiled successfully! " -ForegroundColor Green

    # 4. Prepare Docker build context
    Write-Host "`n[3/5] Preparing docker build context and temporary package copy... " -ForegroundColor Yellow
    Set-Location -Path $DOCKER_DIR

    # Copy jars
    $services = @("gateway", "user", "room", "reservation", "violation", "ai")
    foreach ($s in $services) {
        Copy-Item -Path "$BACKEND_DIR\study-room-$s\target\study-room-$s-1.0.0.jar" -Destination "$DOCKER_DIR\study-room-$s\" -Force
    }

    # Copy frontend dist
    if (Test-Path "frontend\dist") {
        Remove-Item -Path "frontend\dist" -Recurse -Force
    }
    Copy-Item -Path "$FRONTEND_DIR\dist" -Destination "frontend\dist" -Recurse -Force
    Write-Host "[SUCCESS] Build context preparation completed! " -ForegroundColor Green

    # 4.5 Pre-pulling base images to avoid network issues inside Minikube
    Write-Host "`n[3.5/5] Pre-pulling base images on host and loading to Minikube... " -ForegroundColor Yellow
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        Write-Host "[INFO] Pulling eclipse-temurin:21-jre-alpine on host..." -ForegroundColor Cyan
        & docker pull eclipse-temurin:21-jre-alpine
        Write-Host "[INFO] Pulling nginx:alpine on host..." -ForegroundColor Cyan
        & docker pull nginx:alpine

        Write-Host "[INFO] Loading eclipse-temurin:21-jre-alpine into Minikube (this may take a minute)..." -ForegroundColor Cyan
        & minikube image load eclipse-temurin:21-jre-alpine
        Write-Host "[INFO] Loading nginx:alpine into Minikube..." -ForegroundColor Cyan
        & minikube image load nginx:alpine
        Write-Host "[SUCCESS] Base images successfully loaded into Minikube!" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Failed to pre-load base images on host. Will try to build directly inside Minikube." -ForegroundColor Magenta
    }
    $ErrorActionPreference = $oldPreference

    # 5. Bind Minikube Docker daemon and build images
    Write-Host "`n[4/5] Binding Minikube Docker daemon and building images... " -ForegroundColor Yellow
    & minikube docker-env --shell powershell | Invoke-Expression

    foreach ($s in $services) {
        Write-Host "[INFO] Building backend image: study-room-${s}:v999 ... " -ForegroundColor Cyan
        Set-Location -Path "$DOCKER_DIR\study-room-$s"
        docker build -t "study-room-${s}:v999" .
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] Failed to build image study-room-$s!" -ForegroundColor Red
            exit 1
        }
    }

    Write-Host "[INFO] Building frontend image: study-room-frontend:v999 ... " -ForegroundColor Cyan
    Set-Location -Path "$DOCKER_DIR\frontend"
    docker build -t "study-room-frontend:v999" .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to build frontend image!" -ForegroundColor Red
        exit 1
    }

    # Clean up build caches
    Write-Host "[INFO] Cleaning up temporary build cache files... " -ForegroundColor DarkGray
    foreach ($s in $services) {
        Remove-Item -Path "$DOCKER_DIR\study-room-$s\study-room-$s-1.0.0.jar" -ErrorAction SilentlyContinue
    }
    Remove-Item -Path "$DOCKER_DIR\frontend\dist" -Recurse -ErrorAction SilentlyContinue
    Write-Host "[SUCCESS] Mirror compilation and injection into Minikube completed! " -ForegroundColor Green
} else {
    Write-Host "`n[INFO] SkipBuild flag detected! Skipping compilation, packaging and image building... " -ForegroundColor Yellow
}

# 6. Kubernetes Deployment
Write-Host "`n[5/5] Applying K8s resource manifests... " -ForegroundColor Yellow
Set-Location -Path $K8S_DIR

Write-Host "[INFO] Creating namespace... " -ForegroundColor Cyan
kubectl apply -f namespace.yaml

Write-Host "[INFO] Applying ConfigMap and Secret configurations... " -ForegroundColor Cyan
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

Write-Host "[INFO] Deploying common middleware... " -ForegroundColor Cyan
kubectl apply -f middlewares/

Write-Host "[INFO] Waiting for Nacos Registry to be fully ready (Max 180s)... " -ForegroundColor Cyan
try {
    & kubectl wait --namespace=study-room --for=condition=Ready pod -l app=studyroom-nacos --timeout=180s
    Write-Host "[SUCCESS] Nacos Registry is ready! " -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Waiting for Nacos Registry timed out. Continuing deployment... " -ForegroundColor Magenta
}

Write-Host "[INFO] Deploying microservices and frontend... " -ForegroundColor Cyan
kubectl apply -f apps/
Write-Host "[INFO] Performing rollout restart to apply new configuration... " -ForegroundColor Cyan
kubectl rollout restart deployment/study-room-gateway deployment/study-room-user deployment/study-room-room deployment/study-room-reservation deployment/study-room-violation deployment/study-room-ai -n study-room

Write-Host "[INFO] Deploying monitoring stack (SkyWalking, ELK, Prometheus, Grafana)... " -ForegroundColor Cyan
kubectl apply -f monitoring/

Write-Host "`n============================================================ " -ForegroundColor Green
Write-Host " [SUCCESS] Kubernetes one-key deployment completed successfully! " -ForegroundColor Green
Write-Host "============================================================ " -ForegroundColor Green

# 7. Monitor Pod status
Write-Host "`nChecking service startup progress (Press Ctrl+C to exit monitoring)... " -ForegroundColor Yellow
while ($true) {
    Clear-Host
    Write-Host "============================================================ " -ForegroundColor Cyan
    Write-Host " Kubernetes Running Status Monitor " -ForegroundColor Cyan
    Write-Host "============================================================ " -ForegroundColor Cyan
    Write-Host "Current Time: $(Get-Date)`n"
    
    # Print Pod status
    kubectl get pods -n study-room -o wide
    
    # Check if all status are Running or Succeeded
    $podPhases = kubectl get pods -n study-room -o jsonpath='{.items[*].status.phase}'
    $allRunning = $true
    if ($podPhases) {
        $phaseList = $podPhases -split ' '
        foreach ($phase in $phaseList) {
            if ($phase -ne "Running" -and $phase -ne "Succeeded") {
                $allRunning = $false
                break
            }
        }
    } else {
        $allRunning = $false
    }
    
    if ($allRunning) {
        Write-Host "`n[SUCCESS] All microservices and middlewares are successfully running! " -ForegroundColor Green
        break
    }
    
    Write-Host "`nNotice: Refreshing status every 5 seconds... " -ForegroundColor DarkGray
    Start-Sleep -Seconds 5
}

Write-Host "`n============================================================ " -ForegroundColor Green
Write-Host " Service Access Guide " -ForegroundColor Green
Write-Host "============================================================ " -ForegroundColor Green
Write-Host " 1. Frontend Web UI: " -ForegroundColor Yellow
Write-Host "    - Port: 30088 (NodePort)"
Write-Host "    - URL:  http://localhost:30088 "
Write-Host "    - Hint: If unreachable, run 'minikube service frontend-service -n study-room'"
Write-Host " 2. Nacos Console: " -ForegroundColor Yellow
Write-Host "    - URL:  http://localhost:30848/nacos "
Write-Host " 3. API Gateway Port: 30080 " -ForegroundColor Yellow
Write-Host "============================================================ " -ForegroundColor Green
Write-Host "Deployment completed. Press any key or close this window to exit. " -ForegroundColor Cyan
