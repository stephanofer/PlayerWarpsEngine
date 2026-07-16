package com.hera.playerwarps.menu;

public enum WarpScope {
    ACCESSIBLE,
    MY_WARPS;

    public WarpScope next() {
        return this == ACCESSIBLE ? MY_WARPS : ACCESSIBLE;
    }
}
