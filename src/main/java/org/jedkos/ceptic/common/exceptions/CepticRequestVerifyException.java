package org.jedkos.ceptic.common.exceptions;

public class CepticRequestVerifyException extends CepticException {
    public CepticRequestVerifyException(String message) {
        super(message);
    }

    public CepticRequestVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
