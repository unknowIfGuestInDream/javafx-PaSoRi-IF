/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bridge service that manages communication between PaSoRi device and Panasonic アンテナIF device.
 * 
 * This service:
 * - Receives data from PaSoRi and forwards to Panasonic アンテナIF
 * - Receives data from Panasonic アンテナIF and forwards to PaSoRi
 * - Uses message queues to ensure reliable message delivery even during sequential port connections
 * - Logs all communication for debugging purposes
 */
public class CommunicationBridgeService {

    private final SerialPortService pasoriService;
    private final SerialPortService antennaIfService;
    
    // Message queues for reliable message delivery
    private final BlockingQueue<QueuedMessage> pasoriToAntennaQueue;
    private final BlockingQueue<QueuedMessage> antennaToPasoriQueue;
    
    // Background executor for processing queued messages
    private final ExecutorService messageProcessor;
    
    // Maximum retry attempts for message delivery before discarding
    private static final int MAX_RETRY_ATTEMPTS = 100;
    
    private Consumer<LogEntry> logCallback;
    private volatile boolean bridgingEnabled = false;
    private volatile boolean running = true;
    
    /**
     * Wrapper class for queued messages with retry tracking.
     */
    private static class QueuedMessage {
        final byte[] data;
        int retryCount;
        
        QueuedMessage(byte[] data) {
            this.data = data;
            this.retryCount = 0;
        }
    }

    public CommunicationBridgeService() {
        this.pasoriService = new SerialPortService();
        this.antennaIfService = new SerialPortService();
        this.pasoriToAntennaQueue = new LinkedBlockingQueue<>();
        this.antennaToPasoriQueue = new LinkedBlockingQueue<>();
        
        // Create named thread factory for better debugging
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "message-processor-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        this.messageProcessor = Executors.newFixedThreadPool(2, threadFactory);
        
        setupCallbacks();
        startMessageProcessors();
    }
    
    /**
     * Start background threads to process queued messages.
     */
    private void startMessageProcessors() {
        // Processor for PaSoRi -> Antenna messages
        messageProcessor.submit(() -> {
            while (running) {
                try {
                    QueuedMessage message = pasoriToAntennaQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null && antennaIfService.isConnected()) {
                        try {
                            int bytesWritten = antennaIfService.sendData(message.data);
                            if (bytesWritten < 0) {
                                // Send failed, re-queue with retry limit
                                requeue(message, pasoriToAntennaQueue, "Antenna");
                            }
                        } catch (Exception e) {
                            // Send failed, re-queue with retry limit
                            requeue(message, pasoriToAntennaQueue, "Antenna");
                        }
                    } else if (message != null) {
                        // Re-queue if antenna is not connected, with retry limit
                        requeue(message, pasoriToAntennaQueue, "Antenna");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Processor for Antenna -> PaSoRi messages
        messageProcessor.submit(() -> {
            while (running) {
                try {
                    QueuedMessage message = antennaToPasoriQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null && pasoriService.isConnected()) {
                        try {
                            int bytesWritten = pasoriService.sendData(message.data);
                            if (bytesWritten < 0) {
                                // Send failed, re-queue with retry limit
                                requeue(message, antennaToPasoriQueue, "PaSoRi");
                            }
                        } catch (Exception e) {
                            // Send failed, re-queue with retry limit
                            requeue(message, antennaToPasoriQueue, "PaSoRi");
                        }
                    } else if (message != null) {
                        // Re-queue if PaSoRi is not connected, with retry limit
                        requeue(message, antennaToPasoriQueue, "PaSoRi");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Re-queue a message with retry limit checking.
     * 
     * @param message the message to re-queue
     * @param queue the queue to add the message to
     * @param targetName the name of the target device for logging
     */
    private void requeue(QueuedMessage message, BlockingQueue<QueuedMessage> queue, String targetName) {
        message.retryCount++;
        if (message.retryCount < MAX_RETRY_ATTEMPTS) {
            queue.offer(message);
            try {
                // Brief sleep to prevent busy-waiting
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            log(LogEntry.Direction.SYSTEM, 
                "Message to " + targetName + " discarded after max retries (size: " + message.data.length + " bytes)");
        }
    }

    private void setupCallbacks() {
        // Forward data from PaSoRi to アンテナIF via queue
        pasoriService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
            if (bridgingEnabled) {
                // Convert PaSoRi data to Antenna protocol format before sending
                byte[] convertedData = convertPaSoRiToAntennaProtocol(data);
                pasoriToAntennaQueue.offer(new QueuedMessage(convertedData));
            }
        });

        // Forward data from アンテナIF to PaSoRi via queue
        antennaIfService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
            if (bridgingEnabled) {
                // Convert Antenna data to PaSoRi protocol format before sending
                byte[] convertedData = convertAntennaToPaSoRiProtocol(data);
                antennaToPasoriQueue.offer(new QueuedMessage(convertedData));
            }
        });

        // Error callbacks
        pasoriService.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, "PaSoRi Error: " + error));
        antennaIfService.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, "アンテナIF Error: " + error));
    }

    /**
     * Connect to PaSoRi device.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectPaSoRi(SerialPortConfig config) {
        boolean result = pasoriService.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, "PaSoRi connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from PaSoRi device.
     */
    public void disconnectPaSoRi() {
        pasoriService.disconnect();
        log(LogEntry.Direction.SYSTEM, "PaSoRi disconnected");
    }

    /**
     * Connect to Panasonic アンテナIF device via USB CDC-ACM.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectAntennaIf(SerialPortConfig config) {
        boolean result = antennaIfService.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, "アンテナIF connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from アンテナIF device.
     */
    public void disconnectAntennaIf() {
        antennaIfService.disconnect();
        log(LogEntry.Direction.SYSTEM, "アンテナIF disconnected");
    }

    /**
     * Enable or disable automatic data bridging between devices.
     * 
     * @param enabled true to enable bridging
     */
    public void setBridgingEnabled(boolean enabled) {
        this.bridgingEnabled = enabled;
        log(LogEntry.Direction.SYSTEM, "Data bridging " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if bridging is enabled.
     * 
     * @return true if bridging is enabled
     */
    public boolean isBridgingEnabled() {
        return bridgingEnabled;
    }

    /**
     * Manually send data to PaSoRi device.
     * 
     * @param data the data to send
     * @return bytes written or -1 on error
     */
    public int sendToPaSoRi(byte[] data) {
        log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
        return pasoriService.sendData(data);
    }

    /**
     * Manually send data to アンテナIF device.
     * 
     * @param data the data to send
     * @return bytes written or -1 on error
     */
    public int sendToAntennaIf(byte[] data) {
        log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
        return antennaIfService.sendData(data);
    }

    /**
     * Check if PaSoRi is connected.
     * 
     * @return true if connected
     */
    public boolean isPaSoRiConnected() {
        return pasoriService.isConnected();
    }

    /**
     * Check if アンテナIF is connected.
     * 
     * @return true if connected
     */
    public boolean isAntennaIfConnected() {
        return antennaIfService.isConnected();
    }

    /**
     * Set the log callback for communication logging.
     * 
     * @param callback the callback function
     */
    public void setLogCallback(Consumer<LogEntry> callback) {
        this.logCallback = callback;
    }

    /**
     * Shutdown all connections.
     */
    public void shutdown() {
        bridgingEnabled = false;
        running = false;
        
        // Shutdown message processor threads
        messageProcessor.shutdown();
        try {
            if (!messageProcessor.awaitTermination(2, TimeUnit.SECONDS)) {
                messageProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear any remaining queued messages
        pasoriToAntennaQueue.clear();
        antennaToPasoriQueue.clear();
        
        pasoriService.disconnect();
        antennaIfService.disconnect();
        log(LogEntry.Direction.SYSTEM, "Communication bridge shutdown");
    }

    private void log(LogEntry.Direction direction, byte[] data) {
        if (logCallback != null) {
            logCallback.accept(new LogEntry(direction, data));
        }
    }

    private void log(LogEntry.Direction direction, String message) {
        if (logCallback != null) {
            logCallback.accept(new LogEntry(direction, message, false));
        }
    }

    /**
     * Convert data received from PaSoRi to the protocol format expected by Antenna device.
     * This method should transform the raw PaSoRi data to comply with the Antenna protocol.
     * 
     * @param data the raw data received from PaSoRi
     * @return the converted data in Antenna protocol format
     */
    private byte[] convertPaSoRiToAntennaProtocol(byte[] data) {
        if (data == null) {
            return null;
        }
        // TODO: Implement PaSoRi to Antenna protocol conversion
        // The raw data from PaSoRi needs to be transformed to match the Antenna protocol format.
        // Example transformations may include:
        // - Adding/removing protocol headers or trailers
        // - Checksum calculation and appending
        // - Data encoding/decoding
        // - Command translation between protocols
        return data;
    }

    /**
     * Convert data received from Antenna device to the protocol format expected by PaSoRi.
     * This method should transform the raw Antenna data to comply with the PaSoRi protocol.
     * 
     * @param data the raw data received from Antenna device
     * @return the converted data in PaSoRi protocol format
     */
    private byte[] convertAntennaToPaSoRiProtocol(byte[] data) {
        if (data == null) {
            return null;
        }
        // TODO: Implement Antenna to PaSoRi protocol conversion
        // The raw data from Antenna needs to be transformed to match the PaSoRi protocol format.
        // Example transformations may include:
        // - Adding/removing protocol headers or trailers
        // - Checksum calculation and appending
        // - Data encoding/decoding
        // - Command translation between protocols
        return data;
    }
}
