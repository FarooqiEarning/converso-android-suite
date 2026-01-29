# Enterprise Suite Orchestrator
# Builds Android Client + Backend + 3x Dashboards

PRINT_COLOR() { printf "\e[1;36m$1\e[0m\n"; }

PRINT_COLOR "🚀 Initiating Converso Empire Deployment..."

# 1. Build Android APK
if [ -d "mobile" ]; then
    PRINT_COLOR "📦 Building Android Remote Agent..."
    cd mobile
    
    # 1. Ensure Android SDK is configured
    if [ ! -f "local.properties" ]; then
        PRINT_COLOR "🔍 Detecting Android SDK..."
        SDK_PATH=""
        POSSIBLE_PATHS=(
            "$HOME/Android/Sdk"
            "/usr/lib/android-sdk"
            "/opt/android-sdk"
            "/usr/local/lib/android-sdk"
        )

        for path in "${POSSIBLE_PATHS[@]}"; do
            if [ -d "$path" ]; then
                SDK_PATH="$path"
                break
            fi
        done

        if [ -n "$SDK_PATH" ]; then
            PRINT_COLOR "✅ Found SDK at $SDK_PATH"
            echo "sdk.dir=$SDK_PATH" > local.properties
        else
            PRINT_COLOR "⚠️  Android SDK not found in common locations."
            PRINT_COLOR "Please enter your Android SDK path (e.g., /home/owner/Android/Sdk):"
            read -r SDK_PATH
            if [ -d "$SDK_PATH" ]; then
                echo "sdk.dir=$SDK_PATH" > local.properties
            else
                PRINT_COLOR "❌ Invalid SDK path. Build will likely fail."
            fi
        fi
    fi

    # 2. Initialize gradle wrapper if it doesn't exist or JAR is missing
    if [ ! -f "gradlew" ] || [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
        PRINT_COLOR "🔧 Initializing Gradle wrapper..."
        
        # Try to use installed gradle if available
        if command -v gradle >/dev/null 2>&1; then
            gradle wrapper --gradle-version 8.2 || PRINT_COLOR "⚠️ Failed to run 'gradle wrapper'"
        fi
        
        # Double check if JAR is still missing
        if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
            PRINT_COLOR "⚠️ gradle-wrapper.jar is missing. Attempting to download..."
            # Ensure directory exists
            mkdir -p gradle/wrapper
            curl -L https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar || {
                PRINT_COLOR "❌ Failed to download Gradle wrapper JAR!"
                cd ..
                exit 1
            }
        fi
    fi
    
    chmod +x gradlew
    if ./gradlew assembleRelease; then
        PRINT_COLOR "✅ Android APK built successfully!"
        cd ..
    else
        PRINT_COLOR "❌ Android APK build failed!"
        cd ..
    fi
else
    PRINT_COLOR "⚠️  Skipping Android Build: mobile/ directory not found."
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
PRINT_COLOR "Backend:   https://backend-as.conversoempire.world"
PRINT_COLOR "User App:  https://as.conversoempire.world"
PRINT_COLOR "Admin App: https://admin-as.conversoempire.world"
PRINT_COLOR "--------------------------------------------"
