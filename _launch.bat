@echo off
title ShareSystem Services
echo ========================================
echo   ShareSystem - Starting Services
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Starting User Service (port 8181)...
start "UserService-8181" cmd /c "cd /d D:\IDEA\ssm\ShareSystem\share-user-service && java -jar target\share-user-service-1.0.0.jar"
echo   Waiting for startup (15s)...
timeout /t 15 /nobreak

echo [2/3] Starting File Service (port 8182)...
start "FileService-8182" cmd /c "cd /d D:\IDEA\ssm\ShareSystem\share-file-service && java -jar target\share-file-service-1.0.0.jar"
echo   Waiting for startup (15s)...
timeout /t 15 /nobreak

echo [3/3] Starting Gateway (port 8180)...
start "Gateway-8180" cmd /c "cd /d D:\IDEA\ssm\ShareSystem\share-gateway && java -jar target\share-gateway-1.0.0.jar"

echo.
echo ========================================
echo   All services are starting up!
echo.
echo   Gateway : http://localhost:8180
echo   User    : http://localhost:8181
echo   File    : http://localhost:8182
echo.
echo   Admin   : http://localhost:8180/pages/admin.html
echo ========================================
echo.
echo   (Close the service windows to stop)
echo   Press any key to close this window...
pause >nul
