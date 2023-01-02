package org.jedkos.ceptic.stream.exceptions;

public class StreamClosedException extends StreamException {
    public StreamClosedException(String message) {
        super(message);
    }

    public StreamClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
