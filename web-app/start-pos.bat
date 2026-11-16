@echo off
cd /d "%~dp0"
title Mobile Repair Shop POS - Local Server

set "PATH=%ProgramFiles%\nodejs;%ProgramFiles(x86)%\nodejs;%LocalAppData%\Programs\nodejs;%PATH%"

where node >nul 2>nul
if not errorlevel 1 goto NODE_READY

echo [1/4] Node.js not found. Installing Node.js LTS automatically...

where winget >nul 2>nul
if errorlevel 1 goto INSTALL_MSI

echo Trying Windows Package Manager - winget...
winget install --id OpenJS.NodeJS.LTS -e --source winget --accept-package-agreements --accept-source-agreements
set "PATH=%ProgramFiles%\nodejs;%ProgramFiles(x86)%\nodejs;%LocalAppData%\Programs\nodejs;%PATH%"
where node >nul 2>nul
if not errorlevel 1 goto NODE_READY

:INSTALL_MSI
set "NODE_ARCH=x64"
if /i "%PROCESSOR_ARCHITECTURE%"=="x86" if not defined PROCESSOR_ARCHITEW6432 set "NODE_ARCH=x86"
if /i "%PROCESSOR_ARCHITECTURE%"=="ARM64" set "NODE_ARCH=arm64"

echo Downloading official Node.js LTS %NODE_ARCH% installer...
set "NODE_MSI=%TEMP%\nodejs-lts-installer.msi"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $r = Invoke-RestMethod 'https://nodejs.org/dist/index.json'; $v = ($r | Where-Object { $_.lts } | Select-Object -First 1).version; if (-not $v) { throw 'No LTS release found' }; $u = 'https://nodejs.org/dist/' + $v + '/node-' + $v + '-' + $env:NODE_ARCH + '.msi'; Write-Host ('Downloading ' + $u); Invoke-WebRequest -Uri $u -OutFile $env:NODE_MSI"
if errorlevel 1 goto DOWNLOAD_FAILED

echo Running Node.js LTS installer...
msiexec /i "%NODE_MSI%" /passive /norestart
set "MSI_ERR=%errorlevel%"
if exist "%NODE_MSI%" del /f /q "%NODE_MSI%" >nul 2>nul
if "%MSI_ERR%"=="0" goto CHECK_NODE_AFTER_INSTALL
if "%MSI_ERR%"=="3010" goto CHECK_NODE_AFTER_INSTALL
echo [ERROR] Node.js installer exited with code %MSI_ERR%.
goto FAIL_PAUSE

:CHECK_NODE_AFTER_INSTALL
set "PATH=%ProgramFiles%\nodejs;%ProgramFiles(x86)%\nodejs;%LocalAppData%\Programs\nodejs;%PATH%"
where node >nul 2>nul
if not errorlevel 1 goto NODE_READY
echo [INFO] Node.js was installed. Please close this window and double-click start-pos.bat once more.
goto FAIL_PAUSE

:DOWNLOAD_FAILED
echo [ERROR] Could not download Node.js automatically. Check internet connection or install from https://nodejs.org/
goto FAIL_PAUSE

:NODE_READY
for /f "delims=" %%V in ('node -v') do set "NODE_VER=%%V"
echo [OK] Node.js %NODE_VER% ready.

netstat -ano | findstr ":3000 " | findstr "LISTENING" >nul 2>nul
if not errorlevel 1 goto ALREADY_RUNNING

if exist "node_modules\.bin\vite.cmd" goto CHECK_DIST
echo [2/4] Installing project dependencies - first-time setup...
call npm.cmd install --legacy-peer-deps
if errorlevel 1 goto NPM_INSTALL_FAILED

:CHECK_DIST
if exist "dist\index.html" goto START_SERVER
echo [3/4] Building Mobile Repair Shop POS...
call npm.cmd run build
if errorlevel 1 goto BUILD_FAILED

:START_SERVER
echo.
echo [4/4] Starting Mobile Repair Shop POS on http://localhost:3000 ...
echo Keep this window open while using the POS. Close it when shutting down.
echo.
start "" "http://localhost:3000"
call npm.cmd run preview
if errorlevel 1 goto PREVIEW_FAILED
goto END_PAUSE

:ALREADY_RUNNING
echo POS server is already running on http://localhost:3000
start "" "http://localhost:3000"
timeout /t 3 >nul
exit /b 0

:NPM_INSTALL_FAILED
echo [ERROR] npm install failed. Check your internet connection and try again.
goto FAIL_PAUSE

:BUILD_FAILED
echo [ERROR] npm run build failed. Check errors above.
goto FAIL_PAUSE

:PREVIEW_FAILED
echo [ERROR] Local server stopped unexpectedly.
goto FAIL_PAUSE

:FAIL_PAUSE
echo.
pause
exit /b 1

:END_PAUSE
echo.
pause
exit /b 0
