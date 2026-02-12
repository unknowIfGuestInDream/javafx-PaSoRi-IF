# FeliCa SDK Native Files

This directory holds the FeliCa SDK header files and import library for reference:

- `felica.h` - Main FeliCa header
- `felica_basic_lite_s.h` - FeliCa basic/lite-s header with struct and function declarations
- `felica_error.h` - FeliCa error type definitions
- `rw_error.h` - Reader/Writer error type definitions
- `felica.lib` - FeliCa import library (for C/C++ linking reference)

These files are provided by the Sony FeliCa SDK (RC-S300/380 series).

> **Note:** JNA loads the runtime `felica.dll` from the system library path — it is installed
> by the FeliCa port driver (`NFCPortWithDriver.exe`). The `.lib` file here is a C/C++ import
> library for compile-time linking and is not directly loaded by JNA.
