package com.hera.playerwarps.teleport;

public final class SafetyResult {

    private static final SafetyResult SAFE = new SafetyResult(true, "");

    private final boolean safe;
    private final String messageKey;

    private SafetyResult(boolean safe, String messageKey) {
        this.safe = safe;
        this.messageKey = messageKey;
    }

    public static SafetyResult safe() {
        return SAFE;
    }

    public static SafetyResult unsafe(String messageKey) {
        return new SafetyResult(false, messageKey);
    }

    public boolean isSafe() {
        return this.safe;
    }

    public String messageKey() {
        return this.messageKey;
    }
}
