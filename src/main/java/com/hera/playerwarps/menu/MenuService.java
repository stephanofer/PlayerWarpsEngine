package com.hera.playerwarps.menu;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.config.MenuSettings;
import com.hera.playerwarps.config.ConfigManager.LoadedConfig;
import com.hera.playerwarps.menu.button.ConfiguredButton;
import com.hera.playerwarps.menu.loader.ManageWarpButtonLoader;
import com.hera.playerwarps.menu.loader.RefreshButtonLoader;
import com.hera.playerwarps.menu.loader.SearchButtonLoader;
import com.hera.playerwarps.menu.loader.ScopeButtonLoader;
import com.hera.playerwarps.menu.loader.SortButtonLoader;
import com.hera.playerwarps.menu.loader.WarpListButtonLoader;
import com.hera.playerwarps.menu.loader.WhitelistListButtonLoader;
import com.hera.playerwarps.util.Texts;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpAccessService;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpService;
import fr.maxlego08.menu.MenuPlugin;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.pattern.Pattern;
import fr.maxlego08.menu.api.pattern.PatternManager;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MenuService {

    private static final String[] INVENTORY_RESOURCES = new String[] {
            "menus/inventories/main.yml",
            "menus/inventories/manage.yml",
            "menus/inventories/whitelist.yml"
    };
    private static final String[] PATTERN_RESOURCES = new String[] {
            "menus/patterns/pwe_decoration.yml",
            "menus/patterns/pwe_pagination.yml"
    };

    private final PlayerWarpsPlugin plugin;
    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final WarpCache warpCache;
    private final WarpAccessService warpAccessService;
    private final WarpLimitService warpLimitService;
    private final VisitBuffer visitBuffer;
    private final WarpService warpService;
    private final MenuSessionStore sessionStore;
    private final WarpBrowseService browseService;
    private final MenuConfigValidator validator = new MenuConfigValidator();
    private final Map<String, Inventory> inventories = new HashMap<String, Inventory>();
    private final List<Pattern> loadedPatterns = new ArrayList<Pattern>();

    private MenuPlugin menuPlugin;
    private InventoryManager inventoryManager;
    private ButtonManager buttonManager;
    private PatternManager patternManager;

    public MenuService(PlayerWarpsPlugin plugin, ConfigManager configManager, PluginScheduler scheduler, WarpCache warpCache,
                       WarpAccessService warpAccessService, WarpLimitService warpLimitService, VisitBuffer visitBuffer, WarpService warpService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.warpCache = warpCache;
        this.warpAccessService = warpAccessService;
        this.warpLimitService = warpLimitService;
        this.visitBuffer = visitBuffer;
        this.warpService = warpService;
        this.sessionStore = new MenuSessionStore(configManager, scheduler);
        this.browseService = new WarpBrowseService(warpCache, warpAccessService, visitBuffer);
    }

    public void enable() {
        this.menuPlugin = MenuPlugin.getInstance();
        if (this.menuPlugin == null || !Bukkit.getPluginManager().isPluginEnabled("zMenu")) {
            throw new IllegalStateException("zMenu is required but is not enabled");
        }
        this.inventoryManager = this.menuPlugin.getInventoryManager();
        this.buttonManager = this.menuPlugin.getButtonManager();
        this.patternManager = this.menuPlugin.getPatternManager();

        ensureResources();
        validateCurrentFiles();
        validateFallbackIcon(this.configManager.settings().menuSettings());
        registerLoaders();
        loadMenus();
        this.sessionStore.start();
    }

    public void disable() {
        this.sessionStore.stop();
        if (this.inventoryManager != null) {
            this.inventoryManager.deleteInventories(this.plugin);
        }
        unregisterPatterns();
        if (this.buttonManager != null) {
            this.buttonManager.unregisters(this.plugin);
        }
        this.inventories.clear();
    }

    public void validateReload(LoadedConfig loadedConfig) {
        if (!this.configManager.settings().serverId().equals(loadedConfig.settings().serverId())) {
            throw new IllegalArgumentException("server-id changes require a full restart");
        }
        if (!this.configManager.databaseSettings().equals(loadedConfig.databaseSettings())) {
            throw new IllegalArgumentException("storage settings changes require a full restart");
        }
        validateFallbackIcon(loadedConfig.settings().menuSettings());
        validateCurrentFiles();
    }

    public void reloadMenus() {
        if (this.inventoryManager == null) {
            return;
        }
        validateCurrentFiles();
        this.inventoryManager.deleteInventories(this.plugin);
        unregisterPatterns();
        this.inventories.clear();
        loadMenus();
    }

    public MenuSession session(Player player) {
        return this.sessionStore.session(player);
    }

    public List<Warp> browse(Player player, MenuSession session) {
        return this.browseService.browse(player, session);
    }

    public void openMain(Player player) {
        open(player, "main", 1);
    }

    public void open(Player player, String menuName, int page) {
        String normalizedName = menuName.toLowerCase(Locale.ENGLISH);
        if ((normalizedName.equals("manage") || normalizedName.equals("whitelist")) && selectedWarp(player) == null) {
            this.configManager.messages().send(player, "messages.menu-context-missing");
            return;
        }
        Inventory inventory = this.inventories.get(normalizedName);
        if (inventory == null) {
            this.configManager.messages().send(player, "messages.menu-not-found");
            return;
        }
        this.inventoryManager.openInventory(player, inventory, Math.max(1, page));
    }

    public void refresh(Player player) {
        this.inventoryManager.updateInventory(player, this.plugin);
    }

    public void beginSearch(Player player) {
        this.sessionStore.beginSearch(player);
        this.configManager.messages().send(player, "messages.search-start");
        player.closeInventory();
    }

    public boolean consumeSearch(Player player, String query) {
        if (!this.sessionStore.consumeSearch(player)) {
            return false;
        }
        int maxLength = this.configManager.settings().menuSettings().searchMaxLength();
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }
        MenuSession session = session(player);
        session.query(normalized);
        open(player, "main", 1);
        return true;
    }

    public boolean hasPendingSearch(Player player) {
        return this.sessionStore.hasPendingSearch(player);
    }

    public void removeSession(Player player) {
        this.sessionStore.remove(player);
    }

    public void clearSearch(Player player) {
        session(player).query("");
        this.configManager.messages().send(player, "messages.search-cleared");
        open(player, "main", 1);
    }

    public void nextSort(Player player) {
        MenuSession session = session(player);
        session.sort(session.sort().next());
        open(player, "main", 1);
    }

    public void previousSort(Player player) {
        MenuSession session = session(player);
        session.sort(session.sort().previous());
        open(player, "main", 1);
    }

    public void toggleScope(Player player) {
        MenuSession session = session(player);
        session.scope(session.scope().next());
        open(player, "main", 1);
    }

    public void teleportFromMenu(Player player, long warpId) {
        Warp warp = this.warpCache.getById(warpId).orElse(null);
        if (warp == null) {
            this.configManager.messages().send(player, "messages.warp-not-found");
            refresh(player);
            return;
        }
        player.closeInventory();
        this.warpService.teleport(player, warp.name());
    }

    public void openManage(Player player, long warpId) {
        Warp warp = this.warpCache.getById(warpId).orElse(null);
        if (warp == null) {
            this.configManager.messages().send(player, "messages.warp-not-found");
            refresh(player);
            return;
        }
        if (!this.warpAccessService.canManage(player, warp)) {
            this.configManager.messages().send(player, "messages.not-warp-owner");
            return;
        }
        session(player).selectedWarpId(warpId);
        open(player, "manage", 1);
    }

    public void manageAction(Player player, String action) {
        MenuSession session = session(player);
        Warp warp = selectedWarp(player);
        if (warp == null) {
            this.configManager.messages().send(player, "messages.warp-not-found");
            openMain(player);
            return;
        }
        if (!this.warpAccessService.canManage(player, warp)) {
            this.configManager.messages().send(player, "messages.not-warp-owner");
            openMain(player);
            return;
        }

        String normalized = action == null ? "" : action.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.isEmpty() || normalized.equals("none")) {
            return;
        }
        if (normalized.equals("back")) {
            openMain(player);
            return;
        }
        if (normalized.equals("manage-back")) {
            open(player, "manage", 1);
            return;
        }
        if (normalized.equals("teleport")) {
            teleportFromMenu(player, warp.id());
            return;
        }
        if (normalized.equals("lock-toggle")) {
            this.warpService.setLocked(player, warp.name(), !warp.locked());
            refreshLater(player);
            return;
        }
        if (normalized.equals("whitelist-toggle")) {
            this.warpService.setWhitelistEnabled(player, warp.name(), !warp.whitelistEnabled());
            refreshLater(player);
            return;
        }
        if (normalized.equals("whitelist-open")) {
            open(player, "whitelist", 1);
            return;
        }
        if (normalized.equals("reset")) {
            if (!confirm(player, session, "reset:" + warp.id())) {
                return;
            }
            this.warpService.resetLocation(player, warp.name());
            refreshLater(player);
            return;
        }
        if (normalized.equals("remove")) {
            if (!confirm(player, session, "remove:" + warp.id())) {
                return;
            }
            player.closeInventory();
            this.warpService.removeWarp(player, warp.name(), null);
            return;
        }
        this.configManager.messages().send(player, "messages.menu-invalid-action");
    }

    public List<Map.Entry<UUID, String>> whitelistMembers(Player player) {
        Warp warp = selectedWarp(player);
        if (warp == null || !this.warpAccessService.canManage(player, warp)) {
            return Collections.emptyList();
        }
        return new ArrayList<Map.Entry<UUID, String>>(this.warpCache.whitelistMemberNames(warp.id()).entrySet());
    }

    public void removeWhitelistFromMenu(Player player, UUID memberUuid, String memberName) {
        Warp warp = selectedWarp(player);
        if (warp == null || !this.warpAccessService.canManage(player, warp)) {
            this.configManager.messages().send(player, "messages.not-warp-owner");
            openMain(player);
            return;
        }
        String value = memberName == null || memberName.trim().isEmpty() ? memberUuid.toString() : memberName;
        this.warpService.removeWhitelist(player, warp.name(), value);
        refreshLater(player);
    }

    public ItemStack buildSessionItem(Player player, ConfiguredButton button) {
        if (button.getItemStack() == null) {
            return fallbackItem();
        }
        return button.getItemStack().build(player, false, sessionPlaceholders(player));
    }

    public ItemStack buildWarpItem(Player player, ConfiguredButton button, Warp warp) {
        ItemStack itemStack = button.getItemStack() == null ? fallbackItem() : button.getItemStack().build(player, false, warpPlaceholders(player, warp));
        applyWarpIcon(itemStack, warp);
        return itemStack;
    }

    public ItemStack buildWhitelistItem(Player player, ConfiguredButton button, Map.Entry<UUID, String> member) {
        Placeholders placeholders = sessionPlaceholders(player);
        placeholders.register("member", member.getValue());
        placeholders.register("member_uuid", member.getKey().toString());
        return button.getItemStack() == null ? fallbackItem() : button.getItemStack().build(player, false, placeholders);
    }

    public Placeholders sessionPlaceholders(Player player) {
        MenuSession session = session(player);
        Placeholders placeholders = new Placeholders();
        placeholders.register("scope", session.scope() == WarpScope.MY_WARPS ? "mis warps" : "todos");
        placeholders.register("sort", sortName(session.sort()));
        placeholders.register("query", session.query().isEmpty() ? "sin búsqueda" : session.query());
        placeholders.register("total", String.valueOf(this.warpCache.totalWarps()));
        placeholders.register("amount", String.valueOf(this.warpCache.countByOwner(player.getUniqueId())));
        placeholders.register("limit", String.valueOf(this.warpLimitService.cached(player)));

        Warp selected = selectedWarp(player);
        if (selected != null) {
            registerWarp(placeholders, selected);
        }
        return placeholders;
    }

    private Placeholders warpPlaceholders(Player player, Warp warp) {
        Placeholders placeholders = sessionPlaceholders(player);
        registerWarp(placeholders, warp);
        return placeholders;
    }

    private void registerWarp(Placeholders placeholders, Warp warp) {
        placeholders.register("warp", warp.name());
        placeholders.register("owner", warp.ownerName());
        placeholders.register("description", warp.description() == null || warp.description().trim().isEmpty() ? "Sin descripción" : warp.description());
        placeholders.register("visits", String.valueOf(this.visitBuffer.effectiveVisits(warp)));
        placeholders.register("locked", warp.locked() ? "bloqueado" : "desbloqueado");
        placeholders.register("whitelist", warp.whitelistEnabled() ? "activada" : "desactivada");
        placeholders.register("world", warp.location().world());
    }

    private Warp selectedWarp(Player player) {
        Long selectedWarpId = session(player).selectedWarpId();
        return selectedWarpId == null ? null : this.warpCache.getById(selectedWarpId.longValue()).orElse(null);
    }

    private boolean confirm(Player player, MenuSession session, String key) {
        long now = System.currentTimeMillis();
        if (session.consumeConfirmation(key, now)) {
            return true;
        }
        long expiresAt = now + this.configManager.settings().menuSettings().confirmationTimeoutSeconds() * 1000L;
        session.requireConfirmation(key, expiresAt);
        this.configManager.messages().send(player, "messages.menu-confirm");
        return false;
    }

    private void refreshLater(final Player player) {
        this.scheduler.runLaterSync(new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    refresh(player);
                }
            }
        }, 2L);
    }

    private void registerLoaders() {
        this.buttonManager.register(new WarpListButtonLoader(this.plugin, this, "PWE_WARP_LIST", null));
        this.buttonManager.register(new WarpListButtonLoader(this.plugin, this, "PWE_MY_WARPS", WarpScope.MY_WARPS));
        this.buttonManager.register(new SearchButtonLoader(this.plugin, this));
        this.buttonManager.register(new RefreshButtonLoader(this.plugin, this));
        this.buttonManager.register(new ScopeButtonLoader(this.plugin, this));
        this.buttonManager.register(new SortButtonLoader(this.plugin, this));
        this.buttonManager.register(new ManageWarpButtonLoader(this.plugin, this));
        this.buttonManager.register(new WhitelistListButtonLoader(this.plugin, this));
    }

    private void loadMenus() {
        for (String patternResource : PATTERN_RESOURCES) {
            try {
                Pattern pattern = this.patternManager.loadPattern(file(patternResource));
                this.loadedPatterns.add(pattern);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load menu pattern " + patternResource + ": " + exception.getMessage(), exception);
            }
        }

        for (String inventoryResource : INVENTORY_RESOURCES) {
            try {
                Inventory inventory = this.inventoryManager.loadInventory(this.plugin, file(inventoryResource));
                this.inventories.put(nameOf(inventoryResource), inventory);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load menu inventory " + inventoryResource + ": " + exception.getMessage(), exception);
            }
        }
    }

    private void unregisterPatterns() {
        if (this.patternManager == null) {
            return;
        }
        for (Pattern pattern : this.loadedPatterns) {
            this.patternManager.unregisterPattern(pattern);
        }
        this.loadedPatterns.clear();
    }

    private void ensureResources() {
        for (String resource : PATTERN_RESOURCES) {
            saveResourceIfAbsent(resource);
        }
        for (String resource : INVENTORY_RESOURCES) {
            saveResourceIfAbsent(resource);
        }
    }

    private void saveResourceIfAbsent(String resource) {
        File file = file(resource);
        if (file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        this.plugin.saveResource(resource, false);
    }

    private void validateCurrentFiles() {
        for (String patternResource : PATTERN_RESOURCES) {
            this.validator.validatePattern(file(patternResource));
        }
        for (String inventoryResource : INVENTORY_RESOURCES) {
            this.validator.validateInventory(file(inventoryResource));
        }
    }

    private void validateFallbackIcon(MenuSettings settings) {
        this.validator.validateMaterial(settings.fallbackIconMaterial(), "menus.fallback-icon");
    }

    private void applyWarpIcon(ItemStack itemStack, Warp warp) {
        Material material = materialOrFallback(warp.iconMaterial());
        itemStack.setType(material);
        itemStack.setDurability(warp.iconMaterial() == null || Material.matchMaterial(warp.iconMaterial()) == null
                ? this.configManager.settings().menuSettings().fallbackIconData()
                : warp.iconData());
    }

    private Material materialOrFallback(String materialName) {
        Material material = materialName == null || materialName.trim().isEmpty() ? null : Material.matchMaterial(materialName.trim());
        if (material == null || material == Material.AIR) {
            material = Material.matchMaterial(this.configManager.settings().menuSettings().fallbackIconMaterial());
        }
        return material == null || material == Material.AIR ? Material.ENDER_PEARL : material;
    }

    private ItemStack fallbackItem() {
        Material material = materialOrFallback(null);
        ItemStack itemStack = new ItemStack(material, 1, this.configManager.settings().menuSettings().fallbackIconData());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Texts.color("&bWarp de jugador"));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private File file(String resource) {
        return new File(this.plugin.getDataFolder(), resource.replace('/', File.separatorChar));
    }

    private static String nameOf(String resource) {
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        return fileName.endsWith(".yml") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private static String sortName(WarpSort sort) {
        switch (sort) {
            case NEWEST:
                return "más recientes";
            case OLDEST:
                return "más antiguos";
            case MOST_VISITS:
                return "más visitados";
            case ALPHABETICAL:
                return "alfabético";
            case OWNER:
                return "dueño";
            default:
                return sort.configName();
        }
    }
}
