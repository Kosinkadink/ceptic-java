package org.jedkos.ceptic.stream.exceptions;

public class StreamTimeoutException extends StreamException {
    public StreamTimeoutException(String message) {
        super(message);
    }

    public StreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
