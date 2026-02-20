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
 * Bridge service that manages communication between PaSoRi device (via SDK) and IF device.
 *
 * <p>This service acts as a relay (中継器) between IF and PaSoRi using the IF communication protocol:</p>
 * <ul>
 *   <li>Receives IF protocol commands from the IF device via serial port</li>
 *   <li>Processes commands: Open, Close, CardAccess, SetParameter</li>
 *   <li>Uses the FeliCa SDK to communicate with PaSoRi NFC reader</li>
 *   <li>Sends IF protocol responses back to the IF device</li>
 * </ul>
 *
 * <p>The relay always accepts subsequent commands. No FeliCa command timeout monitoring
 * is performed on the relay side. Presence checks are not performed; only NFC packets
 * received via CardAccess are sent to the card.</p>
 */
public class CommunicationBridgeService {

    private final PaSoRiSdkService pasoriSdkService;
    private final SerialPortService antennaIfService;

    // Message queue for response messages to send back to IF device
    private final BlockingQueue<QueuedMessage> responseQueue;

    // Frame accumulator for assembling IF protocol frames from serial data
    private final IfProtocol.FrameAccumulator frameAccumulator;

    // Background executor for processing queued messages
    private final ExecutorService messageProcessor;

    // Maximum retry attempts for message delivery before discarding
    private static final int MAX_RETRY_ATTEMPTS = 100;

    private Consumer<LogEntry> logCallback;
    private volatile Consumer<byte[]> manualReceiveCallback;
    private volatile boolean bridgingEnabled = false;
    private volatile boolean nfcCarrierOn = false;
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
        this.responseQueue = new LinkedBlockingQueue<>();
        this.frameAccumulator = new IfProtocol.FrameAccumulator();

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
        this.messageProcessor = Executors.newFixedThreadPool(1, threadFactory);

        setupCallbacks();
        startMessageProcessors();
    }

    /**
     * Start background thread to process response queue and send responses to IF device.
     */
    private void startMessageProcessors() {
        // Response sender thread: sends queued response frames to IF device
        messageProcessor.submit(() -> {
            while (running) {
                try {
                    QueuedMessage message = responseQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null && antennaIfService.isConnected()) {
                        try {
                            int bytesWritten = antennaIfService.sendData(message.data);
                            if (bytesWritten < 0) {
                                requeue(message, responseQueue, "IF");
                            } else {
                                log(LogEntry.Direction.PASORI_TO_ANTENNA, message.data);
                            }
                        } catch (Exception e) {
                            requeue(message, responseQueue, "IF");
                        }
                    } else if (message != null) {
                        requeue(message, responseQueue, "IF");
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

        // IF protocol frame processing: accumulate and process commands from IF device
        antennaIfService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
            Consumer<byte[]> cb = manualReceiveCallback;
            if (cb != null) {
                cb.accept(data);
            }
            if (bridgingEnabled) {
                byte[] frame = frameAccumulator.feed(data);
                if (frame != null) {
                    processIfCommand(frame);
                }
            }
        });

        // Error callbacks
        antennaIfService.setErrorCallback(error ->
            log(LogEntry.Direction.SYSTEM, "IF Error: " + error));
    }

    /**
     * Process a complete IF protocol command frame received from the IF device.
     *
     * @param frame the raw frame bytes
     */
    private void processIfCommand(byte[] frame) {
        IfProtocol.Message message = IfProtocol.parseFrame(frame);
        if (message == null) {
            log(LogEntry.Direction.SYSTEM, "Invalid IF protocol frame received");
            return;
        }

        switch (message.getCommand()) {
            case IfProtocol.CMD_OPEN -> handleOpenCommand();
            case IfProtocol.CMD_CLOSE -> handleCloseCommand();
            case IfProtocol.CMD_CARD_ACCESS -> handleCardAccessCommand(message);
            case IfProtocol.CMD_SET_PARAMETER -> handleSetParameterCommand(message);
            default -> log(LogEntry.Direction.SYSTEM,
                "Unknown IF command: 0x" + String.format("%02X", message.getCommand()));
        }
    }

    /**
     * Handle Open command (0x10).
     * Initialize the FeliCa library and open the reader (NFC carrier on).
     */
    private void handleOpenCommand() {
        log(LogEntry.Direction.SYSTEM, "IF Open command received");

        boolean success = true;
        if (!pasoriSdkService.isInitialized()) {
            success = pasoriSdkService.initializeLibrary();
        }
        if (success && !pasoriSdkService.isReaderOpened()) {
            success = pasoriSdkService.openReaderAuto();
        }

        nfcCarrierOn = success;
        byte resCode = success ? IfProtocol.RES_CODE_OK : IfProtocol.RES_CODE_ERROR;
        byte[] response = IfProtocol.buildOpenResponse(resCode);
        responseQueue.offer(new QueuedMessage(response));
        log(LogEntry.Direction.SYSTEM, "IF Open response: " + (success ? "OK" : "Error"));
    }

    /**
     * Handle Close command (0x20).
     * Close the reader and dispose the library (NFC carrier off).
     */
    private void handleCloseCommand() {
        log(LogEntry.Direction.SYSTEM, "IF Close command received");

        nfcCarrierOn = false;
        pasoriSdkService.closeReader();

        byte[] response = IfProtocol.buildCloseResponse(IfProtocol.RES_CODE_OK);
        responseQueue.offer(new QueuedMessage(response));
        log(LogEntry.Direction.SYSTEM, "IF Close response: OK");
    }

    /**
     * Handle CardAccess command (0x30).
     * Analyzes the FeliCa data link layer command code to determine the operation:
     * <ul>
     *   <li>Polling (0x04): Use SDK polling to detect cards and return IDm/PMm</li>
     *   <li>Other commands: Forward FeliCa data to the card via SDK thru command</li>
     * </ul>
     */
    private void handleCardAccessCommand(IfProtocol.Message message) {
        log(LogEntry.Direction.SYSTEM, "IF CardAccess command received");

        // Check if Open has been called
        if (!nfcCarrierOn || !pasoriSdkService.isReaderOpened()) {
            byte[] exResponse = IfProtocol.buildExceptionResponse(
                IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_BEFORE_OPEN);
            responseQueue.offer(new QueuedMessage(exResponse));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess exception: Open not called");
            return;
        }

        byte[] felicaData = message.getData();
        if (felicaData == null || felicaData.length < 2) {
            byte[] exResponse = IfProtocol.buildExceptionResponse(
                IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_TIMEOUT);
            responseQueue.offer(new QueuedMessage(exResponse));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess exception: invalid FeliCa data (too short)");
            return;
        }

        int cmdCode = IfProtocol.getFelicaCommandCode(felicaData);
        log(LogEntry.Direction.SYSTEM, "IF CardAccess FeliCa command: 0x" + String.format("%02X", cmdCode));

        if (IfProtocol.isFelicaPollingCommand(felicaData)) {
            handleCardAccessPolling(felicaData);
        } else {
            handleCardAccessThru(felicaData);
        }
    }

    /**
     * Handle CardAccess with FeliCa Polling command (0x04).
     * Uses SDK polling to detect cards and returns IDm/PMm in FeliCa data link layer format.
     *
     * @param felicaData the FeliCa Polling command data
     */
    private void handleCardAccessPolling(byte[] felicaData) {
        byte[] systemCode = IfProtocol.extractPollingSystemCode(felicaData);
        byte[][] cardData;

        if (systemCode != null) {
            cardData = pasoriSdkService.pollCard(systemCode);
            log(LogEntry.Direction.SYSTEM, "IF CardAccess Polling with system code: "
                + PaSoRiSdkService.bytesToHex(systemCode));
        } else {
            cardData = pasoriSdkService.pollCard();
            log(LogEntry.Direction.SYSTEM, "IF CardAccess Polling with default system code");
        }

        if (cardData != null) {
            byte[] idm = cardData[0];
            byte[] pmm = cardData[1];
            byte[] pollingResponse = IfProtocol.buildFelicaPollingResponse(idm, pmm);
            byte[] response = IfProtocol.buildCardAccessResponse(pollingResponse);
            responseQueue.offer(new QueuedMessage(response));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess Polling response: IDm="
                + PaSoRiSdkService.bytesToHex(idm) + " PMm=" + PaSoRiSdkService.bytesToHex(pmm));
        } else {
            byte[] exResponse = IfProtocol.buildExceptionResponse(
                IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_TIMEOUT);
            responseQueue.offer(new QueuedMessage(exResponse));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess Polling: no card found");
        }
    }

    /**
     * Handle CardAccess with FeliCa data send/receive (non-Polling commands).
     * Forwards the FeliCa data to the card via SDK thru command and returns the response.
     *
     * @param felicaData the FeliCa command data to send
     */
    private void handleCardAccessThru(byte[] felicaData) {
        byte[] cardResponse = pasoriSdkService.thruCommand(felicaData);

        if (cardResponse != null) {
            byte[] response = IfProtocol.buildCardAccessResponse(cardResponse);
            responseQueue.offer(new QueuedMessage(response));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess thru response sent (" + cardResponse.length + " bytes)");
        } else {
            byte[] exResponse = IfProtocol.buildExceptionResponse(
                IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_TIMEOUT);
            responseQueue.offer(new QueuedMessage(exResponse));
            log(LogEntry.Direction.SYSTEM, "IF CardAccess thru exception: timeout/error");
        }
    }

    /**
     * Handle SetParameter command (0x40).
     * Configure parameters for the NFC relay library.
     */
    private void handleSetParameterCommand(IfProtocol.Message message) {
        log(LogEntry.Direction.SYSTEM, "IF SetParameter command received");

        byte[] data = message.getData();
        boolean success = false;

        if (data != null && data.length >= 1) {
            byte subCmd = data[0];
            if (subCmd == 0x01 && data.length >= 2) {
                // Set card command maximum response timeout
                // Data[1] (and optional Data[2]) contain the timeout value
                int timeout;
                if (data.length >= 3) {
                    timeout = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                } else {
                    timeout = data[1] & 0xFF;
                }
                if (pasoriSdkService.isInitialized()) {
                    success = true;
                    log(LogEntry.Direction.SYSTEM, "IF SetParameter: timeout set to " + timeout + "ms");
                }
            } else {
                log(LogEntry.Direction.SYSTEM, "IF SetParameter: unknown sub-command 0x"
                    + String.format("%02X", subCmd));
            }
        }

        byte resCode = success ? IfProtocol.RES_CODE_OK : IfProtocol.RES_CODE_ERROR;
        byte[] response = IfProtocol.buildSetParameterResponse(resCode);
        responseQueue.offer(new QueuedMessage(response));
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
            nfcCarrierOn = true;
            log(LogEntry.Direction.SYSTEM, "PaSoRi connected via SDK (auto-detect)");
        }
        return result;
    }

    /**
     * Disconnect from PaSoRi device.
     */
    public void disconnectPaSoRi() {
        nfcCarrierOn = false;
        pasoriSdkService.closeReader();
        log(LogEntry.Direction.SYSTEM, "PaSoRi disconnected");
    }

    /**
     * Connect to IF device via USB CDC-ACM.
     *
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectAntennaIf(SerialPortConfig config) {
        frameAccumulator.reset();
        boolean result = antennaIfService.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, "IF connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from IF device.
     */
    public void disconnectAntennaIf() {
        antennaIfService.disconnect();
        frameAccumulator.reset();
        log(LogEntry.Direction.SYSTEM, "IF disconnected");
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
     * Manually send data to IF device.
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
     * Check if IF device is connected.
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
     * Set a callback for data received from the IF device (used by manual send dialog).
     *
     * @param callback the callback function, or null to remove
     */
    public void setManualReceiveCallback(Consumer<byte[]> callback) {
        this.manualReceiveCallback = callback;
    }

    /**
     * Shutdown all connections.
     */
    public void shutdown() {
        bridgingEnabled = false;
        nfcCarrierOn = false;
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
        responseQueue.clear();

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
}
