/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.tlcsdm.pasori.model.SerialPortConfig;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Service class for handling serial port communication.
 * This service wraps jSerialComm functionality for easier use.
 */
public class SerialPortService {

    private SerialPort serialPort;
    private Consumer<byte[]> dataReceivedCallback;
    private Consumer<String> errorCallback;
    private volatile boolean isConnected = false;

    /**
     * Get list of all available serial ports.
     * 
     * @return array of available SerialPort objects
     */
    public static SerialPort[] getAvailablePorts() {
        return SerialPort.getCommPorts();
    }

    /**
     * Get list of available port names.
     * 
     * @return array of port name strings
     */
    public static String[] getAvailablePortNames() {
        SerialPort[] ports = getAvailablePorts();
        return Arrays.stream(ports)
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
    }

    /**
     * Connect to a serial port with the given configuration.
     * 
     * @param config the serial port configuration
     * @return true if connection successful, false otherwise
     */
    public boolean connect(SerialPortConfig config) {
        if (isConnected) {
            disconnect();
        }

        serialPort = SerialPort.getCommPort(config.getPortName());
        
        if (serialPort == null) {
            if (errorCallback != null) {
                errorCallback.accept("Failed to find port: " + config.getPortName());
            }
            return false;
        }

        // Configure port parameters
        serialPort.setBaudRate(config.getBaudRate());
        serialPort.setNumDataBits(config.getDataBits());
        serialPort.setNumStopBits(config.getStopBits());
        serialPort.setParity(config.getParity());
        
        // Set timeouts
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        // Try to open the port
        if (!serialPort.openPort()) {
            if (errorCallback != null) {
                errorCallback.accept("Failed to open port: " + config.getPortName());
            }
            return false;
        }

        // Add data listener
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    int bytesAvailable = serialPort.bytesAvailable();
                    if (bytesAvailable > 0) {
                        byte[] readBuffer = new byte[bytesAvailable];
                        int bytesRead = serialPort.readBytes(readBuffer, bytesAvailable);
                        if (bytesRead > 0 && dataReceivedCallback != null) {
                            byte[] data = Arrays.copyOf(readBuffer, bytesRead);
                            dataReceivedCallback.accept(data);
                        }
                    }
                }
            }
        });

        isConnected = true;
        return true;
    }

    /**
     * Disconnect from the serial port.
     */
    public void disconnect() {
        if (serialPort != null) {
            serialPort.removeDataListener();
            serialPort.closePort();
            serialPort = null;
        }
        isConnected = false;
    }

    /**
     * Send data to the serial port.
     * 
     * @param data the byte array to send
     * @return the number of bytes written, or -1 if error
     */
    public int sendData(byte[] data) {
        if (!isConnected || serialPort == null) {
            if (errorCallback != null) {
                errorCallback.accept("Cannot send data: port not connected");
            }
            return -1;
        }

        int bytesWritten = serialPort.writeBytes(data, data.length);
        if (bytesWritten < 0) {
            if (errorCallback != null) {
                errorCallback.accept("Failed to write data to serial port");
            }
        }
        return bytesWritten;
    }

    /**
     * Check if connected to a serial port.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }

    /**
     * Set callback for received data.
     * 
     * @param callback the callback function that receives byte array data
     */
    public void setDataReceivedCallback(Consumer<byte[]> callback) {
        this.dataReceivedCallback = callback;
    }

    /**
     * Set callback for errors.
     * 
     * @param callback the callback function that receives error messages
     */
    public void setErrorCallback(Consumer<String> callback) {
        this.errorCallback = callback;
    }

    /**
     * Get the current serial port.
     * 
     * @return the SerialPort object or null if not connected
     */
    public SerialPort getSerialPort() {
        return serialPort;
    }

    /**
     * Get the port name if connected.
     * 
     * @return port name or null if not connected
     */
    public String getPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : null;
    }
}
