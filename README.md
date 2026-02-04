# PaSoRi IF Tool

JavaFX tool for serial communication bridging between PaSoRi device and Panasonic USB CDC-ACM (アンテナIF仕様) device.

## Requirements

- JDK 21 or later
- Maven 3.6 or later

## Dependencies

- JavaFX 21
- jSerialComm 2.10.4

## Features

- **Dual Serial Port Connection**: Connect to both PaSoRi (SDK) and Panasonic USB CDC-ACM (アンテナIF) devices simultaneously
- **Data Bridging**: Automatically forward data between two connected devices
- **Communication Logging**: View all communication data in real-time with timestamps
- **Manual Data Send**: Send hex data manually to either device for testing
- **Port Configuration**: Configure baud rate and other serial port settings

## Building

```bash
mvn clean package
```

## Running

```bash
mvn javafx:run
```

Or run the JAR directly:

```bash
java -jar target/javafx-pasori-if-1.0.0-SNAPSHOT.jar
```

## Usage

1. **Connect PaSoRi Device**
   - Select the serial port for PaSoRi
   - Configure baud rate (default: 115200)
   - Click "Connect"

2. **Connect アンテナIF Device**
   - Select the serial port for the USB CDC-ACM device
   - Configure baud rate (default: 115200)
   - Click "Connect"

3. **Enable Bridging**
   - Once both devices are connected, click "Bridge OFF" button to toggle bridging ON
   - Data will automatically flow between devices

4. **Manual Testing**
   - Enter hex data in the input field (format: `00 A1 FF` or `00A1FF`)
   - Select target device (PaSoRi or アンテナIF)
   - Click "Send"

## Communication Protocol

The tool acts as a transparent bridge:
- Data received from PaSoRi → forwarded to アンテナIF
- Data received from アンテナIF → forwarded to PaSoRi

All communication is logged with timestamps and direction indicators for debugging purposes.
