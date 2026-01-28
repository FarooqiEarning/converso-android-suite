# Enterprise Suite Orchestrator
# Builds Android Client + Backend + 3x Dashboards

PRINT_COLOR() { printf "\e[1;36m$1\e[0m\n"; }

PRINT_COLOR "🚀 Initiating Converso Empire Deployment..."

# 1. Build Android APK
if [ -f "mobile/gradlew" ]; then
    PRINT_COLOR "📦 Building Android Remote Agent..."
    cd mobile
    chmod +x gradlew
    ./gradlew assembleRelease
    cd ..
else
    PRINT_COLOR "⚠️  Skipping Android Build: Gradle wrapper not found in mobile/ directory."
fi

# 2. Dynamic Environment Resolution
PRINT_COLOR "🔧 Configuring Production Environment..."
# Ensure .env exists in backend
if [ ! -f backend/.env ]; then
    cp backend/.env.example backend/.env
fi

# 3. Docker Orchestration
PRINT_COLOR "🐋 Building & Starting Containers..."
sudo docker compose down
sudo docker compose build --parallel
sudo docker compose up -d

PRINT_COLOR "✅ Deployment Successful!"
PRINT_COLOR "--------------------------------------------"
PRINT_COLOR "Backend:   http://localhost:9081"
PRINT_COLOR "User App:  http://localhost:9082"
PRINT_COLOR "Admin App: http://localhost:9083"
PRINT_COLOR "--------------------------------------------"
