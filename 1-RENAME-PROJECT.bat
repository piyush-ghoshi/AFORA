@echo off
echo ========================================
echo AFORA Project Rename Script Launcher
echo ========================================
echo.
echo This will rename all "academia" references to "afora"
echo.
echo IMPORTANT:
echo   1. Close Android Studio first
echo   2. Ensure git is committed
echo   3. A backup will be created automatically
echo.
pause
echo.
echo Running rename script...
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0rename-academia-to-afora.ps1"
echo.
pause
