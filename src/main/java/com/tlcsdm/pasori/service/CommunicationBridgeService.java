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
 * Bridge service that manages communication between PaSoRi device (via SDK) and Panasonic アンテナIF device.
 * 
 * This service:
 * - Uses the FeliCa SDK to communicate with PaSoRi (polling for cards, reading IDm/PMm)
 * - Receives data from Panasonic アンテナIF via serial port
 * - Forwards card data from PaSoRi to アンテナIF
 * - Uses message queues to ensure reliable message delivery
 * - Logs all communication for debugging purposes
 */
public class CommunicationBridgeService {

    private final PaSoRiSdkService pasoriSdkService;
    private final SerialPortService antennaIfService;
    
    // Message queue for PaSoRi -> Antenna messages
    private final BlockingQueue<QueuedMessage> pasoriToAntennaQueue;
    
    // Background executor for processing queued messages and polling
    private final ExecutorService messageProcessor;
    
    // Maximum retry attempts for message delivery before discarding
    private static final int MAX_RETRY_ATTEMPTS = 100;
    
    private Consumer<LogEntry> logCallback;
    private volatile boolean bridgingEnabled = false;
    private volatile boolean pollingActive = false;
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
        this.pasoriSdkService = new PaSoRiSdkService();
        this.antennaIfService = new SerialPortService();
        this.pasoriToAntennaQueue = new LinkedBlockingQueue<>();
        
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
        // Processor for PaSoRi -> Antenna messages (from SDK polling results)
        messageProcessor.submit(() -> {
            while (running) {
                try {
                    QueuedMessage message = pasoriToAntennaQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null && antennaIfService.isConnected()) {
                        try {
                            int bytesWritten = antennaIfService.sendData(message.data);
                            if (bytesWritten < 0) {
                                requeue(message, pasoriToAntennaQueue, "Antenna");
                            }
                        } catch (Exception e) {
                            requeue(message, pasoriToAntennaQueue, "Antenna");
                        }
                    } else if (message != null) {
                        requeue(message, pasoriToAntennaQueue, "Antenna");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Polling thread for PaSoRi SDK card detection
        messageProcessor.submit(() -> {
            while (running) {
                try {
                    if (pollingActive && pasoriSdkService.isReaderOpened()) {
                        byte[][] cardData = pasoriSdkService.pollCard();
                        if (cardData != null) {
                            byte[] idm = cardData[0];
                            byte[] pmm = cardData[1];
                            log(LogEntry.Direction.PASORI_TO_ANTENNA,
                                "Card detected - IDm: " + PaSoRiSdkService.bytesToHex(idm)
                                + " PMm: " + PaSoRiSdkService.bytesToHex(pmm));
                            
                            if (bridgingEnabled) {
                                // Forward card IDm data to Antenna IF
                                byte[] convertedData = convertPaSoRiToAntennaProtocol(idm);
                                pasoriToAntennaQueue.offer(new QueuedMessage(convertedData));
                            }
                        }
                    }
                    Thread.sleep(500); // Poll interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Re-queue a message with retry limit checking.
     */
    private void requeue(QueuedMessage message, BlockingQueue<QueuedMessage> queue, String targetName) {
        message.retryCount++;
        if (message.retryCount < MAX_RETRY_ATTEMPTS) {
            queue.offer(message);
            try {
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
        // Setup SDK callbacks
        pasoriSdkService.setErrorCallback(error ->
            log(LogEntry.Direction.SYSTEM, "PaSoRi SDK Error: " + error));
        pasoriSdkService.setInfoCallback(info ->
            log(LogEntry.Direction.SYSTEM, "PaSoRi SDK: " + info));

        // Forward data received from アンテナIF to log
        antennaIfService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
        });

        // Error callbacks
        antennaIfService.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, "アンテナIF Error: " + error));
    }

    /**
     * Connect to PaSoRi device via FeliCa SDK (auto-detect).
     * 
     * @return true if connection successful
     */
    public boolean connectPaSoRi() {
        if (!pasoriSdkService.isInitialized()) {
            if (!pasoriSdkService.initializeLibrary()) {
                return false;
            }
        }
        boolean result = pasoriSdkService.openReaderAuto();
        if (result) {
            pollingActive = true;
            log(LogEntry.Direction.SYSTEM, "PaSoRi connected via SDK (auto-detect)");
        }
        return result;
    }

    /**
     * Disconnect from PaSoRi device.
     */
    public void disconnectPaSoRi() {
        pollingActive = false;
        pasoriSdkService.closeReader();
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
     * Check if PaSoRi is connected via SDK.
     * 
     * @return true if connected
     */
    public boolean isPaSoRiConnected() {
        return pasoriSdkService.isReaderOpened();
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
        pollingActive = false;
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
        
        pasoriSdkService.shutdown();
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
     * 
     * @param data the raw data received from PaSoRi (e.g. card IDm)
     * @return the converted data in Antenna protocol format
     */
    private byte[] convertPaSoRiToAntennaProtocol(byte[] data) {
        if (data == null) {
            return null;
        }
        // TODO: Implement PaSoRi to Antenna protocol conversion
        return data;
    }
}
