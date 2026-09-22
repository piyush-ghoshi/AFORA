@echo off
echo ========================================
echo AFORA Rename Verification Script
echo ========================================
echo.
echo This will verify the rename was successful
echo.
pause
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0verify-rename.ps1"
echo.
pause
