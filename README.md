# PaSoRi IF Tool

JavaFX tool that acts as a relay (中継器) between a Sony PaSoRi NFC reader/writer and a Panasonic USB CDC-ACM (アンテナIF) device, using the IF communication protocol over serial port.

## Overview

This application bridges two communication interfaces:

- **PaSoRi side**: Communicates with the Sony PaSoRi NFC reader/writer via the FeliCa SDK (felica.dll) through JNA. Supports card polling, reading, writing, and raw FeliCa command passthrough.
- **IF side**: Communicates with the Panasonic antenna IF device via USB CDC-ACM serial port using a framed binary protocol (IF protocol).

The relay receives IF protocol commands from the serial port, executes the corresponding NFC operations via the FeliCa SDK, and returns IF protocol responses back to the serial port.

## IF Communication Protocol

### Message Format

| D0  | D1      | D2  | D3      | ...       | Dn-1 | Dn  |
|-----|---------|-----|---------|-----------|------|-----|
| STX | CMD/RES | LEN | Data(0) | Data(m)   | BCC  | ETX |

- **STX** = 0x02, **ETX** = 0x03
- **BCC**: XOR of D1 through Dn-1, ensures the result is 0x00
- **LEN**: Byte count of Data(0) through Data(m)

### Command List

| No | Command      | CMD/RES   | Description                                        |
|----|--------------|-----------|----------------------------------------------------|
| 1  | Open         | 0x10/0x11 | Start NFC relay — turn NFC carrier ON              |
| 2  | Close        | 0x20/0x21 | Stop NFC relay — turn NFC carrier OFF              |
| 3  | CardAccess   | 0x30/0x31 | Send/receive FeliCa data link layer packets to card |
| 4  | SetParameter | 0x40/0x41 | Configure NFC relay parameters                     |

### CardAccess Command Routing

The CardAccess command analyzes the FeliCa data link layer command code to determine the operation:

- **Polling (0x04)**: Uses SDK `polling_and_get_card_information()` to detect cards, returns IDm + PMm in FeliCa data link layer response format
- **Other commands** (Read Without Encryption 0x06, Write Without Encryption 0x08, etc.): Uses SDK `thru()` to forward raw FeliCa data to the card and return the response

### Exception Responses (0xF1)

| Error Source (Data[0]) | Error Code (Data[1]) | Description                                     |
|------------------------|----------------------|-------------------------------------------------|
| 0x30                   | 0x01                 | CardAccess sent before Open command              |
| 0x30                   | 0x10                 | Card response timeout                            |

## Architecture

```
┌─────────────────────┐         ┌──────────────────────────┐         ┌──────────────────┐
│  Panasonic IF       │  Serial │    PaSoRi IF Tool        │  SDK    │  Sony PaSoRi     │
│  (USB CDC-ACM)      │◄───────►│  CommunicationBridge     │◄───────►│  NFC Reader      │
│                     │  IF     │  IfProtocol              │  JNA    │  (felica.dll)    │
│                     │ Protocol│  PaSoRiSdkService        │         │                  │
└─────────────────────┘         └──────────────────────────┘         └──────────────────┘
```

### Key Components

- **IfProtocol**: IF protocol message framing — builds/parses STX/CMD/LEN/Data/BCC/ETX frames, calculates BCC, accumulates partial serial data into complete frames
- **CommunicationBridgeService**: Main relay logic — receives IF commands from serial, dispatches to the appropriate handler (Open/Close/CardAccess/SetParameter), queues responses
- **PaSoRiSdkService**: FeliCa SDK wrapper — initializes the library, opens the reader, polls for cards, sends raw FeliCa commands via `thru()`
- **FelicaLibrary**: JNA interface to felica.dll — maps native C functions for reader control, card polling, and data exchange
- **SerialPortService**: Serial port communication via jSerialComm — connects to USB CDC-ACM devices

## Quick Start (Non-Java Users)

If you are not familiar with Java development, the easiest way to get a working application is to download a pre-built release or build the distributable package using the included scripts.

### Option 1: Download a Release

Check the [Releases](../../releases) page for pre-built zip files. Extract and run `start.bat` — no Java installation required.

### Option 2: Build from Source (No Java Knowledge Required)

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
Push-Location staging; ..\scripts\jre.ps1; Pop-Location
.\scripts\package-artifact.ps1

# The result is in dist/Pasori-if-windows-<version>.zip
# Extract it anywhere and run start.bat
```

See [scripts/README.md](scripts/README.md) for detailed descriptions of each step.

## Requirements

- JDK 21 or later
- Maven 3.6 or later (or use the included Maven wrapper `mvnw.cmd`)
- Sony PaSoRi NFC reader/writer with FeliCa port driver installed (NFCPortWithDriver.exe)
- Windows OS (felica.dll is Windows-only)

## Dependencies

- JavaFX 21
- jSerialComm 2.10.4
- JNA 5.17.0 (for FeliCa SDK binding)
- AtlantaFX (modern UI themes)
- PreferencesFX (settings UI)
- RichTextFX (colored log display)

## Building

### Quick Build (Maven)

Build the fat JAR using the Maven wrapper included in the project:

```powershell
# Windows (Maven wrapper)
.\mvnw.cmd clean package -DskipTests

# Or using system Maven
mvn clean package -DskipTests
```

The fat JAR is output to `target/javafx-pasori-if.jar`.

### Build Script

The project provides a PowerShell build script that configures an Aliyun Maven mirror (faster downloads in China) and invokes the Maven wrapper automatically:

```powershell
# Build without tests (default)
.\scripts\build.ps1

# Build with tests
.\scripts\build.ps1 -RunTests
```

### Full Packaging (Distributable with Custom JRE)

To create a self-contained distributable zip that includes a minimal custom JRE (no need for users to install Java), run the following scripts **in order** from the project root on Windows:

```powershell
# 1. Build the fat JAR
.\scripts\build.ps1

# 2. Resolve version and prepare staging directory
#    Copies the jar, README, and launcher scripts to staging/
.\scripts\resolve-version.ps1

# 3. Create a minimal custom JRE via jlink
#    Downloads Adoptium JDK, analyzes module dependencies with jdeps,
#    and produces a stripped-down JRE in staging/jre/
Push-Location staging
..\scripts\jre.ps1
Pop-Location

# 4. Package into a distributable zip
#    Creates dist/Pasori-if-windows-<version>.zip
.\scripts\package-artifact.ps1
```

The resulting zip in `dist/` contains everything needed to run the application on Windows without a pre-installed JDK.

> **Note**: The `jre.ps1` script downloads a JDK from [Adoptium](https://adoptium.net/) to build the custom JRE. Internet access is required for this step.

## Running

### From Source (Development)

```powershell
mvn javafx:run
```

Or run the built JAR directly (requires JDK 21 on `PATH`):

```powershell
java -jar target/javafx-pasori-if.jar
```

### From Distributable Package

Extract the packaged zip and use the included launcher scripts:

- **`start.bat`** — Launches the application in the background (no console window)
- **`start.vbs`** — Launches via `start.bat` with a hidden console window
- **`console.bat`** — Launches with a visible console window (useful for debugging)

The launchers automatically detect the bundled `jre/` directory. If the custom JRE is not present, they fall back to the system `java` on `PATH`.

## Usage

1. **Connect PaSoRi Device**: Click "Connect" to auto-detect and open the PaSoRi NFC reader via FeliCa SDK
2. **Connect IF Device**: Select the USB CDC-ACM serial port, configure baud rate, and click "Connect"
3. **Enable Bridging**: Toggle the bridge switch to start relaying IF protocol commands between the two devices
4. **Monitor Communication**: All IF protocol commands and responses are logged in real-time with color-coded direction indicators
5. **Manual Testing**: Send raw hex data to the IF device for protocol testing
