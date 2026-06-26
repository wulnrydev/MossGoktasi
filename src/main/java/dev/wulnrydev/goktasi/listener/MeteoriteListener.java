package dev.wulnrydev.goktasi.listener;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Meteorite;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goktasina yapilan kirma islemlerini dinler.
 * <p>
 * Vurus mekanigi {@link BlockBreakEvent} uzerinden calisir: oyuncu blogu
 * GERCEKTEN kirdiginda (madencilik dongusunu tamamladiginda) bir "vurus"
 * sayilir ve can 1 duser. Blok asla yok edilmez; event her zaman iptal edilir,
 * boylece oyuncu blogu tekrar tekrar kirarak goktasini bitirir.
 * <p>
 * Not: Sol tik (PlayerInteractEvent) ARTIK sayilmaz; yalnizca gercek kirma
 * islemi gecerlidir.
 */
public final class MeteoriteListener implements Listener {

    private final MossGoktasi plugin;

    // Autoclicker'in cok hizli saymasini engellemek icin son vurus zamani.
    private final ConcurrentHashMap<UUID, Long> lastHit = new ConcurrentHashMap<>();

    public MeteoriteListener(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    /** Vurus cooldown'unu config'den canli okur (boylece /goktasi reload aninda gecerli olur). */
    private long hitCooldownMs() {
        return Math.max(0, plugin.getConfig().getLong("settings.hit-cooldown-ms", 0));
    }

    /**
     * Asil vurus: blogun kirilmasi. Goktasi blogu hicbir zaman gercekten
     * yok edilmez (event iptal edilir); her tamamlanan kirma bir can dusurur.
     * <p>
     * {@code ignoreCancelled} KULLANILMAZ: baska bir eklenti (or. WorldGuard
     * build korumasi) event'i iptal etse bile goktasi mantigi calismali ve
     * blok korunmali. Bolge kontrolu eklentinin kendi {@code region} ayariyla
     * yapilir.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Meteorite meteorite = plugin.getMeteoriteManager().getAt(block.getLocation());
        if (meteorite == null) {
            return; // Goktasi degil; karismayalim.
        }

        // Goktasi blogu asla kirilmaz.
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Yenileniyorsa vurus saymaz.
        if (!meteorite.isAlive()) {
            plugin.getMessageManager().sendRespawning(player);
            return;
        }

        // WorldGuard bolge kontrolu (acik ve region tanimliysa).
        if (!checkRegion(player, meteorite)) {
            plugin.getMessageManager().sendNoRegion(player);
            return;
        }

        // Autoclicker cooldown'u (config'de 0 ise atlanir).
        long cooldownMs = hitCooldownMs();
        if (cooldownMs > 0) {
            long now = System.currentTimeMillis();
            Long last = lastHit.get(player.getUniqueId());
            if (last != null && (now - last) < cooldownMs) {
                return;
            }
            lastHit.put(player.getUniqueId(), now);
        }

        // Anti-AFK sayaci: hedefe ulasildiysa GUI acilir ve bu vurus saymaz.
        if (plugin.getAntiAfkManager().isEnabled()) {
            plugin.getAntiAfkManager().registerHit(player);
            if (plugin.getAntiAfkManager().hasOpenMenu(player.getUniqueId())) {
                return; // Menu acildi; bu vurusu sayma.
            }
        }

        // Vurusu isle (can dusur, odul ver, gerekirse oldur).
        plugin.getMeteoriteManager().handleHit(player, meteorite);
    }

    private boolean checkRegion(Player player, Meteorite meteorite) {
        if (!plugin.getConfig().getBoolean("settings.worldguard.enabled", true)) {
            return true;
        }
        if (!meteorite.hasRegion()) {
            return true;
        }
        // Oyuncunun konumu region icinde mi? (Spec: oyuncu region disinda kiramaz.)
        return plugin.getWorldGuardHook().isInRegion(player.getLocation(), meteorite.getRegion());
    }

    /** Oyuncu cikinca cooldown verisini temizle (memory-leak korumasi). */
    public void clear(UUID uuid) {
        lastHit.remove(uuid);
    }
}
