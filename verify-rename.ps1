# ============================================================================
# AFORA Project Rename Verification Script
# ============================================================================
# This script verifies that the academia → afora rename was successful
# by checking for any remaining "academia" references that should have
# been renamed.
# ============================================================================

$ErrorActionPreference = "Continue"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "AFORA Rename Verification" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Get-Location
Write-Host "Scanning project: $projectRoot" -ForegroundColor Green
Write-Host ""

# Directories to exclude
$excludeDirs = @('.gradle', '.idea', 'build', 'backup_*', '.git')

# File extensions to check
$fileExtensions = @('*.kt', '*.kts', '*.xml', '*.java', '*.gradle')

Write-Host "Step 1: Checking for remaining 'academia' references..." -ForegroundColor Yellow
Write-Host ""

# Get all files to scan
$filesToScan = Get-ChildItem -Path $projectRoot -Recurse -Include $fileExtensions | 
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

$issuesFound = @()
$checkedFiles = 0

foreach ($file in $filesToScan) {
    $checkedFiles++
    $relativePath = $file.FullName.Substring($projectRoot.Path.Length + 1)
    
    try {
        $content = Get-Content -Path $file.FullName -Raw -ErrorAction Stop
        
        # Check for academia references (case-insensitive)
        if ($content -match 'academia' -and $content -notmatch 'Smart Classroom Attendance Management') {
            $lines = Get-Content -Path $file.FullName
            $lineNumber = 0
            
            foreach ($line in $lines) {
                $lineNumber++
                if ($line -match 'academia') {
                    $issuesFound += [PSCustomObject]@{
                        File = $relativePath
                        Line = $lineNumber
                        Content = $line.Trim()
                    }
                }
            }
        }
    }
    catch {
        Write-Host "  ⚠ Error reading: $relativePath" -ForegroundColor Yellow
    }
}

Write-Host "Checked $checkedFiles files" -ForegroundColor Gray
Write-Host ""

if ($issuesFound.Count -eq 0) {
    Write-Host "✓ SUCCESS: No remaining 'academia' references found!" -ForegroundColor Green
} else {
    Write-Host "⚠ WARNING: Found $($issuesFound.Count) potential 'academia' references:" -ForegroundColor Yellow
    Write-Host ""
    
    $issuesFound | ForEach-Object {
        Write-Host "  File: $($_.File)" -ForegroundColor Yellow
        Write-Host "  Line $($_.Line): $($_.Content)" -ForegroundColor Gray
        Write-Host ""
    }
    
    Write-Host "These may need manual review." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "Step 2: Verifying package structure..." -ForegroundColor Yellow
Write-Host ""

# Check if old 'academia' directories still exist
$oldDirs = Get-ChildItem -Path $projectRoot -Recurse -Directory -Filter "academia" -ErrorAction SilentlyContinue |
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

if ($oldDirs.Count -eq 0) {
    Write-Host "✓ SUCCESS: No 'academia' directories found" -ForegroundColor Green
} else {
    Write-Host "⚠ WARNING: Found $($oldDirs.Count) 'academia' directories:" -ForegroundColor Yellow
    foreach ($dir in $oldDirs) {
        $relativePath = $dir.FullName.Substring($projectRoot.Path.Length + 1)
        Write-Host "  • $relativePath" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "============================================================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "Step 3: Checking key configuration files..." -ForegroundColor Yellow
Write-Host ""

$configChecks = @(
    @{
        File = "androidApp\build.gradle.kts"
        ExpectedPattern = 'namespace = "com\.afora\.android"'
        Description = "Android namespace"
    },
    @{
        File = "androidApp\build.gradle.kts"
        ExpectedPattern = 'applicationId = "com\.afora\.android"'
        Description = "Application ID"
    },
    @{
        File = "androidApp\src\main\AndroidManifest.xml"
        ExpectedPattern = 'com\.afora\.android'
        Description = "Manifest package"
    },
    @{
        File = "shared\build.gradle.kts"
        ExpectedPattern = 'namespace = "com\.afora\.shared"'
        Description = "Shared module namespace"
    }
)

$configIssues = 0

foreach ($check in $configChecks) {
    $filePath = Join-Path $projectRoot $check.File
    
    if (Test-Path $filePath) {
        $content = Get-Content -Path $filePath -Raw
        
        if ($content -match $check.ExpectedPattern) {
            Write-Host "  ✓ $($check.Description)" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($check.Description) - NOT FOUND" -ForegroundColor Red
            $configIssues++
        }
    } else {
        Write-Host "  ⚠ $($check.File) - FILE NOT FOUND" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "============================================================================" -ForegroundColor Cyan

# Final summary
Write-Host ""
Write-Host "Verification Summary:" -ForegroundColor Cyan
Write-Host ""

$totalIssues = $issuesFound.Count + $oldDirs.Count + $configIssues

if ($totalIssues -eq 0) {
    Write-Host "  ✓ All checks passed!" -ForegroundColor Green
    Write-Host "  ✓ Rename appears to be successful" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Run: .\gradlew clean build" -ForegroundColor Gray
    Write-Host "  2. Run: .\gradlew test" -ForegroundColor Gray
    Write-Host "  3. Open in Android Studio and verify" -ForegroundColor Gray
} else {
    Write-Host "  ⚠ Found $totalIssues potential issues" -ForegroundColor Yellow
    Write-Host "  • Content references: $($issuesFound.Count)" -ForegroundColor Gray
    Write-Host "  • Old directories: $($oldDirs.Count)" -ForegroundColor Gray
    Write-Host "  • Config issues: $configIssues" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Please review the issues above and fix manually if needed." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
