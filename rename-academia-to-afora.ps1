# ============================================================================
# AFORA Project Rename Script: academia → afora
# ============================================================================
# This script renames all occurrences of "academia" to "afora" throughout
# the entire project, including:
# - Package names (com.academia.* → com.afora.*)
# - Class names (AcademiaApplication → AforaApplication)
# - Directory structures
# - Configuration files
# - Documentation
#
# IMPORTANT: Run this script from the project root directory
# BACKUP: This script creates a backup before making changes
# ============================================================================

$ErrorActionPreference = "Stop"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "AFORA Project Rename: academia → afora" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

# Get project root (current directory)
$projectRoot = Get-Location
Write-Host "Project root: $projectRoot" -ForegroundColor Green
Write-Host ""

# Create backup timestamp
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $projectRoot "backup_before_rename_$timestamp"

Write-Host "Step 0: Creating backup..." -ForegroundColor Yellow
Write-Host "Backup location: $backupDir" -ForegroundColor Gray

# Create backup of critical directories (excluding build outputs)
$itemsToBackup = @(
    "androidApp\src",
    "shared\src",
    "backend\src",
    "androidApp\build.gradle.kts",
    "shared\build.gradle.kts",
    "backend\build.gradle.kts",
    "build.gradle.kts",
    "settings.gradle.kts",
    "androidApp\src\main\AndroidManifest.xml"
)

New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

foreach ($item in $itemsToBackup) {
    $sourcePath = Join-Path $projectRoot $item
    if (Test-Path $sourcePath) {
        $destPath = Join-Path $backupDir $item
        $destDir = Split-Path $destPath -Parent
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        Copy-Item -Path $sourcePath -Destination $destPath -Recurse -Force
        Write-Host "  ✓ Backed up: $item" -ForegroundColor Gray
    }
}

Write-Host "✓ Backup completed!" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 1: Update file contents (package declarations, imports, references)
# ============================================================================

Write-Host "Step 1: Updating file contents..." -ForegroundColor Yellow

$replacements = @(
    # Package declarations
    @{ Pattern = 'package com\.academia\.'; Replacement = 'package com.afora.' }
    
    # Imports
    @{ Pattern = 'import com\.academia\.'; Replacement = 'import com.afora.' }
    
    # Qualified names in code
    @{ Pattern = 'com\.academia\.android'; Replacement = 'com.afora.android' }
    @{ Pattern = 'com\.academia\.shared'; Replacement = 'com.afora.shared' }
    @{ Pattern = 'com\.academia\.backend'; Replacement = 'com.afora.backend' }
    
    # Class names
    @{ Pattern = 'AcademiaApplication'; Replacement = 'AforaApplication' }
    @{ Pattern = 'AcademiaBackendApplication'; Replacement = 'AforaBackendApplication' }
    @{ Pattern = 'AcademiaTheme'; Replacement = 'AforaTheme' }
    @{ Pattern = 'academia_'; Replacement = 'afora_' }
    
    # XML namespace and package attributes
    @{ Pattern = 'com\.academia\.android'; Replacement = 'com.afora.android' }
    
    # Application ID
    @{ Pattern = 'applicationId = "com\.academia\.android"'; Replacement = 'applicationId = "com.afora.android"' }
    @{ Pattern = 'namespace = "com\.academia\.android"'; Replacement = 'namespace = "com.afora.android"' }
    @{ Pattern = 'namespace = "com\.academia\.shared"'; Replacement = 'namespace = "com.afora.shared"' }
    
    # Comments and documentation
    @{ Pattern = 'Smart Classroom Attendance Management'; Replacement = 'Smart Classroom Attendance Management' }
)

# File extensions to process
$fileExtensions = @('*.kt', '*.kts', '*.xml', '*.java', '*.gradle', '*.md', '*.yml', '*.yaml', '*.properties', '*.json')

# Directories to exclude
$excludeDirs = @('.gradle', '.idea', 'build', 'backup_*', '.git')

Write-Host "  Scanning for files to update..." -ForegroundColor Gray

# Get all files to process
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

$totalFiles = $filesToProcess.Count
Write-Host "  Found $totalFiles files to process" -ForegroundColor Gray
Write-Host ""

$processedCount = 0
$modifiedCount = 0

foreach ($file in $filesToProcess) {
    $processedCount++
    $relativePath = $file.FullName.Substring($projectRoot.Path.Length + 1)
    
    # Show progress every 10 files
    if ($processedCount % 10 -eq 0 -or $processedCount -eq $totalFiles) {
        Write-Progress -Activity "Updating file contents" -Status "$processedCount of $totalFiles files" -PercentComplete (($processedCount / $totalFiles) * 100)
    }
    
    try {
        $content = Get-Content -Path $file.FullName -Raw -ErrorAction Stop
        $originalContent = $content
        $modified = $false
        
        foreach ($replacement in $replacements) {
            if ($content -match $replacement.Pattern) {
                $content = $content -replace $replacement.Pattern, $replacement.Replacement
                $modified = $true
            }
        }
        
        if ($modified) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
            $modifiedCount++
            Write-Host "  ✓ Modified: $relativePath" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "  ✗ Error processing: $relativePath" -ForegroundColor Red
        Write-Host "    $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Progress -Activity "Updating file contents" -Completed

Write-Host ""
Write-Host "✓ Updated $modifiedCount files" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 2: Rename directories (package structure)
# ============================================================================

Write-Host "Step 2: Renaming directory structures..." -ForegroundColor Yellow

function Rename-PackageDirectory {
    param (
        [string]$basePath,
        [string]$oldPackage,
        [string]$newPackage
    )
    
    if (-not (Test-Path $basePath)) {
        Write-Host "  ⚠ Path not found: $basePath" -ForegroundColor Yellow
        return
    }
    
    # Find all 'academia' directories in the package structure
    $academiaDirs = Get-ChildItem -Path $basePath -Recurse -Directory -Filter "academia" -ErrorAction SilentlyContinue
    
    foreach ($dir in $academiaDirs) {
        $parentPath = Split-Path $dir.FullName -Parent
        $newDirPath = Join-Path $parentPath "afora"
        
        if (Test-Path $newDirPath) {
            Write-Host "  ⚠ Target directory already exists: $newDirPath" -ForegroundColor Yellow
            continue
        }
        
        try {
            Rename-Item -Path $dir.FullName -NewName "afora" -Force
            $relativePath = $dir.FullName.Substring($projectRoot.Path.Length + 1)
            Write-Host "  ✓ Renamed: $relativePath → afora" -ForegroundColor Green
        }
        catch {
            Write-Host "  ✗ Error renaming: $($dir.FullName)" -ForegroundColor Red
            Write-Host "    $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

# Rename package directories in all modules
$modulePaths = @(
    "androidApp\src\main\kotlin\com",
    "androidApp\src\test\kotlin\com",
    "shared\src\commonMain\kotlin\com",
    "shared\src\androidMain\kotlin\com",
    "shared\src\iosMain\kotlin\com",
    "backend\src\main\kotlin\com",
    "backend\src\test\kotlin\com"
)

foreach ($modulePath in $modulePaths) {
    $fullPath = Join-Path $projectRoot $modulePath
    Rename-PackageDirectory -basePath $fullPath -oldPackage "academia" -newPackage "afora"
}

Write-Host ""
Write-Host "✓ Directory renaming completed!" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Step 3: Rename specific files
# ============================================================================

Write-Host "Step 3: Renaming specific files..." -ForegroundColor Yellow

# Find and rename AcademiaApplication files
$appFiles = Get-ChildItem -Path $projectRoot -Recurse -Filter "*Academia*.kt" -ErrorAction SilentlyContinue |
    Where-Object {
        $exclude = $false
        foreach ($dir in $excludeDirs) {
            if ($_.FullName -like "*\$dir\*") {
                $exclude = $true
                break
            }
        }
        -not $exclude
    }

foreach ($file in $appFiles) {
    $newName = $file.Name -replace 'Academia', 'Afora'
    $newPath = Join-Path (Split-Path $file.FullName -Parent) $newName
    
    if ($file.Name -ne $newName) {
        try {
            Rename-Item -Path $file.FullName -NewName $newName -Force
            $relativePath = $file.FullName.Substring($projectRoot.Path.Length + 1)
            Write-Host "  ✓ Renamed: $relativePath → $newName" -ForegroundColor Green
        }
        catch {
            Write-Host "  ✗ Error renaming: $($file.FullName)" -ForegroundColor Red
            Write-Host "    $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "✓ File renaming completed!" -ForegroundColor Green
Write-Host ""

# ============================================================================
# Summary
# ============================================================================

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "Rename Operation Completed!" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Green
Write-Host "  • Backup created: $backupDir" -ForegroundColor Gray
Write-Host "  • Files modified: $modifiedCount" -ForegroundColor Gray
Write-Host "  • Package structure: com.academia.* → com.afora.*" -ForegroundColor Gray
Write-Host "  • Class names: Academia* → Afora*" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Review the changes in your IDE" -ForegroundColor Gray
Write-Host "  2. Clean the project: .\gradlew clean" -ForegroundColor Gray
Write-Host "  3. Rebuild: .\gradlew build" -ForegroundColor Gray
Write-Host "  4. Run tests: .\gradlew test" -ForegroundColor Gray
Write-Host "  5. If issues occur, restore from: $backupDir" -ForegroundColor Gray
Write-Host ""
Write-Host "============================================================================" -ForegroundColor Cyan

# Pause to let user see the results
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
