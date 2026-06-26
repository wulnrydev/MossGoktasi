package dev.wulnrydev.goktasi.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Tek bir goktasini (meteorite) temsil eden veri objesi.
 * <p>
 * Konfigurasyondan okunan sabit ayarlari (konum, blok, can vb.) ve calisma
 * sirasinda degisen durumu (mevcut can, alive/dead, respawn sayaci) tutar.
 * <p>
 * Durum alanlari ({@code currentHealth}, {@code alive}, {@code respawnSecondsLeft})
 * yalnizca ana thread (Bukkit scheduler / event) tarafindan degistirildigi icin
 * ek senkronizasyona gerek yoktur.
 */
public final class Meteorite {

    // ----- Kimlik / sabit ayarlar -----
    private final String id;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Material block;
    private final int maxHealth;
    private final int respawnTime;
    private final int finishThreshold;
    private final String region;

    // ----- Goruntu / duyuru -----
    private final String displayName;
    // Bos ise global messages.respawn-broadcast kullanilir; "NONE" ise duyuru kapanir.
    private final String respawnMessage;

    // ----- Hologram ayarlari -----
    private final double hologramHeight;
    private final List<String> hologramLines;
    private final List<String> respawnLines;

    // ----- Oduller -----
    private final List<Reward> hitRewards;
    private final List<Reward> finishRewards;

    // ----- Calisma zamani durumu (sadece main thread) -----
    private int currentHealth;
    private boolean alive = true;
    private int respawnSecondsLeft = 0;

    // Onbellege alinmis konum (her seferinde olusturmamak icin).
    private Location cachedLocation;

    private Meteorite(String id, String worldName, int x, int y, int z, Material block,
                      int maxHealth, int respawnTime, int finishThreshold, String region,
                      String displayName, String respawnMessage,
                      double hologramHeight, List<String> hologramLines, List<String> respawnLines,
                      List<Reward> hitRewards, List<Reward> finishRewards) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.maxHealth = maxHealth;
        this.respawnTime = respawnTime;
        this.finishThreshold = finishThreshold;
        this.region = region;
        this.displayName = displayName;
        this.respawnMessage = respawnMessage;
        this.hologramHeight = hologramHeight;
        this.hologramLines = hologramLines;
        this.respawnLines = respawnLines;
        this.hitRewards = hitRewards;
        this.finishRewards = finishRewards;
        this.currentHealth = maxHealth;
    }

    /**
     * Konfigurasyon bolumunden goktasi olusturur.
     *
     * @return gecerli goktasi veya zorunlu alanlar eksikse {@code null}.
     */
    public static Meteorite fromSection(String id, ConfigurationSection s) {
        if (s == null) {
            return null;
        }
        String worldName = s.getString("world", "world");
        int x = s.getInt("x");
        int y = s.getInt("y");
        int z = s.getInt("z");

        Material block = Material.matchMaterial(s.getString("block", "OBSIDIAN"));
        // Gecersiz ya da yerlestirilebilir olmayan (blok olmayan) materyalleri reddet;
        // aksi halde setType cagrisi yukleme sirasinda exception atabilir.
        if (block == null || !block.isBlock()) {
            return null;
        }
        int maxHealth = Math.max(1, s.getInt("max-health", 50));
        int respawnTime = Math.max(1, s.getInt("respawn-time", 60));
        int finishThreshold = Math.max(0, s.getInt("finish-threshold", 0));
        String region = s.getString("region", "");
        String displayName = s.getString("display-name", id);
        String respawnMessage = s.getString("respawn-message", "");

        ConfigurationSection holo = s.getConfigurationSection("hologram");
        double height = holo != null ? holo.getDouble("height", 1.5) : 1.5;
        List<String> lines = holo != null ? holo.getStringList("lines") : Collections.emptyList();
        List<String> respawnLines = holo != null ? holo.getStringList("respawn-lines") : Collections.emptyList();

        List<Reward> hit = readRewards(s.getConfigurationSection("rewards.hit"));
        List<Reward> finish = readRewards(s.getConfigurationSection("rewards.finish"));

        return new Meteorite(id, worldName, x, y, z, block, maxHealth, respawnTime, finishThreshold,
                region, displayName, respawnMessage, height, lines, respawnLines, hit, finish);
    }

    private static List<Reward> readRewards(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyList();
        }
        java.util.List<Reward> list = new java.util.ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(key);
            Reward reward = Reward.fromSection(rewardSection);
            if (reward != null) {
                list.add(reward);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    //  Yardimcilar
    // ------------------------------------------------------------------

    /** Goktasinin blok konumunu doner (onbellekli). Dunya yuklenmemisse null. */
    public Location getLocation() {
        if (cachedLocation == null) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            cachedLocation = new Location(world, x, y, z);
        }
        return cachedLocation;
    }

    /** Hologramin duracagi konum (blogun ortasi + yukseklik). */
    public Location getHologramLocation() {
        Location base = getLocation();
        return base == null ? null : base.clone().add(0.5, hologramHeight, 0.5);
    }

    /** Verilen konum bu goktasinin blogu mu? */
    public boolean matches(Location loc) {
        return loc != null
                && loc.getBlockX() == x
                && loc.getBlockY() == y
                && loc.getBlockZ() == z
                && loc.getWorld() != null
                && loc.getWorld().getName().equals(worldName);
    }

    // ------------------------------------------------------------------
    //  Getter / durum
    // ------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public Material getBlock() {
        return block;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public int getFinishThreshold() {
        return finishThreshold;
    }

    public String getRegion() {
        return region;
    }

    public boolean hasRegion() {
        return region != null && !region.isBlank();
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Bu goktasina ozel respawn mesaji (bos olabilir). */
    public String getRespawnMessage() {
        return respawnMessage;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getRespawnSecondsLeft() {
        return respawnSecondsLeft;
    }

    public void setRespawnSecondsLeft(int respawnSecondsLeft) {
        this.respawnSecondsLeft = respawnSecondsLeft;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public List<String> getRespawnLines() {
        return respawnLines;
    }

    public List<Reward> getHitRewards() {
        return hitRewards;
    }

    public List<Reward> getFinishRewards() {
        return finishRewards;
    }
}
