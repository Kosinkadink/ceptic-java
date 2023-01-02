package org.jedkos.ceptic.security.exceptions;

import org.jedkos.ceptic.common.exceptions.CepticException;

public class SecurityException extends CepticException {

    public SecurityException(String message) {
        super(message);
    }
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

}
