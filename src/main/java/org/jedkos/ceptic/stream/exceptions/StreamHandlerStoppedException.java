package org.jedkos.ceptic.stream.exceptions;

public class StreamHandlerStoppedException extends StreamException {
    public StreamHandlerStoppedException(String message) {
        super(message);
    }

    public StreamHandlerStoppedException(String message, Throwable cause) {
        super(message, cause);
    }
}
