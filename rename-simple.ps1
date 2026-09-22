# Simplified AFORA Project Rename Script: academia → afora
# This version is more straightforward and reliable

$ErrorActionPreference = "Stop"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "AFORA Project Rename: academia → afora" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Get-Location
Write-Host "Project root: $projectRoot" -ForegroundColor Green
Write-Host ""

# Create backup
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $projectRoot "backup_before_rename_$timestamp"

Write-Host "Creating backup at: $backupDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

$itemsToBackup = @("androidApp\src", "shared\src", "backend\src", "*.gradle.kts", "androidApp\src\main\AndroidManifest.xml")
foreach ($item in $itemsToBackup) {
    $sources = Get-ChildItem -Path $projectRoot -Filter $item -Recurse -ErrorAction SilentlyContinue | 
        Where-Object { $_.FullName -notlike "*\build\*" -and $_.FullName -notlike "*\.gradle\*" }
    
    foreach ($source in $sources) {
        $relativePath = $source.FullName.Substring($projectRoot.Path.Length + 1)
        $destPath = Join-Path $backupDir $relativePath
        $destDir = Split-Path $destPath -Parent
        New-Item -ItemType Directory -Path $destDir -Force -ErrorAction SilentlyContinue | Out-Null
        Copy-Item -Path $source.FullName -Destination $destPath -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "✓ Backup completed" -ForegroundColor Green
Write-Host ""

# Step 1: Update file contents
Write-Host "Step 1: Updating file contents..." -ForegroundColor Yellow

$fileExtensions = @('*.kt', '*.kts', '*.xml', '*.java', '*.yml', '*.yaml', '*.properties', '*.md')
$excludeDirs = @('.gradle', '.idea', 'build', 'backup_*', '.git')

$filesToProcess = Get-ChildItem -Path $projectRoot -Recurse -Include $fileExtensions | 
    Where-Object { 
        $file = $_
        $exclude = $false
        foreach ($dir in $excludeDirs) {
            if ($file.FullName -like "*\$dir\*") {
                $exclude = $true
                break
            }
        }
        -not $exclude
    }

$modifiedCount = 0

foreach ($file in $filesToProcess) {
    try {
        $content = Get-Content -Path $file.FullName -Raw -ErrorAction Stop
        $originalContent = $content
        
        # Replace all academia → afora patterns
        $content = $content -replace 'package com\.academia\.', 'package com.afora.'
        $content = $content -replace 'import com\.academia\.', 'import com.afora.'
        $content = $content -replace 'com\.academia\.android', 'com.afora.android'
        $content = $content -replace 'com\.academia\.shared', 'com.afora.shared'
        $content = $content -replace 'com\.academia\.backend', 'com.afora.backend'
        $content = $content -replace 'AcademiaApplication', 'AforaApplication'
        $content = $content -replace 'AcademiaBackendApplication', 'AforaBackendApplication'
        $content = $content -replace 'AcademiaTheme', 'AforaTheme'
        $content = $content -replace 'applicationId = "com\.academia\.android"', 'applicationId = "com.afora.android"'
        $content = $content -replace 'namespace = "com\.academia\.android"', 'namespace = "com.afora.android"'
        $content = $content -replace 'namespace = "com\.academia\.shared"', 'namespace = "com.afora.shared"'
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
            $modifiedCount++
            $relativePath = $file.FullName.Substring($projectRoot.Path.Length + 1)
            Write-Host "  ✓ $relativePath" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "  ✗ Error: $($file.FullName)" -ForegroundColor Red
    }
}

Write-Host "✓ Updated $modifiedCount files" -ForegroundColor Green
Write-Host ""

# Step 2: Rename directories
Write-Host "Step 2: Renaming directories..." -ForegroundColor Yellow

$academiaDirs = Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter "academia" -ErrorAction SilentlyContinue |
    Where-Object {
        $dir = $_
        $exclude = $false
        foreach ($excludeDir in $excludeDirs) {
            if ($dir.FullName -like "*\$excludeDir\*") {
                $exclude = $true
                break
            }
        }
        -not $exclude
    }

foreach ($dir in $academiaDirs) {
    try {
        $parentPath = Split-Path $dir.FullName -Parent
        $newDirPath = Join-Path $parentPath "afora"
        
        if (-not (Test-Path $newDirPath)) {
            Rename-Item -Path $dir.FullName -NewName "afora" -Force
            $relativePath = $dir.FullName.Substring($projectRoot.Path.Length + 1)
            Write-Host "  ✓ $relativePath → afora" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "  ✗ Error: $($dir.FullName)" -ForegroundColor Red
    }
}

Write-Host "✓ Directory renaming completed" -ForegroundColor Green
Write-Host ""

# Step 3: Rename files
Write-Host "Step 3: Renaming files..." -ForegroundColor Yellow

$appFiles = Get-ChildItem -Path $projectRoot -Recurse -Filter "*Academia*.kt" -ErrorAction SilentlyContinue |
    Where-Object {
        $file = $_
        $exclude = $false
        foreach ($dir in $excludeDirs) {
            if ($file.FullName -like "*\$dir\*") {
                $exclude = $true
                break
            }
        }
        -not $exclude
    }

foreach ($file in $appFiles) {
    $newName = $file.Name -replace 'Academia', 'Afora'
    if ($file.Name -ne $newName) {
        try {
            Rename-Item -Path $file.FullName -NewName $newName -Force
            $relativePath = $file.FullName.Substring($projectRoot.Path.Length + 1)
            Write-Host "  ✓ $relativePath → $newName" -ForegroundColor Green
        }
        catch {
            Write-Host "  ✗ Error: $($file.FullName)" -ForegroundColor Red
        }
    }
}

Write-Host "✓ File renaming completed" -ForegroundColor Green
Write-Host ""

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "Rename Operation Completed!" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Green
Write-Host "  • Backup: $backupDir" -ForegroundColor Gray
Write-Host "  • Files modified: $modifiedCount" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. .\gradlew clean" -ForegroundColor Gray
Write-Host "  2. .\gradlew build" -ForegroundColor Gray
Write-Host ""
