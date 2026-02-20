# Build & Package Scripts

Follow these steps in order to build and package the application manually.

## Prerequisites

- JDK 21 or later
- PowerShell 5.1+ (Windows)

## Steps

### 1. Build

```powershell
.\scripts\build.ps1
```

Builds the project using Maven wrapper with Aliyun mirror. Tests are skipped by default.
To include tests, use:

```powershell
.\scripts\build.ps1 -RunTests
```

### 2. Resolve version & prepare staging

```powershell
.\scripts\resolve-version.ps1
```

Extracts the Maven project version, locates the built jar, and copies all necessary files into the `staging/` directory.

### 3. Create Custom JRE

```powershell
.\scripts\jre.ps1 -StagingDir staging
```

Downloads the Adoptium JDK 21 for Windows x64, uses `jdeps` to analyze the application jar
for required JDK modules, then creates a minimal custom JRE with `jlink` containing only
those modules. The downloaded JDK is cleaned up after the custom JRE is created.

### 4. Package artifact

```powershell
.\scripts\package-artifact.ps1
```

Compresses the staging directory into `dist/Pasori-if-windows-<version>.zip` and cleans up the staging directory.
