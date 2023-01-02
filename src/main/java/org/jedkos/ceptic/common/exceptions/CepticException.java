package org.jedkos.ceptic.common.exceptions;

public class CepticException extends Exception {

    public CepticException(String message) {
        super(message);
    }

    public CepticException(String message, Throwable cause) {
        super(message, cause);
    }

}
