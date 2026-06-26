package dev.wulnrydev.goktasi.listener;

import dev.wulnrydev.goktasi.MossGoktasi;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Oyuncu sunucudan ciktiginda ona ait tum gecici verileri (anti-afk sayaclari,
 * vurus cooldown'lari) temizleyerek memory-leak olusmasini engeller.
 */
public final class PlayerCleanupListener implements Listener {

    private final MossGoktasi plugin;
    private final MeteoriteListener meteoriteListener;

    public PlayerCleanupListener(MossGoktasi plugin, MeteoriteListener meteoriteListener) {
        this.plugin = plugin;
        this.meteoriteListener = meteoriteListener;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        plugin.getAntiAfkManager().clear(uuid);
        meteoriteListener.clear(uuid);
    }
}
