# Bootstrap Gradle - Minimal Download
Write-Host "`n=== Gradle Bootstrap (Space-Saving) ===" -ForegroundColor Cyan
Write-Host "Downloading only 60KB wrapper jar`n" -ForegroundColor Green

$gradleVersion = "8.4"
$mavenUrl = "https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/$gradleVersion/gradle-wrapper-$gradleVersion.jar"
$destPath = "gradle\wrapper\gradle-wrapper.jar"

Write-Host "Downloading gradle-wrapper.jar..." -ForegroundColor Yellow

try {
    New-Item -ItemType Directory -Path "gradle\wrapper" -Force | Out-Null
    Invoke-WebRequest -Uri $mavenUrl -OutFile $destPath -UseBasicParsing
    
    $fileSize = [math]::Round((Get-Item $destPath).Length / 1KB, 2)
    Write-Host "Success! Downloaded $fileSize KB" -ForegroundColor Green
    Write-Host "File: $destPath" -ForegroundColor Green
    
    Write-Host "`nCopying to backend..." -ForegroundColor Yellow
    Copy-Item $destPath -Destination "backend\gradle\wrapper\gradle-wrapper.jar" -Force
    Write-Host "Backend wrapper ready" -ForegroundColor Green
    
    Write-Host "`n=== Next Steps ===" -ForegroundColor Cyan
    Write-Host "1. Run: .\gradlew --version" -ForegroundColor White
    Write-Host "2. Then: .\gradlew :shared:build" -ForegroundColor White
    
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}
