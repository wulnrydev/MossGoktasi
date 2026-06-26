package dev.wulnrydev.goktasi.hook;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.manager.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI baglantisi: "en cok goktasi kiran" siralamasini disari acar.
 * <p>
 * Desteklenen placeholder'lar (identifier: {@code goktasi}):
 * <ul>
 *     <li>{@code %goktasi_kills%}           - oyuncunun kirdigi toplam goktasi</li>
 *     <li>{@code %goktasi_rank%}            - oyuncunun siralamadaki yeri</li>
 *     <li>{@code %goktasi_top_name_<n>%}    - n. siradaki oyuncunun adi</li>
 *     <li>{@code %goktasi_top_amount_<n>%}  - n. siradaki oyuncunun skoru</li>
 * </ul>
 * Ornek: {@code %goktasi_top_name_1%} -> birinci sıradaki oyuncu.
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private static final String PREFIX_NAME = "top_name_";
    private static final String PREFIX_AMOUNT = "top_amount_";

    private final MossGoktasi plugin;

    public PlaceholderHook(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "goktasi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "wulnrydev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** /papi reload sonrasi kayitli kalsin. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        StatsManager stats = plugin.getStatsManager();
        if (stats == null) {
            return null;
        }
        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("kills")) {
            return player == null ? "0" : String.valueOf(stats.getKills(player.getUniqueId()));
        }
        if (key.equals("rank")) {
            return player == null ? "0" : String.valueOf(stats.getRank(player.getUniqueId()));
        }
        if (key.startsWith(PREFIX_NAME)) {
            int pos = parsePosition(key.substring(PREFIX_NAME.length()));
            return pos < 1 ? "" : stats.getTopName(pos);
        }
        if (key.startsWith(PREFIX_AMOUNT)) {
            int pos = parsePosition(key.substring(PREFIX_AMOUNT.length()));
            return pos < 1 ? "" : stats.getTopAmount(pos);
        }
        return null; // Bilinmeyen placeholder.
    }

    private int parsePosition(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
