package ro.andreilarazboi.donutcore.sell;

import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

@SuppressWarnings({"deprecation", "removal"})
public class GuiClickListener
implements Listener {
    private final DonutSell plugin;

    public GuiClickListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!this.plugin.isModuleActive()) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof GuiHolder)) {
            return;
        }
        e.setCancelled(true);
        GuiHolder holder = (GuiHolder)e.getView().getTopInventory().getHolder();
        Player p = (Player)e.getWhoClicked();
        int slot = e.getRawSlot();
        FileConfiguration cfg = this.plugin.getConfig();
        if (holder.getPage() == -1) {
            String cat = holder.getCategoryKey();
            ProgressGui.CategoryIcon ico = this.plugin.getProgressGui().categoryIcons.get(cat);
            int backSlot = this.plugin.getMenusConfig().getInt("progress-menu.back-button.slot", 45);
            if (ico != null && slot == ico.slot) {
                this.playClickSound(p, cfg);
                this.plugin.runAtPlayerLater(p, () -> new CategoryGui(this.plugin, cat).open(p, 0), 1L);
                return;
            }
            if (slot == backSlot) {
                this.playClickSound(p, cfg);
                if (this.plugin.isSellMultiMenuEnabled()) {
                    this.plugin.getSellGui().openNew(p);
                } else {
                    this.plugin.getSellGui().open(p);
                }
                return;
            }
            return;
        }
        String cat = holder.getCategoryKey();
        int page = holder.getPage();
        int prevSlot = this.plugin.getMenusConfig().getInt("category-menu.previous-page-slot", 49);
        int nextSlot = this.plugin.getMenusConfig().getInt("category-menu.next-page-slot", 51);
        int backSlot = this.plugin.getMenusConfig().getInt("category-menu.back-button.slot", 45);
        int rows = this.plugin.getMenusConfig().getInt("category-menu.rows", 6);
        int perPage = (rows - 1) * 9;
        if (slot >= 0 && slot < perPage) {
            if (e.getView().getTopInventory().getItem(slot) != null) {
                this.playClickSound(p, cfg);
            }
            return;
        }
        if (slot == prevSlot && page > 0) {
            this.plugin.runAtPlayerLater(p, () -> new CategoryGui(this.plugin, cat).open(p, page - 1), 1L);
            return;
        }
        if (slot == nextSlot) {
            int totalEntries = 0;
            if (this.plugin.getWorthConfig().getList("categories." + cat) != null) {
                for (Object element : this.plugin.getWorthConfig().getList("categories." + cat)) {
                    if (!(element instanceof Map)) continue;
                    Map<?, ?> m = (Map<?, ?>)element;
                    totalEntries += m.size();
                }
            }
            if (page < (int)Math.ceil((double)totalEntries / (double)perPage) - 1) {
                this.plugin.runAtPlayerLater(p, () -> new CategoryGui(this.plugin, cat).open(p, page + 1), 1L);
            }
            return;
        }
        if (slot == backSlot) {
            this.playClickSound(p, cfg);
            this.plugin.getProgressGui().open(p, cat);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!this.plugin.isModuleActive()) return;
        if (e.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            e.setCancelled(true);
        }
    }

    private void playClickSound(Player p, FileConfiguration cfg) {
        Sound snd;
        String raw = cfg.getString("sounds.click-sound", "UI_BUTTON_CLICK");
        try {
            snd = Sound.valueOf((String)raw.toUpperCase());
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("Invalid sounds.click-sound: '" + raw + "'. Using UI_BUTTON_CLICK instead.");
            snd = Sound.UI_BUTTON_CLICK;
        }
        p.playSound(p.getLocation(), snd, 1.0f, 1.0f);
    }
}

