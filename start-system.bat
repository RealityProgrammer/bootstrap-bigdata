@echo off
echo ===========================================
echo   E-commerce Analytics BigData System
echo ===========================================
echo.

REM Check if Docker is running
docker version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

echo Starting services with Docker Compose...
cd docker
docker-compose up -d

echo.
echo Waiting for services to start...
timeout /t 30 /nobreak >nul

echo.
echo ===========================================
echo   Services are starting up!
echo ===========================================
echo.
echo Frontend Dashboard: http://localhost:3000
echo Backend API:        http://localhost:8080
echo Spark Master UI:    http://localhost:8090
echo MySQL Database:     localhost:3306
echo.
echo ===========================================
echo   Commands to manage the system:
echo ===========================================
echo.
echo Stop all services:
echo   docker-compose down
echo.
echo View logs:
echo   docker-compose logs -f [service_name]
echo.
echo Restart a service:
echo   docker-compose restart [service_name]
echo.
echo ===========================================

pause
