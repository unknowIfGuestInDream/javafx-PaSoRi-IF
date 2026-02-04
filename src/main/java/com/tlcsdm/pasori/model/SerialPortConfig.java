/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.model;

/**
 * Represents serial port configuration settings.
 */
public class SerialPortConfig {

    private String portName;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;

    public SerialPortConfig() {
        // Default settings
        this.baudRate = 115200;
        this.dataBits = 8;
        this.stopBits = 1;
        this.parity = 0; // None
    }

    public SerialPortConfig(String portName, int baudRate, int dataBits, int stopBits, int parity) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    @Override
    public String toString() {
        return "SerialPortConfig{" +
                "portName='" + portName + '\'' +
                ", baudRate=" + baudRate +
                ", dataBits=" + dataBits +
                ", stopBits=" + stopBits +
                ", parity=" + parity +
                '}';
    }
}
