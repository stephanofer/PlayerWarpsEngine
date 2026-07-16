package com.hera.playerwarps.warp;

public final class WarpAccessResult {

    private static final WarpAccessResult ALLOWED = new WarpAccessResult(true, null);

    private final boolean allowed;
    private final String messageKey;

    private WarpAccessResult(boolean allowed, String messageKey) {
        this.allowed = allowed;
        this.messageKey = messageKey;
    }

    public static WarpAccessResult allowed() {
        return ALLOWED;
    }

    public static WarpAccessResult denied(String messageKey) {
        return new WarpAccessResult(false, messageKey);
    }

    public boolean isAllowed() {
        return this.allowed;
    }

    public String messageKey() {
        return this.messageKey;
    }
}
