<# 
.SYNOPSIS
    Build and package the JavaFX PaSoRi IF application.

.DESCRIPTION
    Uses Maven wrapper to build the project with Chinese mirror for faster downloads in CN.
    This script should be called from the project root directory.
#>
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

# Maven China mirror settings (Aliyun)
$settingsContent = @"
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun-central</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Central Mirror</name>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
    <mirror>
      <id>aliyun-public</id>
      <mirrorOf>public</mirrorOf>
      <name>Aliyun Maven Public Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
"@

# Write temporary settings file
$settingsFile = Join-Path $PSScriptRoot '..' '.mvn' 'settings.xml'
$settingsDir = Split-Path $settingsFile -Parent
if (-not (Test-Path $settingsDir)) { New-Item -ItemType Directory -Path $settingsDir | Out-Null }
$settingsContent | Out-File -FilePath $settingsFile -Encoding utf8 -Force

# Build command
$mvnArgs = @('clean', 'package', '-s', $settingsFile)
if ($SkipTests) {
    $mvnArgs += '-DskipTests'
}

Write-Host "Building with Maven wrapper..." -ForegroundColor Cyan
Write-Host "  Arguments: $($mvnArgs -join ' ')" -ForegroundColor Gray

$mvnw = Join-Path $PSScriptRoot '..' 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    Write-Host "Maven wrapper not found at $mvnw, falling back to mvn" -ForegroundColor Yellow
    $mvnw = 'mvn'
}

& $mvnw @mvnArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

Write-Host "Build completed successfully." -ForegroundColor Green
