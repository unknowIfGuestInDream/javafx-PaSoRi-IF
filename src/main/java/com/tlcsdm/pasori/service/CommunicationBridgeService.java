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
 * <p>
 * This service:
 * <ul>
 *   <li>Receives data from PaSoRi and forwards to Panasonic アンテナIF</li>
 *   <li>Receives data from Panasonic アンテナIF and forwards to PaSoRi</li>
 *   <li>Uses message queues to ensure reliable message delivery even during sequential port connections</li>
 *   <li>Logs all communication for debugging purposes</li>
 * </ul>
 * <p>
 * The service uses {@link SerialPortHandler} abstraction for device communication,
 * allowing for future device-specific implementations.
 */
public class CommunicationBridgeService {

    private final SerialPortHandler pasoriHandler;
    private final SerialPortHandler antennaIfHandler;
    
    // Message queues for reliable message delivery
    private final BlockingQueue<QueuedMessage> pasoriToAntennaQueue;
    private final BlockingQueue<QueuedMessage> antennaToPasoriQueue;
    
    // Background executor for processing queued messages
    private final ExecutorService messageProcessor;
    
    // Maximum retry attempts for message delivery before discarding
    private static final int MAX_RETRY_ATTEMPTS = 100;
    
    // Retry delay in milliseconds
    private static final long RETRY_DELAY_MS = 50;
    
    // Queue poll timeout in milliseconds
    private static final long QUEUE_POLL_TIMEOUT_MS = 100;
    
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

    /**
     * Create a new communication bridge service with default handlers.
     */
    public CommunicationBridgeService() {
        this(new PaSoRiPortHandler(), new AntennaIfPortHandler());
    }

    /**
     * Create a new communication bridge service with custom handlers.
     * This constructor supports dependency injection for testing and customization.
     *
     * @param pasoriHandler    the handler for PaSoRi device
     * @param antennaIfHandler the handler for AntennaIF device
     */
    public CommunicationBridgeService(SerialPortHandler pasoriHandler, SerialPortHandler antennaIfHandler) {
        this.pasoriHandler = pasoriHandler;
        this.antennaIfHandler = antennaIfHandler;
        this.pasoriToAntennaQueue = new LinkedBlockingQueue<>();
        this.antennaToPasoriQueue = new LinkedBlockingQueue<>();
        
        // Create named thread factory for better debugging
        ThreadFactory threadFactory = createMessageProcessorThreadFactory();
        this.messageProcessor = Executors.newFixedThreadPool(2, threadFactory);
        
        setupCallbacks();
        startMessageProcessors();
    }

    /**
     * Create a thread factory for message processor threads.
     *
     * @return the configured thread factory
     */
    private ThreadFactory createMessageProcessorThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "message-processor-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
    }
    
    /**
     * Start background threads to process queued messages.
     */
    private void startMessageProcessors() {
        // Processor for PaSoRi -> Antenna messages
        messageProcessor.submit(() -> 
            processMessageQueue(pasoriToAntennaQueue, antennaIfHandler, "Antenna"));
        
        // Processor for Antenna -> PaSoRi messages
        messageProcessor.submit(() -> 
            processMessageQueue(antennaToPasoriQueue, pasoriHandler, "PaSoRi"));
    }

    /**
     * Process messages from a queue and send them to the target handler.
     * This method runs in a loop until the service is shut down.
     *
     * @param queue      the message queue to process
     * @param target     the target handler to send messages to
     * @param targetName the name of the target for logging
     */
    private void processMessageQueue(BlockingQueue<QueuedMessage> queue, 
                                     SerialPortHandler target, 
                                     String targetName) {
        while (running) {
            try {
                QueuedMessage message = queue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (message != null) {
                    if (target.isConnected()) {
                        try {
                            int bytesWritten = target.sendData(message.data);
                            if (bytesWritten < 0) {
                                // Send failed, re-queue with retry limit
                                requeue(message, queue, targetName);
                            }
                        } catch (Exception e) {
                            // Send failed, re-queue with retry limit
                            requeue(message, queue, targetName);
                        }
                    } else {
                        // Re-queue if target is not connected, with retry limit
                        requeue(message, queue, targetName);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
                Thread.sleep(RETRY_DELAY_MS);
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
        pasoriHandler.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
            if (bridgingEnabled) {
                pasoriToAntennaQueue.offer(new QueuedMessage(data));
            }
        });

        // Forward data from アンテナIF to PaSoRi via queue
        antennaIfHandler.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
            if (bridgingEnabled) {
                antennaToPasoriQueue.offer(new QueuedMessage(data));
            }
        });

        // Error callbacks
        pasoriHandler.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, pasoriHandler.getName() + " Error: " + error));
        antennaIfHandler.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, antennaIfHandler.getName() + " Error: " + error));
    }

    /**
     * Connect to PaSoRi device.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectPaSoRi(SerialPortConfig config) {
        boolean result = pasoriHandler.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, pasoriHandler.getName() + " connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from PaSoRi device.
     */
    public void disconnectPaSoRi() {
        pasoriHandler.disconnect();
        log(LogEntry.Direction.SYSTEM, pasoriHandler.getName() + " disconnected");
    }

    /**
     * Connect to Panasonic アンテナIF device via USB CDC-ACM.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectAntennaIf(SerialPortConfig config) {
        boolean result = antennaIfHandler.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, antennaIfHandler.getName() + " connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from アンテナIF device.
     */
    public void disconnectAntennaIf() {
        antennaIfHandler.disconnect();
        log(LogEntry.Direction.SYSTEM, antennaIfHandler.getName() + " disconnected");
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
        return pasoriHandler.sendData(data);
    }

    /**
     * Manually send data to アンテナIF device.
     * 
     * @param data the data to send
     * @return bytes written or -1 on error
     */
    public int sendToAntennaIf(byte[] data) {
        log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
        return antennaIfHandler.sendData(data);
    }

    /**
     * Check if PaSoRi is connected.
     * 
     * @return true if connected
     */
    public boolean isPaSoRiConnected() {
        return pasoriHandler.isConnected();
    }

    /**
     * Check if アンテナIF is connected.
     * 
     * @return true if connected
     */
    public boolean isAntennaIfConnected() {
        return antennaIfHandler.isConnected();
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
     * Shutdown all connections and release resources.
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
        
        pasoriHandler.disconnect();
        antennaIfHandler.disconnect();
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
}
