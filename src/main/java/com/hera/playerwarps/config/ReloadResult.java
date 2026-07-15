package com.hera.playerwarps.config;

public final class ReloadResult {

    private final boolean success;
    private final String error;

    private ReloadResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public static ReloadResult success() {
        return new ReloadResult(true, "");
    }

    public static ReloadResult failure(String error) {
        return new ReloadResult(false, error == null ? "unknown error" : error);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String error() {
        return this.error;
    }
}
