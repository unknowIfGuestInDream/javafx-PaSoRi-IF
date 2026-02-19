<# 
.SYNOPSIS
    Create a minimal custom JRE using jlink.

.DESCRIPTION
    Uses jdeps to analyze the application jar for required JDK modules,
    then creates a custom runtime image with jlink containing only those modules.
    This significantly reduces the JRE size compared to a full JRE download.
    This script should be called from the staging directory containing the application jar.
    Requires JDK 21+ with jlink and jdeps on the PATH.
#>

$ErrorActionPreference = 'Stop'

# Find the application jar in the current directory
$jar = Get-ChildItem -Path '*.jar' -File | Select-Object -First 1
if ($null -eq $jar) { throw 'No jar file found in current directory' }

Write-Host "Analyzing module dependencies for $($jar.Name)..." -ForegroundColor Cyan

# Use jdeps to determine required JDK modules from the fat jar
$modules = $null
$jdepsErr = $null
try {
    $modules = & jdeps --ignore-missing-deps --multi-release 21 --print-module-deps $jar.Name 2>&1 |
        Where-Object { $_ -is [string] } | Select-Object -Last 1
    if ($LASTEXITCODE -ne 0) { $modules = $null }
} catch {
    $jdepsErr = $_.Exception.Message
    $modules = $null
}

if (-not $modules -or $modules.Trim() -eq '') {
    # Fallback: conservative set covering JavaFX, serial I/O, JNA, logging (Logback),
    # Preferences API, XML processing, and sun.misc.Unsafe access.
    # Derived from module-info.java requires and transitive runtime dependencies.
    $modules = 'java.base,java.desktop,java.logging,java.management,java.naming,java.prefs,java.xml,jdk.unsupported'
    Write-Host "jdeps analysis failed, using fallback modules: $modules" -ForegroundColor Yellow
    if ($jdepsErr) { Write-Host "  Reason: $jdepsErr" -ForegroundColor Yellow }
} else {
    $modules = $modules.Trim()
    Write-Host "Detected modules: $modules" -ForegroundColor Cyan
}

# Remove existing jre directory if present
if (Test-Path 'jre') { Remove-Item -Path 'jre' -Recurse -Force }

Write-Host "Creating custom JRE with jlink..." -ForegroundColor Cyan

# Create custom JRE with size optimizations:
#   --strip-debug:    Remove debug info to reduce size
#   --no-man-pages:   Exclude man pages
#   --no-header-files: Exclude C header files
#   --compress zip-9: Maximum compression
& jlink --add-modules $modules --output jre --strip-debug --no-man-pages --no-header-files --compress zip-9
if ($LASTEXITCODE -ne 0) { throw "jlink failed with exit code $LASTEXITCODE" }

Write-Host "Custom JRE created successfully." -ForegroundColor Green
