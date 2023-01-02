package org.jedkos.ceptic.common.exceptions;

public class CepticIOException extends CepticException {
    public CepticIOException(String message) {
        super(message);
    }

    public CepticIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
