package com.hera.playerwarps.command;

import com.hera.playerwarps.warp.Warp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ListSubCommand implements SubCommand {

    private static final int PAGE_SIZE = 8;

    @Override
    public String name() {
        return "list";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.list";
    }

    @Override
    public void execute(CommandContext context) {
        int page = 1;
        String ownerFilter = null;

        if (context.args().length >= 2) {
            Integer parsedPage = parsePage(context.args()[1]);
            if (parsedPage == null) {
                ownerFilter = context.args()[1];
            } else {
                page = parsedPage.intValue();
            }
        }
        if (context.args().length >= 3) {
            ownerFilter = context.args()[2];
        }

        if (page <= 0) {
            context.messages().send(context.sender(), "messages.list-invalid-page");
            return;
        }

        List<Warp> warps = new ArrayList<Warp>();
        String normalizedOwner = ownerFilter == null ? null : ownerFilter.toLowerCase(Locale.ENGLISH);
        for (Warp warp : context.services().warpCache().allSnapshot()) {
            if (normalizedOwner != null && !warp.ownerName().toLowerCase(Locale.ENGLISH).equals(normalizedOwner)) {
                continue;
            }
            warps.add(warp);
        }

        if (warps.isEmpty()) {
            context.messages().send(context.sender(), "messages.list-empty");
            return;
        }

        Collections.sort(warps, new Comparator<Warp>() {
            @Override
            public int compare(Warp first, Warp second) {
                return first.name().compareToIgnoreCase(second.name());
            }
        });

        int pages = Math.max(1, (int) Math.ceil(warps.size() / (double) PAGE_SIZE));
        if (page > pages) {
            page = pages;
        }

        Map<String, String> headerPlaceholders = new HashMap<String, String>();
        headerPlaceholders.put("page", String.valueOf(page));
        headerPlaceholders.put("pages", String.valueOf(pages));
        context.messages().send(context.sender(), "messages.list-header", headerPlaceholders);

        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(warps.size(), from + PAGE_SIZE);
        for (int index = from; index < to; index++) {
            Warp warp = warps.get(index);
            Map<String, String> linePlaceholders = new HashMap<String, String>();
            linePlaceholders.put("warp", warp.name());
            linePlaceholders.put("owner", warp.ownerName());
            linePlaceholders.put("visits", String.valueOf(warp.visits()));
            context.messages().send(context.sender(), "messages.list-line", linePlaceholders);
        }
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (context.args().length != 3) {
            return Collections.emptyList();
        }

        String prefix = context.args()[2].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<String>();
        for (Warp warp : context.services().warpCache().allSnapshot()) {
            String ownerName = warp.ownerName();
            if (ownerName.toLowerCase(Locale.ENGLISH).startsWith(prefix) && !suggestions.contains(ownerName)) {
                suggestions.add(ownerName);
            }
        }
        return suggestions;
    }

    private Integer parsePage(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
