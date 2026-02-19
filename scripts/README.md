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
Push-Location staging
..\scripts\jre.ps1
Pop-Location
```

Uses `jdeps` and `jlink` to create a minimal custom JRE in the staging directory.
The script analyzes the application jar for required JDK modules and creates
a custom runtime image with only those modules, significantly reducing the
JRE size compared to a full JRE download.

### 4. Package artifact

```powershell
.\scripts\package-artifact.ps1
```

Compresses the staging directory into `dist/Pasori-if-windows-<version>.zip` and cleans up the staging directory.
