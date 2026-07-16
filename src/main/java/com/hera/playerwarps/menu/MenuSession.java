package com.hera.playerwarps.menu;

public final class MenuSession {

    private WarpScope scope;
    private WarpSort sort;
    private String query;
    private Long selectedWarpId;
    private String pendingConfirmation;
    private long confirmationExpiresAt;
    private long lastAccessAt;

    MenuSession(WarpSort defaultSort) {
        this.scope = WarpScope.ACCESSIBLE;
        this.sort = defaultSort;
        this.query = "";
        touch();
    }

    public WarpScope scope() {
        return this.scope;
    }

    public void scope(WarpScope scope) {
        this.scope = scope;
        touch();
    }

    public WarpSort sort() {
        return this.sort;
    }

    public void sort(WarpSort sort) {
        this.sort = sort;
        touch();
    }

    public String query() {
        return this.query;
    }

    public void query(String query) {
        this.query = query == null ? "" : query;
        touch();
    }

    public Long selectedWarpId() {
        return this.selectedWarpId;
    }

    public void selectedWarpId(Long selectedWarpId) {
        this.selectedWarpId = selectedWarpId;
        touch();
    }

    public void requireConfirmation(String key, long expiresAt) {
        this.pendingConfirmation = key;
        this.confirmationExpiresAt = expiresAt;
        touch();
    }

    public boolean consumeConfirmation(String key, long now) {
        boolean confirmed = key != null && key.equals(this.pendingConfirmation) && now <= this.confirmationExpiresAt;
        this.pendingConfirmation = null;
        this.confirmationExpiresAt = 0L;
        touch();
        return confirmed;
    }

    public long lastAccessAt() {
        return this.lastAccessAt;
    }

    public void touch() {
        this.lastAccessAt = System.currentTimeMillis();
    }
}
