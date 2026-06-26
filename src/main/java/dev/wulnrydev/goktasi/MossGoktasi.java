package dev.wulnrydev.goktasi;

import dev.wulnrydev.goktasi.command.GoktasiCommand;
import dev.wulnrydev.goktasi.config.MessageManager;
import dev.wulnrydev.goktasi.hook.HologramManager;
import dev.wulnrydev.goktasi.hook.PlaceholderHook;
import dev.wulnrydev.goktasi.hook.WorldGuardHook;
import dev.wulnrydev.goktasi.listener.AntiAfkListener;
import dev.wulnrydev.goktasi.listener.MeteoriteListener;
import dev.wulnrydev.goktasi.listener.PlayerCleanupListener;
import dev.wulnrydev.goktasi.manager.AntiAfkManager;
import dev.wulnrydev.goktasi.manager.MeteoriteManager;
import dev.wulnrydev.goktasi.manager.RewardManager;
import dev.wulnrydev.goktasi.manager.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * MossGoktasi - PaperMC 1.20+ icin optimize edilmis goktasi (meteorite) eklentisi.
 * <p>
 * Yapimci: wulnrydev
 * <p>
 * Tum yoneticiler tek bir merkezi noktadan ({@code this}) erisilir ve eklenti
 * kapanirken duzgunce temizlenir.
 */
public final class MossGoktasi extends JavaPlugin {

    private MessageManager messageManager;
    private WorldGuardHook worldGuardHook;
    private HologramManager hologramManager;
    private RewardManager rewardManager;
    private AntiAfkManager antiAfkManager;
    private MeteoriteManager meteoriteManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        // Varsayilan config.yml'yi disari cikar (yoksa).
        saveDefaultConfig();

        // Yoneticileri olustur.
        this.messageManager = new MessageManager(this);
        this.worldGuardHook = new WorldGuardHook(this);
        this.hologramManager = new HologramManager(this);
        this.rewardManager = new RewardManager(this);
        this.antiAfkManager = new AntiAfkManager(this);
        this.statsManager = new StatsManager(this);
        this.meteoriteManager = new MeteoriteManager(this);

        logHookStatus();

        // Siralama verisini periyodik kaydet.
        statsManager.startAutoSave();

        // PlaceholderAPI varsa siralama placeholder'larini kaydet.
        registerPlaceholders();

        // Goktaslarini yukle ve global task'i baslat.
        meteoriteManager.load();
        meteoriteManager.startTask();

        // Listener'lari kaydet.
        MeteoriteListener meteoriteListener = new MeteoriteListener(this);
        getServer().getPluginManager().registerEvents(meteoriteListener, this);
        getServer().getPluginManager().registerEvents(new AntiAfkListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCleanupListener(this, meteoriteListener), this);

        // Komutu kaydet.
        GoktasiCommand command = new GoktasiCommand(this);
        Objects.requireNonNull(getCommand("goktasi")).setExecutor(command);
        Objects.requireNonNull(getCommand("goktasi")).setTabCompleter(command);

        getLogger().info("MossGoktasi etkinlestirildi. (wulnrydev)");
    }

    @Override
    public void onDisable() {
        if (meteoriteManager != null) {
            meteoriteManager.unload();
        }
        if (statsManager != null) {
            statsManager.shutdown();
        }
        getLogger().info("MossGoktasi devre disi birakildi.");
    }

    /** /goktasi reload tarafindan cagrilir: config + tum yoneticiler yeniden yuklenir. */
    public void reloadAll() {
        reloadConfig();
        messageManager.reload();
        antiAfkManager.reload();
        statsManager.reload();
        // Goktaslarini ve global task'i yeniden kur.
        meteoriteManager.load();
        meteoriteManager.startTask();
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI: bulundu (siralama placeholder'lari etkin).");
        } else {
            getLogger().info("PlaceholderAPI: BULUNAMADI (siralama placeholder'lari devre disi).");
        }
    }

    private void logHookStatus() {
        getLogger().info("DecentHolograms: " + (hologramManager.isAvailable() ? "bulundu" : "BULUNAMADI (hologramlar devre disi)"));
        getLogger().info("WorldGuard: " + (worldGuardHook.isAvailable() ? "bulundu" : "BULUNAMADI (bolge kontrolu devre disi)"));
    }

    // ------------------- Getter'lar -------------------
    public MessageManager getMessageManager()   { return messageManager; }
    public WorldGuardHook getWorldGuardHook()   { return worldGuardHook; }
    public HologramManager getHologramManager() { return hologramManager; }
    public RewardManager getRewardManager()     { return rewardManager; }
    public AntiAfkManager getAntiAfkManager()   { return antiAfkManager; }
    public MeteoriteManager getMeteoriteManager() { return meteoriteManager; }
    public StatsManager getStatsManager()       { return statsManager; }
}
