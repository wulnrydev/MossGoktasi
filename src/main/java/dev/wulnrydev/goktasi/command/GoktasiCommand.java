package dev.wulnrydev.goktasi.command;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Meteorite;
import dev.wulnrydev.goktasi.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
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
            sender.sendMessage(TextUtil.color("&7/goktasi &#00D420reload &7| &#00D420list &7| &#00D420info <id>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
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
            default -> plugin.getMessageManager().send(sender, "&7Bilinmeyen alt komut.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Stream.of("reload", "list", "info")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return plugin.getMeteoriteManager().getMeteorites().stream()
                    .map(Meteorite::getId)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
