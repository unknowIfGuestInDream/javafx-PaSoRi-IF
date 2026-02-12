<# 
.SYNOPSIS
    Resolve the project version and prepare staging directory.

.DESCRIPTION
    Extracts the Maven project version, locates the built jar, and copies
    all files needed for packaging into a staging directory.
    This script should be called from the project root directory after build.ps1.
#>

$ErrorActionPreference = 'Stop'

# Resolve version from pom.xml
$versionOutput = & mvn -q -DforceStdout 'help:evaluate' -Dexpression='project.version' 2>$null
$version = $versionOutput.Trim()
if (-not $version) { throw 'Failed to extract version from Maven' }
Write-Host "Project version: $version" -ForegroundColor Cyan

# Locate jar file
$jar = Get-ChildItem -Path 'target\javafx-pasori-if.jar' -File -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $jar) { throw 'No jar file found matching pattern target\javafx-pasori-if.jar' }
Write-Host "Jar file: $($jar.Name)" -ForegroundColor Cyan

# Set environment variables for subsequent steps (GitHub Actions compatible)
if ($env:GITHUB_ENV) {
    Add-Content -Path $env:GITHUB_ENV -Value "APP_VERSION=$version" -Encoding utf8
    Add-Content -Path $env:GITHUB_ENV -Value "APP_JAR=$($jar.Name)" -Encoding utf8
}

# Create staging directory
$stagingDir = 'staging'
if (Test-Path $stagingDir) { Remove-Item -Path $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir | Out-Null

# Copy files to staging
Copy-Item -Path $jar.FullName -Destination $stagingDir
Copy-Item -Path 'README.md' -Destination $stagingDir
Copy-Item -Path 'scripts\windows\*' -Destination $stagingDir -Recurse

Write-Host "Staging directory prepared at: $stagingDir" -ForegroundColor Green
