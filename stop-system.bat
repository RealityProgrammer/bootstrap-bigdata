@echo off
echo ===========================================
echo   Stopping E-commerce Analytics System
echo ===========================================
echo.

cd docker
echo Stopping all services...
docker-compose down

echo.
echo Cleaning up containers and networks...
docker-compose down --remove-orphans

echo.
echo System stopped successfully!
echo.
echo To remove all data (including database):
echo   docker-compose down -v
echo.
pause
