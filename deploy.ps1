# --- CONVERSO EMPIRE MASTER DEPLOYMENT SCRIPT (WINDOWS) ---

Write-Host "🚀 Starting World-Class Deployment Flow..." -ForegroundColor Cyan

# 1. Android APK Build
Write-Host "📦 Building Android APK..." -ForegroundColor Yellow
cd mobile
.\gradlew assembleRelease
cd ..

# 2. Master Docker Build
Write-Host "🐳 Building Docker Containers..." -ForegroundColor Yellow
sudo docker compose build --parallel

# 3. Global Orchestration Up
Write-Host "🚢 Deploying Converso Suite to Docker..." -ForegroundColor Yellow
sudo docker compose up -d

Write-Host "✨ DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "User Dashboard:  http://localhost:9082"
Write-Host "Admin Panel:     http://localhost:9083"
Write-Host "Backend API:     http://localhost:9081"
