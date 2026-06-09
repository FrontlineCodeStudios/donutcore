
package ro.andreilarazboi.donutcore.crates;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public class GUIItemUtil {
    private final DonutCrates plugin;

    public GUIItemUtil(DonutCrates pl) {
        this.plugin = pl;
    }

    public ItemStack buildItemFromSection(ConfigurationSection it) {
        ItemStack stack;
        if (it.isItemStack("item")) {
            stack = it.getItemStack("item").clone();
        } else {
            int amount;
            Material mat = Material.valueOf((String)it.getString("material", "STONE"));
            stack = new ItemStack(mat, amount = it.getInt("amount", 1));
            ItemMeta im = stack.getItemMeta();
            if (im != null) {
                String name = it.getString("displayname", null);
                if (name != null) {
                    im.displayName(Utils.toComponent(name));
                }
                List<String> lore = it.getStringList("lore");
                if (!lore.isEmpty()) {
                    im.lore(Utils.toComponents(lore));
                }
                for (String ench : it.getStringList("enchantments")) {
                    String[] p = ench.split(";");
                    Enchantment e = Enchantment.getByName((String)p[0].trim());
                    if (e == null) continue;
                    int lvl = 1;
                    try {
                        lvl = Integer.parseInt(p[1].trim());
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    im.addEnchant(e, lvl, true);
                }
                if (it.getBoolean("unbreakable", false)) {
                    im.setUnbreakable(true);
                }
                if (im instanceof ArmorMeta) {
                    ArmorMeta am = (ArmorMeta)im;
                    if (it.isConfigurationSection("trim")) {
                        try {
                            NamespacedKey mns = NamespacedKey.fromString((String)it.getString("trim.material"));
                            NamespacedKey pns = NamespacedKey.fromString((String)it.getString("trim.pattern"));
                            TrimMaterial tm = (TrimMaterial)Registry.TRIM_MATERIAL.get(mns);
                            TrimPattern tp = (TrimPattern)Registry.TRIM_PATTERN.get(pns);
                            if (tm != null && tp != null) {
                                am.setTrim(new ArmorTrim(tm, tp));
                            }
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                    }
                }
                stack.setItemMeta(im);
            }
        }
        stack.setAmount(it.getInt("amount", Math.max(1, stack.getAmount())));
        return stack;
    }

    public ItemStack buildKeyItem(String crate) {
        ConfigurationSection sec = this.plugin.cfg.crates.getConfigurationSection("Keys." + crate + ".item");
        if (sec == null) {
            return new ItemStack(Material.TRIPWIRE_HOOK);
        }
        return this.buildItemFromSection(sec);
    }
}

