package org.jedkos.ceptic.net.exceptions;

import org.jedkos.ceptic.common.exceptions.CepticException;

public class SocketCepticException extends CepticException {

    public SocketCepticException(String message) {
        super(message);
    }

    public SocketCepticException(String message, Throwable cause) {
        super(message, cause);
    }

}
