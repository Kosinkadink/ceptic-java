package org.jedkos.ceptic.stream;

import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.Constants;
import org.jedkos.ceptic.common.Stopwatch;
import org.jedkos.ceptic.encode.*;
import org.jedkos.ceptic.encode.exceptions.UnknownEncodingException;
import org.jedkos.ceptic.stream.exceptions.StreamClosedException;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.jedkos.ceptic.stream.exceptions.StreamHandlerStoppedException;
import org.jedkos.ceptic.stream.exceptions.StreamTotalDataSizeException;
import org.jedkos.ceptic.stream.iteration.DataStreamFrameGenerator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamHandler {

    private final LinkedBlockingDeque<StreamFrame> readBuffer;
    private final LinkedBlockingDeque<StreamFrame> managerSendDeque;
    private final AtomicInteger readBufferCounter = new AtomicInteger(0);
    private final AtomicInteger sendBufferCounter = new AtomicInteger(0);
    private final long bufferWaitTimeout = 100;

    private final Lock sendBufferLock;
    private final Condition eventSendBufferDecreased;
    private final Lock readBufferLock;
    private final Condition eventReadBufferDecreased;

    private final UUID streamId;

    private final StreamSettings settings;

    private final Stopwatch existenceTimer = new Stopwatch();
    private final Stopwatch keepAliveTimer = new Stopwatch();
    private boolean shouldStop = false;

    private EncodeHandler encodeHandler = new EncodeHandler(new ArrayList<EncodeObject>() {
        {
            add(EncodeType.none.getEncoder());
        }
    });

    public StreamHandler(UUID streamId, StreamSettings settings, LinkedBlockingDeque<StreamFrame> managerSendDeque) {
        this.streamId = streamId;
        this.settings = settings;
        this.managerSendDeque = managerSendDeque;

        readBuffer = new LinkedBlockingDeque<>();
        sendBufferLock = new ReentrantLock();
        eventSendBufferDecreased = sendBufferLock.newCondition();
        readBufferLock = new ReentrantLock();
        eventReadBufferDecreased = readBufferLock.newCondition();

        startTimers();
    }

    public UUID getStreamId() {
        return streamId;
    }

    public StreamSettings getSettings() {
        return settings;
    }

    private void startTimers() {
        existenceTimer.start();
        keepAliveTimer.start();
    }

    public void setEncode(String encodingString) throws UnknownEncodingException {
        encodeHandler = EncodeGetter.get(encodingString);
    }

    //region Buffer Checks
    public boolean isReadBufferFull() {
        return readBufferCounter.get() >= settings.readBufferSize;
    }

    public boolean isSendBufferFull() {
        return sendBufferCounter.get() >= settings.sendBufferSize;
    }

    protected void incrementSendBuffer(StreamFrame frame) {
        sendBufferCounter.getAndAdd(frame.getSize());
    }

    protected void decrementSendBuffer(StreamFrame frame) {
        sendBufferLock.lock();
        try {
            sendBufferCounter.getAndAdd(frame.getSize() * -1);
            eventSendBufferDecreased.signalAll();
        } finally {
            sendBufferLock.unlock();
        }
    }

    protected void incrementReadBuffer(StreamFrame frame) {
        readBufferCounter.getAndAdd(frame.getSize());
    }

    protected void decrementReadBuffer(StreamFrame frame) {
        readBufferLock.lock();
        try {
            readBufferCounter.getAndAdd(frame.getSize() * -1);
            eventReadBufferDecreased.signalAll();
        } finally {
            readBufferLock.unlock();
        }
    }
    //endregion

    //region Send
    /**
     * Add a closed frame to buffer and stop handler
     */
    public void sendClose() {
        sendClose(null);
    }

    /**
     * Add a closed frame to buffer and stop handler
     * @param data Optional data to include inside closed frame
     */
    public void sendClose(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        try {
            send(StreamFrameHelper.createClose(streamId, data));
        } catch (StreamException ignored) { }
        // stop handler, since close frame is being sent
        stop();
    }

    /**
     * Add frame to send buffer
     * @param frame StreamFrame to send
     */
    private void send(StreamFrame frame) throws StreamException {
        if (!isStopped()) {
            // update keep alive
            updateKeepAlive();
            // encode data on frame
            frame.encodeData(encodeHandler);
            // increment send buffer counter
            incrementSendBuffer(frame);
            // while not stopped, attempt to insert frame into buffer
            while (!isStopped()) {
                try {
                    if (isSendBufferFull()) {
                        try {
                            sendBufferLock.lock();
                            eventSendBufferDecreased.await(bufferWaitTimeout, TimeUnit.MILLISECONDS);
                            continue;
                        } finally {
                            sendBufferLock.unlock();
                        }
                    }
                    if (managerSendDeque.offerLast(frame, bufferWaitTimeout, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    //stop();
                    //throw new StreamHandlerStoppedException("handler is stopped; cannot send frames through a stopped handler");
                }
            }
        } else {
            throw new StreamHandlerStoppedException("handler is stopped; cannot send frames through a stopped handler");
        }

    }

    /**
     * Add multiple frames to send buffer
     * @param frames Iterable of StreamFrames to send sequentially
     * @throws StreamException if handler is closed or other stream issue
     */
    private void sendAll(Iterable<StreamFrame> frames) throws StreamException {
        for(StreamFrame frame : frames) {
            send(frame);
        }
    }

    /**
     * Send data by converting to StreamFrames and adding them to send buffer
     * @param data Data to send through stream
     * @throws StreamException if handler is closed or other stream issue
     */
    public void sendData(byte[] data) throws StreamException {
        sendData(data, false, false);
    }

    /**
     * Send data by converting to StreamFrames and adding them to send buffer
     * @param data Data to send through stream
     * @param isFirstHeader true if first header, false otherwise
     * @param isResponse true if response, false otherwise
     * @throws StreamException if handler is closed or other stream issue
     */
    public void sendData(byte[] data, boolean isFirstHeader, boolean isResponse) throws StreamException {
        sendAll(new DataStreamFrameGenerator(streamId, data, settings.frameMaxSize, isFirstHeader, isResponse));
    }

    /**
     * Send request as data converted into StreamFrames added to send buffer
     * @param request CepticRequest to be sent
     * @throws StreamException if handler is closed or other stream issue
     */
    public void sendRequest(CepticRequest request) throws StreamException {
        sendData(request.getData(), true, false);
    }

    /**
     * Send response as data converted into StreamFrames added to send buffer
     * @param response CepticResponse to be sent
     * @throws StreamException if handler is closed or other stream issue
     */
    public void sendResponse(CepticResponse response) throws StreamException {
        sendData(response.getData(), false, true);
    }


    /**
     * Send response as file data converted into StreamFrames added to send buffer
     * @param file File containing data to be sent
     */
    public void sendFile(File file) throws StreamException {
        throw new UnsupportedOperationException("sendFile method not implemented yet");
    }
    //endregion

    //region Read
    /**
     * Add StreamFrame to the read buffer (called by StreamManager to populate read buffer for use with read methods)
     * @param frame StreamFrame to be added to read buffer
     * @throws StreamHandlerStoppedException if handler is stopped during execution
     */
    protected void addToRead(StreamFrame frame) throws StreamHandlerStoppedException {
        if (isStopped()) {
            throw new StreamHandlerStoppedException("handler is stopped; cannot add frames to read through a stopped handler");
        }
        // update keep alive
        updateKeepAlive();
        // while not stopped, attempt to insert frame into buffer
        incrementReadBuffer(frame);
        // if is a close frame, insert into buffer and stop handler
        if (frame.isClose()) {
            readBuffer.offerLast(frame);
            stop();
            return;
        }
        while (!isStopped()) {
            try {
                if (isReadBufferFull()) {
                    try {
                        readBufferLock.lock();
                        eventReadBufferDecreased.await(bufferWaitTimeout, TimeUnit.MILLISECONDS);
                        continue;
                    } finally {
                        readBufferLock.unlock();
                    }
                }
                if (readBuffer.offerLast(frame, bufferWaitTimeout, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
            catch (InterruptedException e) {
                //stop();
                //throw new StreamHandlerStoppedException("handler is stopped; cannot add frames to read through a stopped handler");
            }
        }
    }

    /**
     * Read next frame from read buffer, defaulting to streamTimeout setting as timeout
     * @return StreamFrame or null if no frame received
     * @throws StreamHandlerStoppedException if handler stopped before/during call
     * @throws StreamClosedException if close frame received
     */
    private StreamFrame readNextFrame() throws StreamHandlerStoppedException, StreamClosedException {
        return readNextFrame(settings.streamTimeout);
    }

    /**
     * Read next from from read buffer
     * @param timeout Maximum amount of time (milliseconds) to wait for frame;
     *                < 0 will use streamTimeout as value,
     *                0 will not wait and return null if no frame in buffer,
     *                > 0 will wait up to specified amt of time
     * @return StreamFrame or null if no frame received
     * @throws StreamHandlerStoppedException if handler stopped before/during call
     * @throws StreamClosedException if close frame received
     */
    private StreamFrame readNextFrame(long timeout) throws StreamHandlerStoppedException, StreamClosedException {
        // if timeout is less than 0, then block and wait to get next frame (up to stream timeout)
        StreamFrame frame;
        if (timeout < 0) {
            while (true) {
                try {
                    frame = readBuffer.pollFirst(settings.streamTimeout, TimeUnit.SECONDS);
                    break;
                } catch (InterruptedException e) {
//                    stop();
//                    throw new StreamHandlerStoppedException("handler is stopped; cannot read frames through a stopped handler");
                }
            }
        }
        // if timeout is 0, do not block and immediately return
        else if (timeout == 0) {
            frame = readBuffer.pollFirst();
        }
        // otherwise, wait up to specified time (bounded by stream timeout)
        else {
            while(true) {
                try {
                    frame = readBuffer.pollFirst(Math.min(timeout, settings.streamTimeout), TimeUnit.SECONDS);
                    break;
                } catch (InterruptedException e) {
                    //stop();
                    //throw new StreamHandlerStoppedException("handler is stopped; cannot read frames through a stopped handler");
                }
            }
        }
        // if frame not null
        if (frame != null) {
            // decrement read buffer counter
            decrementReadBuffer(frame);
            // decode frame data
            frame.decodeData(encodeHandler);
            // if close frame, throw exception
            if (frame.isClose()) {
                throw new StreamClosedException(new String(frame.getData(), StandardCharsets.UTF_8));
            }
            return frame;
        }
        // if handler is stopped and no additional frames to read, throw exception
        if (isStopped() && readBuffer.isEmpty()) {
            throw new StreamHandlerStoppedException("handler is stopped and no frames in read buffer; cannot read frames through a stopped handler");
        }
        // return null, since frame will be null here
        return null;
    }

    private StreamData readData(long timeout, long maxLength, boolean convertResponse) throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        List<StreamFrame> frames = new ArrayList<>();
        int totalLength = 0;
        boolean isResponse = false;

        while (true) {
            StreamFrame frame = readNextFrame(timeout);
            // if frame was null, stop listening for more frames
            if (frame == null) {
                break;
            }
            // add data
            frames.add(frame);
            totalLength += frame.getData().length;
            // check if max length provided and if past limit
            if (maxLength > 0 && totalLength > maxLength) {
                stop();
                throw new StreamTotalDataSizeException(
                        String.format("total data received has surpassed max_length of %d", maxLength));
            }
            if (frame.isResponse()) {
                isResponse = true;
            }
            // if frame is last, stop listening for more frames
            if (frame.isLast()) {
                break;
            }
        }
        // combine frame data
        byte[] finalArray = null;
        int index = 0;
        for (StreamFrame frame : frames) {
            if (finalArray == null) {
                finalArray = Arrays.copyOf(frame.getData(), totalLength);
            } else {
                System.arraycopy(frame.getData(), 0, finalArray, index, frame.getData().length);
            }
            index += frame.getData().length;
        }
        // convert to response, if necessary
        if (isResponse && convertResponse) {
            return new StreamData(CepticResponse.fromData(finalArray));
        } else {
            return new StreamData(finalArray);
        }
    }

    /**
     * Reads data combined from multiple frames with default timeout and no maxLength limit
     * @return StreamData object containing either byte array or CepticResponse (but NOT both)
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException will not happen, but still part of signature
     */
    public StreamData readData() throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readData(0);
    }

    /**
     * Reads data combined from multiple frames with default timeout
     * @param maxLength Maximum length of data to receive (unlimited if 0)
     * @return StreamData object containing either byte array or CepticResponse (but NOT both)
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException if maxLength exceeded
     */
    public StreamData readData(long maxLength) throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readData(settings.streamTimeout, maxLength);
    }

    /**
     * Reads data combined from multiple frames
     * @param timeout Maximum amount of time to wait for each frame
     * @param maxLength Maximum length of data to receive (unlimited if 0)
     * @return StreamData object containing either byte array or CepticResponse (but NOT both)
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException if maxLength exceeded
     */
    public StreamData readData(long timeout, long maxLength) throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readData(timeout, maxLength, true);
    }



    /**
     * Reads data combined from multiple frames always as byte array with default timeout and no maxLength limit
     * @return byte array
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException will not happen, but still part of signature
     */
    public byte[] readDataRaw() throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readDataRaw(0);
    }

    /**
     * Reads data combined from multiple frames always as byte array with default timeout
     * @param maxLength Maximum length of data to receive (unlimited if 0)
     * @return byte array
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException if maxLength exceeded
     */
    public byte[] readDataRaw(long maxLength) throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readDataRaw(settings.streamTimeout, maxLength);
    }

    /**
     * Reads data combined from multiple frames always as byte array
     * @param timeout Maximum amount of time to wait for each frame
     * @param maxLength Maximum length of data to receive (unlimited if 0)
     * @return byte array
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException if maxLength exceeded
     */
    public byte[] readDataRaw(long timeout, long maxLength) throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        return readData(timeout, maxLength, false).getData();
    }


    /**
     * Read header data from one or more frames as byte array
     * @return byte array
     * @throws StreamHandlerStoppedException if handler stopped before or during execution
     * @throws StreamClosedException if stream signalled to close during execution
     * @throws StreamTotalDataSizeException if maxLength exceeded
     */
    public byte[] readHeaderDataRaw() throws StreamHandlerStoppedException, StreamClosedException, StreamTotalDataSizeException {
        // length should be no more than: headersMaxSize + command + endpoint + 2x\r\n (4 bytes)
        return readDataRaw(settings.streamTimeout, settings.headersMaxSize + Constants.COMMAND_LENGTH + Constants.ENDPOINT_LENGTH + 4);
    }

    //endregion

    //region Keep Alive Timeout
    public boolean isTimedOut() {
        return keepAliveTimer.getTimeSeconds() > settings.streamTimeout;
    }

    public void updateKeepAlive() {
        keepAliveTimer.update();
    }
    //endregion

    //region Stopped
    public void stop() {
        shouldStop = true;
    }

    public boolean isStopped() {
        return shouldStop;
    }
    //endregion
}
