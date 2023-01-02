package org.jedkos.ceptic.security.exceptions;

public class SecurityPEMException extends SecurityException {

    public SecurityPEMException(String message) {
        super(message);
    }

    public SecurityPEMException(String message, Throwable cause) {
        super(message, cause);
    }
}
