@echo off
setlocal enabledelayedexpansion
title ShareSystem - Database Init

set DIR=%~dp0
set SQL_FILE=%DIR%sql\init.sql
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=share_system
set DB_USER=root
set DB_PASS=112233

echo =============================================
echo    ShareSystem Database Init Tool
echo =============================================
echo.

echo [1] Detecting MySQL ...

set MYSQL_EXE=

if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
if exist "D:\mysql\bin\mysql.exe" set "MYSQL_EXE=D:\mysql\bin\mysql.exe"
if exist "D:\mysql-8.0\bin\mysql.exe" set "MYSQL_EXE=D:\mysql-8.0\bin\mysql.exe"
if exist "D:\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
if exist "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

REM Try PATH
where mysql.exe >nul 2>&1
if !ERRORLEVEL! equ 0 (
    for /f "delims=" %%M in ('where mysql.exe 2^>nul') do (
        set "MYSQL_EXE=%%M"
    )
)

if "!MYSQL_EXE!"=="" (
    echo.
    echo   [ERROR] mysql.exe not found!
    echo.
    echo   Please make sure MySQL is installed.
    echo   Common install paths checked:
    echo     C:\Program Files\MySQL\MySQL Server 8.0\bin\
    echo     C:\Program Files\MySQL\MySQL Server 8.4\bin\
    echo     D:\mysql\bin\
    echo.
    echo   If MySQL is installed elsewhere, open this .bat
    echo   file and add your path to the detection list.
    echo.
    pause
    exit /b 1
)

echo   Found: !MYSQL_EXE!

echo.
echo [2] Testing connection to !DB_HOST!:!DB_PORT! ...
"!MYSQL_EXE!" -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! -e "SELECT 1 AS connection_ok;" 2>nul
if !ERRORLEVEL! neq 0 (
    echo   [ERROR] Cannot connect to MySQL!
    echo.
    echo   Check:
    echo     1. MySQL service is running?
    echo        (Run: net start MySQL   or   services.msc)
    echo     2. Credentials: root / 112233
    echo     3. Port: !DB_PORT!
    echo.
    pause
    exit /b 1
)
echo   Connection OK.

echo.
echo [3] Creating database ...
"!MYSQL_EXE!" -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! -e "DROP DATABASE IF EXISTS !DB_NAME!; CREATE DATABASE !DB_NAME! CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>nul
if !ERRORLEVEL! neq 0 (
    echo   [ERROR] Failed to create database!
    pause
    exit /b 1
)
echo   Database !DB_NAME! created.

echo.
echo [4] Importing tables and seed data ...
if not exist "!SQL_FILE!" (
    echo   [ERROR] SQL file missing: !SQL_FILE!
    pause
    exit /b 1
)
"!MYSQL_EXE!" -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! --default-character-set=utf8mb4 !DB_NAME! < "!SQL_FILE!" 2>&1
if !ERRORLEVEL! neq 0 (
    echo   [ERROR] SQL import failed!
    echo   Try running init.sql manually in MySQL Workbench.
    pause
    exit /b 1
)
echo   Tables and data imported.

echo.
echo [5] Verifying ...
echo.
echo   --- Users ---
"!MYSQL_EXE!" -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! -e "SELECT id, username, nickname, role FROM !DB_NAME!.user;"
echo.
echo   --- Tables ---
"!MYSQL_EXE!" -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! -e "SHOW TABLES FROM !DB_NAME!;"

echo.
echo =============================================
echo   DONE! Database is ready.
echo =============================================
echo.
echo   Database : !DB_NAME!
echo   Admin    : admin / admin123
echo   Test     : test  / test123
echo.
echo   Next step: run the launcher to start services.
echo.
pause
