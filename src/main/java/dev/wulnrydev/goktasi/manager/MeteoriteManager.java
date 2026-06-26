package dev.wulnrydev.goktasi.manager;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Meteorite;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tum goktaslarini yukler, durumlarini yonetir ve TEK bir global task ile
 * (her goktasi icin ayri task ACMADAN) respawn/hologram dongusunu calistirir.
 * <p>
 * Bu tasarim CPU dostudur: 100 goktasi olsa bile saniyede yalnizca 1 task
 * tetiklenir ve yalnizca "olu" (yenilenen) goktaslari isler.
 */
public final class MeteoriteManager {

    private final MossGoktasi plugin;

    // Konum-tabanli hizli arama icin: "world,x,y,z" -> Meteorite
    private final Map<String, Meteorite> byLocation = new LinkedHashMap<>();
    private final List<Meteorite> meteorites = new ArrayList<>();

    private BukkitTask task;

    public MeteoriteManager(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Yukleme / bosaltma
    // ------------------------------------------------------------------

    /** Config'den tum goktaslarini yukler ve dunyada canli olarak yerlestirir. */
    public void load() {
        unload();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("meteorites");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            Meteorite meteorite = Meteorite.fromSection(id, root.getConfigurationSection(id));
            if (meteorite == null) {
                plugin.getLogger().warning("Gecersiz goktasi yapilandirmasi atlandi: " + id);
                continue;
            }
            meteorites.add(meteorite);
            byLocation.put(key(meteorite), meteorite);

            // Canli blogu yerlestir ve hologrami olustur.
            spawnAlive(meteorite);
        }

        plugin.getLogger().info(meteorites.size() + " goktasi yuklendi.");
    }

    /** Tum goktaslarini ve hologramlari kaldirir (reload/disable icin). */
    public void unload() {
        stopTask();
        for (Meteorite m : meteorites) {
            plugin.getHologramManager().remove(m);
        }
        meteorites.clear();
        byLocation.clear();
    }

    /** Global respawn/hologram task'ini baslatir. */
    public void startTask() {
        stopTask();
        int interval = Math.max(1, plugin.getConfig().getInt("settings.task-interval-ticks", 20));
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // ------------------------------------------------------------------
    //  Global tick (saniyede 1) - sadece olu goktaslari isler
    // ------------------------------------------------------------------
    private void tick() {
        for (Meteorite m : meteorites) {
            if (m.isAlive()) {
                continue; // Canli goktaslari icin hicbir is yok (CPU tasarrufu).
            }
            int left = m.getRespawnSecondsLeft() - 1;
            if (left <= 0) {
                respawn(m);
            } else {
                m.setRespawnSecondsLeft(left);
                // Hologramda kalan sureyi gunceller (asenkron).
                plugin.getHologramManager().updateRespawning(m, left);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Durum gecisleri
    // ------------------------------------------------------------------

    /** Goktasini canli haline getirir: blogu koyar, cani fulleyip hologrami olusturur. */
    private void spawnAlive(Meteorite m) {
        Location loc = m.getLocation();
        if (loc == null) {
            plugin.getLogger().warning("Goktasi '" + m.getId() + "' icin dunya bulunamadi.");
            return;
        }
        m.setAlive(true);
        m.setCurrentHealth(m.getMaxHealth());
        loc.getBlock().setType(m.getBlock(), false);
        plugin.getHologramManager().create(m);
    }

    /** Goktasini olduruyor: BEDROCK'a cevirir ve respawn sayacini baslatir. */
    private void die(Meteorite m) {
        m.setAlive(false);
        m.setRespawnSecondsLeft(m.getRespawnTime());

        Location loc = m.getLocation();
        if (loc != null) {
            loc.getBlock().setType(Material.BEDROCK, false);
        }
        plugin.getHologramManager().updateRespawning(m, m.getRespawnTime());
    }

    /** Yenilenme bitti: tekrar orijinal bloguna doner, can fullenir, duyuru yapilir. */
    private void respawn(Meteorite m) {
        spawnAlive(m);
        // Sadece gercek yenilenmede duyuru yap (ilk yuklemede degil).
        plugin.getMessageManager().broadcastRespawn(m.getDisplayName(), m.getRespawnMessage());
    }

    // ------------------------------------------------------------------
    //  Vurus mantigi (listener tarafindan cagrilir, main thread)
    // ------------------------------------------------------------------

    /**
     * Oyuncunun goktasina bir vurusunu isler.
     * <ul>
     *     <li>Can 1 azalir.</li>
     *     <li>Normal vurus odulleri verilir.</li>
     *     <li>Can finish-threshold'a duserse finish odulleri verilir.</li>
     *     <li>Can 0 olunca goktasi olur (BEDROCK).</li>
     * </ul>
     */
    public void handleHit(Player player, Meteorite m) {
        int newHealth = m.getCurrentHealth() - 1;
        m.setCurrentHealth(Math.max(0, newHealth));

        RewardManager rewards = plugin.getRewardManager();

        // Normal vurus odulleri her vuruste.
        rewards.grant(player, m.getHitRewards());

        // Finish odulleri: son N vurusta verilir.
        //   threshold = 0 -> sadece olduren vurus (newHealth < 1)
        //   threshold = 3 -> son 3 vurus    (newHealth 2,1,0)
        if (newHealth < Math.max(1, m.getFinishThreshold())) {
            rewards.grant(player, m.getFinishRewards());
        }

        if (newHealth <= 0) {
            // Olduren vurus: siralamada bu oyuncuya +1 goktasi.
            plugin.getStatsManager().addKill(player);
            die(m);
        } else {
            // Can degisti -> hologrami gunceller (asenkron).
            plugin.getHologramManager().updateAlive(m);
        }
    }

    // ------------------------------------------------------------------
    //  Arama / yardimcilar
    // ------------------------------------------------------------------

    /** Verilen konumdaki goktasini doner (yoksa null). O(1). */
    public Meteorite getAt(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return byLocation.get(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    private String key(Meteorite m) {
        Location l = m.getLocation();
        String world = l != null ? l.getWorld().getName() : "?";
        return world + "," + (l != null ? l.getBlockX() : 0) + "," + (l != null ? l.getBlockY() : 0) + "," + (l != null ? l.getBlockZ() : 0);
    }

    public Collection<Meteorite> getMeteorites() {
        return meteorites;
    }
}
