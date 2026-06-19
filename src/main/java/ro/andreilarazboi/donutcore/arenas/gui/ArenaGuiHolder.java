package ro.andreilarazboi.donutcore.arenas.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ArenaGuiHolder implements InventoryHolder {

    private final String type;      // "list" | "detail" | "trigger-list" | "trigger-detail"
    private final String arenaName; // null for "list"
    private final String extra;     // trigger name, only set for "trigger-detail"

    public ArenaGuiHolder(String type, String arenaName) {
        this(type, arenaName, null);
    }

    public ArenaGuiHolder(String type, String arenaName, String extra) {
        this.type      = type;
        this.arenaName = arenaName;
        this.extra     = extra;
    }

    public String getType()      { return type; }
    public String getArenaName() { return arenaName; }
    public String getExtra()     { return extra; }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException();
    }
}
