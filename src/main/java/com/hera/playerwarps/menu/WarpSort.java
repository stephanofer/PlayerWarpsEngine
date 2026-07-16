package com.hera.playerwarps.menu;

import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;

import java.util.Comparator;
import java.util.Locale;

public enum WarpSort {
    NEWEST("newest"),
    OLDEST("oldest"),
    MOST_VISITS("most-visits"),
    ALPHABETICAL("alphabetical"),
    OWNER("owner");

    private final String configName;

    WarpSort(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return this.configName;
    }

    public static WarpSort from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        for (WarpSort sort : values()) {
            if (sort.configName.equals(normalized)) {
                return sort;
            }
        }
        return NEWEST;
    }

    public WarpSort next() {
        WarpSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public WarpSort previous() {
        WarpSort[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public Comparator<Warp> comparator(final VisitBuffer visitBuffer) {
        Comparator<Warp> tieBreaker = new Comparator<Warp>() {
            @Override
            public int compare(Warp first, Warp second) {
                int byName = first.name().compareToIgnoreCase(second.name());
                if (byName != 0) {
                    return byName;
                }
                return Long.compare(first.id(), second.id());
            }
        };

        if (this == OLDEST) {
            return compareLongAscending(new LongValue() {
                @Override
                public long value(Warp warp) {
                    return warp.createdAt();
                }
            }, tieBreaker);
        }
        if (this == MOST_VISITS) {
            return compareLongDescending(new LongValue() {
                @Override
                public long value(Warp warp) {
                    return visitBuffer.effectiveVisits(warp);
                }
            }, tieBreaker);
        }
        if (this == ALPHABETICAL) {
            return tieBreaker;
        }
        if (this == OWNER) {
            return new Comparator<Warp>() {
                @Override
                public int compare(Warp first, Warp second) {
                    int byOwner = normalize(first.ownerName()).compareTo(normalize(second.ownerName()));
                    if (byOwner != 0) {
                        return byOwner;
                    }
                    return tieBreaker.compare(first, second);
                }
            };
        }
        return compareLongDescending(new LongValue() {
            @Override
            public long value(Warp warp) {
                return warp.createdAt();
            }
        }, tieBreaker);
    }

    private static Comparator<Warp> compareLongAscending(final LongValue value, final Comparator<Warp> tieBreaker) {
        return new Comparator<Warp>() {
            @Override
            public int compare(Warp first, Warp second) {
                int compared = Long.compare(value.value(first), value.value(second));
                return compared == 0 ? tieBreaker.compare(first, second) : compared;
            }
        };
    }

    private static Comparator<Warp> compareLongDescending(final LongValue value, final Comparator<Warp> tieBreaker) {
        return new Comparator<Warp>() {
            @Override
            public int compare(Warp first, Warp second) {
                int compared = Long.compare(value.value(second), value.value(first));
                return compared == 0 ? tieBreaker.compare(first, second) : compared;
            }
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }

    private interface LongValue {
        long value(Warp warp);
    }
}
