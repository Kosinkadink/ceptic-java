package org.jedkos.ceptic.stream.exceptions;

import org.jedkos.ceptic.common.exceptions.CepticException;

public class StreamException extends CepticException {
    public StreamException(String message) {
        super(message);
    }

    public StreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
