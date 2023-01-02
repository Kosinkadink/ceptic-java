package org.jedkos.ceptic.encode.exceptions;

import org.jedkos.ceptic.common.exceptions.CepticException;

public class UnknownEncodingException extends CepticException {

    public UnknownEncodingException(String message) {
        super(message);
    }

    public UnknownEncodingException(String message, Throwable cause) {
        super(message, cause);
    }

}
