# Quick JDK 17 Setup Script for AFORA
# Helps download and configure JDK 17 for Phase A1 builds

Write-Host "`n=== AFORA JDK 17 Setup ===" -ForegroundColor Cyan
Write-Host "Phase A1 requires JDK 17 (current system has Java 26)`n" -ForegroundColor Yellow

# Check if JDK 17 already exists
$jdk17Paths = @(
    "C:\Program Files\Eclipse Adoptium\jdk-17*",
    "C:\Program Files\Java\jdk-17*",
    "C:\Program Files\Microsoft\jdk-17*"
)

$found = $false
foreach ($path in $jdk17Paths) {
    $existing = Get-Item $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($existing) {
        Write-Host "Found JDK 17: $($existing.FullName)" -ForegroundColor Green
        $env:JAVA_HOME = $existing.FullName
        Write-Host "Testing..." -ForegroundColor Yellow
        & "$($existing.FullName)\bin\java.exe" -version
        $found = $true
        break
    }
}

if (-not $found) {
    Write-Host "JDK 17 not found. Downloading..." -ForegroundColor Yellow
    Write-Host "`nOption 1: Download from browser (recommended)" -ForegroundColor Cyan
    Write-Host "URL: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor White
    Write-Host "Download: Windows x64 MSI installer" -ForegroundColor White
    
    Write-Host "`nOption 2: Use winget (if available)" -ForegroundColor Cyan
    Write-Host "Command: winget install EclipseAdoptium.Temurin.17.JDK" -ForegroundColor White
    
    $choice = Read-Host "`nTry winget install? (y/n)"
    
    if ($choice -eq 'y') {
        Write-Host "`nInstalling JDK 17 via winget..." -ForegroundColor Yellow
        winget install EclipseAdoptium.Temurin.17.JDK
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Installation complete! Restart PowerShell and run this script again." -ForegroundColor Green
        } else {
            Write-Host "winget failed. Please download manually from:" -ForegroundColor Red
            Write-Host "https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Yellow
        }
    } else {
        Write-Host "`nPlease download JDK 17 manually:" -ForegroundColor Yellow
        Write-Host "1. Go to: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor White
        Write-Host "2. Download Windows x64 MSI" -ForegroundColor White
        Write-Host "3. Run installer" -ForegroundColor White
        Write-Host "4. Run this script again" -ForegroundColor White
    }
    
    exit
}

# Update gradle.properties
Write-Host "`nUpdating gradle.properties..." -ForegroundColor Yellow
$gradleProps = Get-Content "gradle.properties"
$newContent = $gradleProps -replace 'org\.gradle\.java\.home=.*', "org.gradle.java.home=$($env:JAVA_HOME -replace '\\','\\')"
$newContent | Set-Content "gradle.properties"
Write-Host "Updated gradle.properties" -ForegroundColor Green

# Set environment variable permanently
Write-Host "`nSetting JAVA_HOME permanently..." -ForegroundColor Yellow
try {
    [System.Environment]::SetEnvironmentVariable('JAVA_HOME', $env:JAVA_HOME, 'Machine')
    Write-Host "JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
    Write-Host "(Requires admin rights - may fail, but gradle.properties is updated)" -ForegroundColor Gray
} catch {
    Write-Host "Could not set system JAVA_HOME (need admin). Using gradle.properties." -ForegroundColor Yellow
}

Write-Host "`n=== Next Steps ===" -ForegroundColor Cyan
Write-Host "1. Restart PowerShell (to load new JAVA_HOME)" -ForegroundColor White
Write-Host "2. Run: cd c:\Users\piyus\Desktop\AFORA" -ForegroundColor White
Write-Host "3. Run: .\gradlew :shared:build" -ForegroundColor White
Write-Host "4. Run: .\gradlew :androidApp:assembleDebug" -ForegroundColor White
Write-Host "5. Run: cd backend; .\gradlew build" -ForegroundColor White

Write-Host "`nSee BUILD_VERIFICATION.md for complete build guide" -ForegroundColor Gray
