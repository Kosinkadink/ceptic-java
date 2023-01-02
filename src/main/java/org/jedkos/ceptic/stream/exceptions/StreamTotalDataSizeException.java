package org.jedkos.ceptic.stream.exceptions;

public class StreamTotalDataSizeException extends StreamException {
    public StreamTotalDataSizeException(String message) {
        super(message);
    }

    public StreamTotalDataSizeException(String message, Throwable cause) {
        super(message, cause);
    }
}
