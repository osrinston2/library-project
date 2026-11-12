package com.library.exception;

public class InvalidBookReturnException extends RuntimeException {

    public InvalidBookReturnException(String message) {
        super(message);
    }
}

