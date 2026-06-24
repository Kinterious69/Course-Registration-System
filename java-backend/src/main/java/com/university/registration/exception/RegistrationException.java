package com.university.registration.exception;

/**
 * Base exception for all registration system errors.
 * All custom exceptions extend this class (checked exception hierarchy).
 */
public class RegistrationException extends Exception {

    private final String errorCode;

    public RegistrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RegistrationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
