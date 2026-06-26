package dev.wulnrydev.goktasi.listener;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Meteorite;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goktasina yapilan vuruslari dinler.
 * <ul>
 *     <li>{@link BlockBreakEvent}: goktasinin asla kirilmamasini garanti eder
 *         (iptal eder). Sayim burada YAPILMAZ; cift sayimi onlemek icin.</li>
 *     <li>{@link PlayerInteractEvent} (SOL TIK): asil vurus mekanigi. Her sol
 *         tikta can 1 duser.</li>
 * </ul>
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
     * Goktasi blogunun gercekten kirilmasini her zaman engeller.
     * (Survival modda obsidian gibi bloklar tamamen kirilirsa diye guvenlik agi.)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Meteorite meteorite = plugin.getMeteoriteManager().getAt(event.getBlock().getLocation());
        if (meteorite != null) {
            event.setCancelled(true);
        }
    }

    /** Asil vurus: sol tik ile blok kirma eylemi. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Meteorite meteorite = plugin.getMeteoriteManager().getAt(block.getLocation());
        if (meteorite == null) {
            return; // Goktasi degil; karismayalim.
        }

        // Goktasi etkilesimi: vanilla kirma animasyonunu/etkilesimini iptal et.
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
