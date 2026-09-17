# Minimal Gradle initialization script
# This downloads only what's needed (~100MB) and generates the wrapper

Write-Host "Initializing Gradle wrapper (space-efficient method)..." -ForegroundColor Green

# Check if Java is installed
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found. Please install JDK 17 first." -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Yellow
    exit 1
}

# Use gradlew to auto-download Gradle on first run
Write-Host "Running gradlew to auto-initialize..." -ForegroundColor Yellow
Write-Host "This will download Gradle (~100MB) to user cache, not project folder" -ForegroundColor Cyan

# The gradlew.bat script will auto-download Gradle to %USERPROFILE%\.gradle
.\gradlew.bat --version

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ Gradle initialized successfully!" -ForegroundColor Green
    Write-Host "Gradle cached in: $env:USERPROFILE\.gradle" -ForegroundColor Cyan
    Write-Host "`nYou can now run: .\gradlew :shared:build" -ForegroundColor Yellow
} else {
    Write-Host "`n✗ Initialization failed" -ForegroundColor Red
}
