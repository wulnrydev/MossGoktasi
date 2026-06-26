package dev.wulnrydev.goktasi.manager;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Reward;
import dev.wulnrydev.goktasi.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Odul listelerini sans faktorune gore degerlendirip uygular.
 * Tum islemler main thread'de cagrilmalidir (komut/inventory islemleri).
 */
public final class RewardManager {

    private final MossGoktasi plugin;

    public RewardManager(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    /**
     * Verilen odul listesindeki her odulu sans yuzdesine gore degerlendirir;
     * sansi tutanlari oyuncuya verir.
     */
    public void grant(Player player, List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        for (Reward reward : rewards) {
            double roll = ThreadLocalRandom.current().nextDouble(100.0);
            if (roll <= reward.getChance()) {
                apply(player, reward);
            }
        }
    }

    private void apply(Player player, Reward reward) {
        switch (reward.getType()) {
            case COMMAND -> {
                String cmd = reward.getCommand().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            case ITEM -> {
                ItemStack item = new ItemStack(reward.getMaterial(), reward.getAmount());
                // Ozel isim / lore / custom model data (varsa) uygula.
                if (reward.hasCustomModelData() || reward.hasDisplayName() || reward.hasLore()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (reward.hasCustomModelData()) {
                            meta.setCustomModelData(reward.getCustomModelData());
                        }
                        if (reward.hasDisplayName()) {
                            meta.setDisplayName(TextUtil.color(reward.getDisplayName()));
                        }
                        if (reward.hasLore()) {
                            meta.setLore(TextUtil.color(reward.getLore()));
                        }
                        item.setItemMeta(meta);
                    }
                }
                // Envanter doluysa yere birak (kayip olmasin).
                player.getInventory().addItem(item).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        }
    }
}
