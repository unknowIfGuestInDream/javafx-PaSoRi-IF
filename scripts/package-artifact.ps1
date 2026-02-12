<# 
.SYNOPSIS
    Package the staging directory into a distributable zip.

.DESCRIPTION
    Compresses the staging directory into a zip file in the dist directory.
    This script should be called from the project root directory after resolve-version.ps1.
    Requires APP_VERSION environment variable to be set.
#>

$ErrorActionPreference = 'Stop'

# Resolve version
$version = $env:APP_VERSION
if (-not $version) {
    # Fallback: extract version from Maven if not set in environment
    $version = & mvn -q -DforceStdout 'help:evaluate' -Dexpression='project.version' 2>$null
    $version = $version.Trim()
    if (-not $version) { throw 'APP_VERSION not set and failed to extract version from Maven' }
}

Write-Host "Packaging version: $version" -ForegroundColor Cyan

# Ensure dist directory exists
if (-not (Test-Path dist)) { New-Item -ItemType Directory -Path dist | Out-Null }

# Create zip archive
$zipName = "Pasori-if-windows-$version.zip"
Compress-Archive -Path 'staging\*' -DestinationPath "dist/$zipName" -Force
Write-Host "Created dist/$zipName" -ForegroundColor Green

# Cleanup staging
Remove-Item -Path 'staging' -Recurse -Force
Write-Host "Staging directory cleaned up." -ForegroundColor Gray
