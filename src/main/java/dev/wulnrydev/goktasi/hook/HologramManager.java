package dev.wulnrydev.goktasi.hook;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.model.Meteorite;
import dev.wulnrydev.goktasi.util.TextUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * DecentHolograms ile goktasi hologramlarini yonetir.
 * <p>
 * DecentHolograms yoksa {@link #isAvailable()} false doner ve tum cagrilar
 * sessizce atlanir (soft-depend guvenligi).
 * <p>
 * Hologram guncellemeleri ANA THREAD'de yapilir. Guncelleme yalnizca vuruste
 * ve olu goktasi basina saniyede 1 kez tetiklendigi icin maliyeti ihmal
 * edilebilir; DHAPI thread-safe olmadigindan async kullanmak kazanc saglamadan
 * ConcurrentModificationException / paket sorunu riski getirirdi.
 */
public final class HologramManager {

    private final boolean available;

    public HologramManager(MossGoktasi plugin) {
        Plugin dh = plugin.getServer().getPluginManager().getPlugin("DecentHolograms");
        this.available = dh != null && dh.isEnabled();
    }

    public boolean isAvailable() {
        return available;
    }

    private String hologramName(Meteorite m) {
        return "moss_goktasi_" + m.getId();
    }

    /** Goktasi icin hologrami olusturur (veya mevcutsa yeniden olusturur). */
    public void create(Meteorite meteorite) {
        if (!available) {
            return;
        }
        Location loc = meteorite.getHologramLocation();
        if (loc == null) {
            return;
        }
        remove(meteorite);
        List<String> lines = buildAliveLines(meteorite);
        // DHAPI cagrilari hafiftir; olusturma ana thread'de guvenlidir.
        // saveToFile=false: hologramlar plugin tarafindan her yuklemede yeniden
        // uretildiginden DecentHolograms diskine yazmak gereksiz I/O olur.
        DHAPI.createHologram(hologramName(meteorite), loc, false, lines);
    }

    /** Canli haldeki satirlari (can degiskenleri doldurulmus) gunceller. */
    public void updateAlive(Meteorite meteorite) {
        if (!available) {
            return;
        }
        applyLines(meteorite, buildAliveLines(meteorite));
    }

    /** Yenilenme satirlarini (kalan sure ile) gunceller. */
    public void updateRespawning(Meteorite meteorite, int secondsLeft) {
        if (!available) {
            return;
        }
        List<String> lines = new ArrayList<>(meteorite.getRespawnLines().size());
        for (String line : meteorite.getRespawnLines()) {
            lines.add(TextUtil.color(line.replace("%time%", String.valueOf(secondsLeft))));
        }
        applyLines(meteorite, lines);
    }

    private List<String> buildAliveLines(Meteorite m) {
        List<String> lines = new ArrayList<>(m.getHologramLines().size());
        for (String line : m.getHologramLines()) {
            lines.add(TextUtil.color(line
                    .replace("%health%", String.valueOf(m.getCurrentHealth()))
                    .replace("%max_health%", String.valueOf(m.getMaxHealth()))));
        }
        return lines;
    }

    /**
     * Satir guncellemesini ana thread'de uygular. DHAPI thread-safe olmadigi
     * icin senkron calisir; goktasi sayisi az ve guncelleme seyrek oldugundan
     * ana thread uzerinde ihmal edilebilir maliyettedir.
     */
    private void applyLines(Meteorite meteorite, List<String> lines) {
        Hologram holo = DHAPI.getHologram(hologramName(meteorite));
        if (holo != null) {
            DHAPI.setHologramLines(holo, lines);
        }
    }

    /** Hologrami siler. */
    public void remove(Meteorite meteorite) {
        if (!available) {
            return;
        }
        Hologram holo = DHAPI.getHologram(hologramName(meteorite));
        if (holo != null) {
            DHAPI.removeHologram(hologramName(meteorite));
        }
    }
}
