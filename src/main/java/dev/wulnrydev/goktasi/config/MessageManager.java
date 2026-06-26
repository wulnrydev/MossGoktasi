package dev.wulnrydev.goktasi.config;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Tum mesajlari config'den okuyup, prefix + renk uygulayarak gonderir.
 * Hicbir metin koda gomulu (hardcoded) degildir.
 */
public final class MessageManager {

    private final MossGoktasi plugin;

    private String prefix = "";
    private String noRegion = "";
    private String respawning = "";
    private String reload = "";
    private String noPermission = "";
    private boolean broadcastRespawn = true;
    private String respawnBroadcast = "";

    public MessageManager(MossGoktasi plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Mesajlari config'den (yeniden) yukler. */
    public void reload() {
        FileConfiguration c = plugin.getConfig();
        this.prefix = TextUtil.color(c.getString("messages.prefix", ""));
        this.noRegion = c.getString("messages.no-region", "");
        this.respawning = c.getString("messages.respawning", "");
        this.reload = c.getString("messages.reload", "");
        this.noPermission = c.getString("messages.no-permission", "");
        this.broadcastRespawn = c.getBoolean("settings.broadcast-respawn", true);
        this.respawnBroadcast = c.getString("messages.respawn-broadcast", "");
    }

    /** Prefix ekleyip renklendirerek gonderir. */
    public void send(CommandSender target, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        target.sendMessage(prefix + TextUtil.color(message));
    }

    public void sendNoRegion(CommandSender t)    { send(t, noRegion); }
    public void sendRespawning(CommandSender t)  { send(t, respawning); }
    public void sendReload(CommandSender t)      { send(t, reload); }
    public void sendNoPermission(CommandSender t){ send(t, noPermission); }

    /**
     * Bir goktasi yenilendiginde tum oyunculara duyuru gonderir.
     * <p>
     * Mesaj onceligi: goktasinin kendi {@code respawn-message}'i -> global
     * {@code messages.respawn-broadcast}. Herhangi biri "NONE" / bos ise duyuru
     * yapilmaz. {@code %meteorite%} -> goktasinin display-name ile degisir.
     */
    public void broadcastRespawn(String meteoriteDisplayName, String perMeteoriteMessage) {
        if (!broadcastRespawn) {
            return;
        }
        String template = (perMeteoriteMessage != null && !perMeteoriteMessage.isBlank())
                ? perMeteoriteMessage
                : respawnBroadcast;

        if (template == null || template.isBlank() || template.equalsIgnoreCase("NONE")) {
            return;
        }
        String coloredName = TextUtil.color(meteoriteDisplayName);
        String out = prefix + TextUtil.color(template).replace("%meteorite%", coloredName);
        Bukkit.broadcastMessage(out);
    }

    public String getPrefix() {
        return prefix;
    }
}
