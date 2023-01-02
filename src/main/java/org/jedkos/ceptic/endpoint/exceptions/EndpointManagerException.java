package org.jedkos.ceptic.endpoint.exceptions;

import org.jedkos.ceptic.common.exceptions.CepticException;

public class EndpointManagerException extends CepticException {
    public EndpointManagerException(String message) {
        super(message);
    }

    public EndpointManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
