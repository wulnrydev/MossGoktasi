package dev.wulnrydev.goktasi.command;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.manager.StatsManager;
import dev.wulnrydev.goktasi.model.Meteorite;
import dev.wulnrydev.goktasi.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /goktasi yonetim komutu: reload, list, info.
 */
public final class GoktasiCommand implements TabExecutor {

    private static final String PERMISSION = "goktasi.admin";
    private final MossGoktasi plugin;

    public GoktasiCommand(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            plugin.getMessageManager().sendNoPermission(sender);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextUtil.color("&7/goktasi &#00D420reload &7| &#00D420list &7| &#00D420info <id> &7| &#00D420top &7| &#00D420giveitem <id> [oyuncu] [adet]"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.getMessageManager().sendReload(sender);
            }
            case "list" -> {
                String ids = plugin.getMeteoriteManager().getMeteorites().stream()
                        .map(Meteorite::getId)
                        .collect(Collectors.joining(", "));
                plugin.getMessageManager().send(sender, "&7Göktaşları: &#ffd500" + (ids.isEmpty() ? "yok" : ids));
            }
            case "info" -> {
                if (args.length < 2) {
                    plugin.getMessageManager().send(sender, "&7Kullanım: &#ffd500/goktasi info <id>");
                    return true;
                }
                Meteorite m = plugin.getMeteoriteManager().getMeteorites().stream()
                        .filter(met -> met.getId().equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if (m == null) {
                    plugin.getMessageManager().send(sender, "&7Böyle bir göktaşı &#ffd500yok&7.");
                    return true;
                }
                plugin.getMessageManager().send(sender, "&#00D420" + m.getId()
                        + " &7| Can: &#ffd500" + m.getCurrentHealth() + "/" + m.getMaxHealth()
                        + " &7| Durum: &#ffd500" + (m.isAlive() ? "Canlı" : "Yenileniyor (" + m.getRespawnSecondsLeft() + "s)"));
            }
            case "top" -> {
                List<StatsManager.TopEntry> entries = plugin.getStatsManager().getTop(10);
                if (entries.isEmpty()) {
                    plugin.getMessageManager().send(sender, "&7Henüz göktaşı kıran &#ffd500yok&7.");
                    return true;
                }
                plugin.getMessageManager().send(sender, "&#00D420En Çok Göktaşı Kıranlar:");
                int rank = 1;
                for (StatsManager.TopEntry e : entries) {
                    plugin.getMessageManager().send(sender, "&7" + rank + ". &#ffd500" + e.name()
                            + " &7- &#00D420" + e.kills() + " &7göktaşı");
                    rank++;
                }
            }
            case "giveitem" -> handleGiveItem(sender, args);
            default -> plugin.getMessageManager().send(sender, "&7Bilinmeyen alt komut.");
        }
        return true;
    }

    /** /goktasi giveitem <id> [oyuncu] [adet] */
    private void handleGiveItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "&7Kullanım: &#ffd500/goktasi giveitem <id> [oyuncu] [adet]");
            return;
        }
        String id = args[1];
        if (!plugin.getCustomItemManager().exists(id)) {
            plugin.getMessageManager().send(sender, "&7Böyle bir item &#ffd500yok&7: &#ffd500" + id);
            return;
        }

        // Hedef oyuncu: belirtilmemisse komutu kullanan oyuncu.
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "&7Oyuncu &#ffd500çevrimiçi değil&7: &#ffd500" + args[2]);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            plugin.getMessageManager().send(sender, "&7Konsoldan kullanırken &#ffd500oyuncu adı &7belirtmelisin.");
            return;
        }

        // Miktar: belirtilmemisse config'deki varsayilan (0 -> ezme).
        int amount = 0;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                plugin.getMessageManager().send(sender, "&7Geçersiz adet: &#ffd500" + args[3]);
                return;
            }
        }

        plugin.getCustomItemManager().give(target, id, amount);
        plugin.getMessageManager().send(sender, "&#00D420" + target.getName() + " &7oyuncusuna &#ffd500" + id + " &7verildi.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Stream.of("reload", "list", "info", "top", "giveitem")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return plugin.getMeteoriteManager().getMeteorites().stream()
                    .map(Meteorite::getId)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args[0].equalsIgnoreCase("giveitem")) {
            // arg2: item id, arg3: oyuncu adi
            if (args.length == 2) {
                return plugin.getCustomItemManager().getIds().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                        names.add(p.getName());
                    }
                }
                return names;
            }
        }
        return Collections.emptyList();
    }
}
