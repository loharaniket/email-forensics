package com.emailforensics.backend.exception;

import com.emailforensics.backend.common.BadRequestException;

public class InvalidEmailException extends BadRequestException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
