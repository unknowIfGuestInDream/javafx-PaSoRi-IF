# Quick Start (Non-Java Users)

If you are not familiar with Java development, the easiest way to get a working application is to download a pre-built release or build the distributable package using the included scripts.

## Option 1: Download a Release

Check the [Releases](https://github.com/unknowIfGuestInDream/javafx-PaSoRi-IF/releases) page for pre-built zip files. Extract and run `start.bat` — no Java installation required.

## Option 2: Build from Source (No Java Knowledge Required)

The project includes PowerShell scripts that automate the entire build process on Windows:

```powershell
# 1. Install JDK 21 (e.g., from https://adoptium.net/) and ensure 'java' is on PATH

# 2. Clone this repository
git clone https://github.com/unknowIfGuestInDream/javafx-PaSoRi-IF.git
cd javafx-PaSoRi-IF

# 3. Build the application (Maven wrapper is included, no separate Maven install needed)
.\scripts\build.ps1

# 4. Prepare and package a self-contained distributable (downloads a JRE automatically)
.\scripts\resolve-version.ps1
.\scripts\jre.ps1 -StagingDir staging
.\scripts\package-artifact.ps1

# The result is in dist/Pasori-if-windows-<version>.zip
# Extract it anywhere and run start.bat
```

See [scripts/README.md](../scripts/README.md) for detailed descriptions of each step.
