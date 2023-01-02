package org.jedkos.ceptic.stream.exceptions;

public class StreamFrameSizeException extends StreamException {
    public StreamFrameSizeException(String message) {
        super(message);
    }

    public StreamFrameSizeException(String message, Throwable cause) {
        super(message, cause);
    }
}
