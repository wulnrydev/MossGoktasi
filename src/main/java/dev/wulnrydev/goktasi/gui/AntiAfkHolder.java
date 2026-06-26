package dev.wulnrydev.goktasi.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Anti-AFK GUI'sini diger envanterlerden ayirt etmek icin kullanilan
 * isaretci (marker) InventoryHolder. Listener bu tip uzerinden hangi
 * envanterin AFK menusu oldugunu O(1) anlar; baslik karsilastirmasi yapmaz.
 */
public final class AntiAfkHolder implements InventoryHolder {

    private final Player player;
    private final int correctSlot;
    private Inventory inventory;

    public AntiAfkHolder(Player player, int correctSlot) {
        this.player = player;
        this.correctSlot = correctSlot;
    }

    public Player getPlayer() {
        return player;
    }

    public int getCorrectSlot() {
        return correctSlot;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
