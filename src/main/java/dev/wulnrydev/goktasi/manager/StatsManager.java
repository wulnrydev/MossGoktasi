package dev.wulnrydev.goktasi.manager;

import dev.wulnrydev.goktasi.MossGoktasi;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Oyuncularin kirdigi (oldurdugu) goktasi sayilarini tutar ve diske kaydeder.
 * <p>
 * Bir oyuncu bir goktasini OLDURDUGUNDE (son vurus) sayaci +1 artar. Veriler
 * {@code stats.yml} dosyasinda saklanir ve PlaceholderAPI ile bir siralama
 * (top) olarak gosterilebilir.
 * <p>
 * <b>Thread modeli:</b> Sayim her zaman ana thread'de ({@link #addKill}) yapilir.
 * Siralama onbellegi ({@code top}, {@code rankByUuid}) {@code volatile}'dir;
 * PlaceholderAPI istekleri (async olabilir) bunlari guvenle okuyabilir.
 * Diske yazma seyrek ve asenkron yapilir (I/O ana thread'i kilitlemez).
 */
public final class StatsManager {

    /** Siralamada tutulacak maksimum kayit (placeholder cikti boyutunu sinirlamak icin). */
    private static final int MAX_TOP = 100;

    private final MossGoktasi plugin;
    private final File file;

    // UUID -> kirilan goktasi sayisi / son bilinen oyuncu adi.
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    // Onbellekler (siralama her oldurmede yeniden hesaplanir).
    private volatile List<TopEntry> top = List.of();
    private volatile Map<UUID, Integer> rankByUuid = Map.of();

    // Bos siralar icin gosterilecek metinler (config'den).
    private volatile String emptyName = "---";
    private volatile String emptyAmount = "0";

    private volatile boolean dirty = false;
    private BukkitTask autoSaveTask;

    public StatsManager(MossGoktasi plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        reload();
        load();
    }

    /** Siralamadaki tek bir kayit (oyuncu + skor). */
    public record TopEntry(UUID uuid, String name, int kills) {}

    // ------------------------------------------------------------------
    //  Yukleme / kaydetme
    // ------------------------------------------------------------------

    /** Config'e bagli ayarlari (bos sira metinleri) yeniden okur. */
    public void reload() {
        this.emptyName = plugin.getConfig().getString("leaderboard.empty-name", "---");
        this.emptyAmount = plugin.getConfig().getString("leaderboard.empty-amount", "0");
    }

    /** stats.yml'den verileri belege yukler. */
    public void load() {
        kills.clear();
        names.clear();

        if (file.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection players = data.getConfigurationSection("players");
            if (players != null) {
                for (String key : players.getKeys(false)) {
                    UUID uuid = parseUuid(key);
                    if (uuid == null) {
                        continue; // Bozuk kayit; atla.
                    }
                    ConfigurationSection sec = players.getConfigurationSection(key);
                    if (sec == null) {
                        continue;
                    }
                    int amount = Math.max(0, sec.getInt("kills", 0));
                    String name = sec.getString("name", "?");
                    if (amount > 0) {
                        kills.put(uuid, amount);
                        names.put(uuid, name);
                    }
                }
            }
        }
        recompute();
    }

    /** Periyodik asenkron otomatik kaydetmeyi baslatir. */
    public void startAutoSave() {
        stopAutoSave();
        // 30 saniyede bir, yalnizca degisiklik varsa kaydet (asenkron).
        this.autoSaveTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::saveIfDirty, 600L, 600L);
    }

    private void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    /** Otomatik kaydetmeyi durdurur ve son veriyi senkron kaydeder (disable icin). */
    public void shutdown() {
        stopAutoSave();
        save();
    }

    private void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    /**
     * Mevcut verileri diske yazar. Ana thread'i kilitlememek icin asenkron
     * cagrilabilir; ConcurrentHashMap anlik goruntusu uzerinde calisir.
     */
    public synchronized void save() {
        // Anlik goruntu al (ana thread eszamanli yazsa bile guvenli).
        Map<UUID, Integer> killSnapshot = new HashMap<>(kills);
        Map<UUID, String> nameSnapshot = new HashMap<>(names);

        FileConfiguration out = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> e : killSnapshot.entrySet()) {
            String base = "players." + e.getKey();
            out.set(base + ".kills", e.getValue());
            out.set(base + ".name", nameSnapshot.getOrDefault(e.getKey(), "?"));
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            out.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("stats.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Sayim
    // ------------------------------------------------------------------

    /** Oyuncu bir goktasini oldurdu: sayaci +1 artir ve siralamayi guncelle. */
    public void addKill(Player player) {
        UUID uuid = player.getUniqueId();
        kills.merge(uuid, 1, Integer::sum);
        names.put(uuid, player.getName());
        dirty = true;
        recompute();
    }

    /** Siralama ve rank onbelleklerini yeniden olusturur (oldurme aninda). */
    private void recompute() {
        List<TopEntry> list = new ArrayList<>(kills.size());
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) {
            list.add(new TopEntry(e.getKey(), names.getOrDefault(e.getKey(), "?"), e.getValue()));
        }
        list.sort(Comparator.comparingInt(TopEntry::kills).reversed());

        Map<UUID, Integer> ranks = new HashMap<>(list.size() * 2);
        for (int i = 0; i < list.size(); i++) {
            ranks.put(list.get(i).uuid(), i + 1);
        }

        // Top listesini sinirla (placeholder ihtiyaci icin yeterli).
        if (list.size() > MAX_TOP) {
            list = new ArrayList<>(list.subList(0, MAX_TOP));
        }

        this.rankByUuid = ranks;
        this.top = List.copyOf(list);
    }

    // ------------------------------------------------------------------
    //  Sorgular (PlaceholderAPI tarafindan, async olabilir)
    // ------------------------------------------------------------------

    /** Oyuncunun kirdigi toplam goktasi sayisi. */
    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    /** Oyuncunun siralamadaki yeri (1 tabanli); kaydi yoksa 0. */
    public int getRank(UUID uuid) {
        return rankByUuid.getOrDefault(uuid, 0);
    }

    /** Ilk {@code limit} kaydi (en cok kirandan aza) doner. Komut/GUI icin. */
    public List<TopEntry> getTop(int limit) {
        List<TopEntry> snapshot = top;
        if (limit >= snapshot.size()) {
            return snapshot;
        }
        return List.copyOf(snapshot.subList(0, Math.max(0, limit)));
    }

    /** {@code position}. siradaki (1 tabanli) oyuncunun adi; bossa empty-name. */
    public String getTopName(int position) {
        List<TopEntry> snapshot = top;
        if (position >= 1 && position <= snapshot.size()) {
            return snapshot.get(position - 1).name();
        }
        return emptyName;
    }

    /** {@code position}. siradaki (1 tabanli) oyuncunun skoru; bossa empty-amount. */
    public String getTopAmount(int position) {
        List<TopEntry> snapshot = top;
        if (position >= 1 && position <= snapshot.size()) {
            return String.valueOf(snapshot.get(position - 1).kills());
        }
        return emptyAmount;
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
