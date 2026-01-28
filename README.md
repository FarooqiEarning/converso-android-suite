# Converso Android Suite - Enterprise Remote Management

Converso is a professional-grade Android remote management and automation suite, designed for enterprise-level oversight, device telemetry, and real-time remote control.

## 🚀 Overview

The suite consists of four major components:
- **Backend Empire**: A high-performance Node.js/Prisma/PostgreSQL API with Redis-scaled WebSockets.
- **Android Remote Agent**: A native Kotlin agent leveraging Accessibility Services and MediaProjection for deep system control.
- **User Dashboard**: A premium React-based console for individual device management and telemetry visualization.
- **Admin Portal**: A "God-mode" administrative interface for platform-wide user control and billing verification.

## 🛠️ Tech Stack

- **Backend**: Node.js, Express, Prisma, Socket.IO, PostgreSQL, Redis.
- **Mobile**: Kotlin, MediaProjection API, Accessibility Services.
- **Frontend**: React, Vite, Tailwind CSS, Lucide icons.
- **Deployment**: Docker, Docker Compose.

## 📦 Quick Start (Docker)

Ensure you have Docker and Docker Compose installed.

```bash
# Clone the repository
git clone https://github.com/FarooqiEarning/converso-android-suite.git
cd converso-android-suite

# Use the master deployment script
sudo bash deploy.sh
```

## 🔐 Security & Permissions

To enable remote control, the Android agent requires:
- **Accessibility Service**: For touch injection and UI scraping.
- **Notification Access**: For real-time relay of system alerts.
- **Battery Optimization Exemption**: To ensure stable background operation.

## 📄 License

This project is proprietary and confidential.