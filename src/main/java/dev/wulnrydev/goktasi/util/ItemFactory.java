package dev.wulnrydev.goktasi.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Ortak esya olusturucu: materyal + miktar + (istege bagli) isim, lore ve
 * custom model data'dan bir {@link ItemStack} uretir.
 * <p>
 * Hem odul esyalari (RewardManager) hem de id'li ozel esyalar (CustomItemManager)
 * tarafindan kullanilir; boylece esya olusturma mantigi tek yerde toplanir.
 */
public final class ItemFactory {

    private ItemFactory() {
        // Yardimci sinif; ornek olusturulamaz.
    }

    /**
     * Verilen ozelliklerle bir esya olusturur. Isim/lore renk kodlarini
     * ({@code &a}, {@code &#RRGGBB}) destekler. Bos/sifir alanlar atlanir.
     */
    public static ItemStack build(Material material, int amount, String name, List<String> lore, int customModelData) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));

        boolean hasName = name != null && !name.isEmpty();
        boolean hasLore = lore != null && !lore.isEmpty();
        boolean hasModelData = customModelData > 0;

        if (hasName || hasLore || hasModelData) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (hasModelData) {
                    meta.setCustomModelData(customModelData);
                }
                if (hasName) {
                    meta.setDisplayName(TextUtil.color(name));
                }
                if (hasLore) {
                    meta.setLore(TextUtil.color(lore));
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
