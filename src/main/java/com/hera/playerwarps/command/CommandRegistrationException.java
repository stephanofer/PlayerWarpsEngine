package com.hera.playerwarps.command;

public final class CommandRegistrationException extends Exception {

    public CommandRegistrationException(String message) {
        super(message);
    }

    public CommandRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
