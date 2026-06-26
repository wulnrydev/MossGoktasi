package dev.wulnrydev.goktasi.manager;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Config'deki {@code items:} bolumunde tanimli, id'li ozel esyalari yonetir.
 * <p>
 * {@code /goktasi giveitem <id> [oyuncu] [adet]} komutu bu esyalari verir.
 * Esyalar yuklemede bir kez olusturulup saklanir; verirken klonlanir.
 */
public final class CustomItemManager {

    private final MossGoktasi plugin;

    // id (kucuk harf) -> hazir esya sablonu.
    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public CustomItemManager(MossGoktasi plugin) {
        this.plugin = plugin;
        load();
    }

    /** Config'deki tum ozel esyalari (yeniden) yukler. */
    public void load() {
        items.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("items");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Material mat = Material.matchMaterial(s.getString("material", ""));
            if (mat == null || !mat.isItem()) {
                plugin.getLogger().warning("Gecersiz ozel item materyali atlandi: " + id);
                continue;
            }
            int amount = Math.max(1, s.getInt("amount", 1));
            String name = s.getString("name", "");
            List<String> lore = s.getStringList("lore");
            int customModelData = Math.max(0, s.getInt("custom-model-data", 0));

            items.put(id.toLowerCase(Locale.ROOT), ItemFactory.build(mat, amount, name, lore, customModelData));
        }

        plugin.getLogger().info(items.size() + " ozel item yuklendi.");
    }

    /** Bir id tanimli mi? */
    public boolean exists(String id) {
        return id != null && items.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Verilen id'li esyayi oyuncuya verir.
     *
     * @param amount {@code > 0} ise miktari ezer; aksi halde config'deki miktar kullanilir.
     * @return esya verildiyse {@code true}, id yoksa {@code false}.
     */
    public boolean give(Player player, String id, int amount) {
        ItemStack template = (id == null) ? null : items.get(id.toLowerCase(Locale.ROOT));
        if (template == null) {
            return false;
        }
        ItemStack item = template.clone();
        if (amount > 0) {
            item.setAmount(amount);
        }
        // Envanter doluysa yere birak (kayip olmasin).
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        return true;
    }

    /** Tanimli tum esya id'leri. */
    public Set<String> getIds() {
        return Collections.unmodifiableSet(items.keySet());
    }
}
