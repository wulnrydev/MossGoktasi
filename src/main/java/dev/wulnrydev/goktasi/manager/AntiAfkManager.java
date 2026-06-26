package dev.wulnrydev.goktasi.manager;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.gui.AntiAfkHolder;
import dev.wulnrydev.goktasi.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Anti-AFK (makro/autoclicker) korumasi.
 * <p>
 * Her oyuncu icin bir vurus sayaci ve [min-max] arasi rastgele bir "hedef"
 * tutulur. Sayac hedefe ulasinca oyuncuya bir GUI acilir. Oyuncu dogru esyaya
 * (BARRIER) tiklamazsa veya menuyu kapatirsa {@code afk-cmd} calistirilir.
 * <p>
 * <b>Memory-leak korumasi:</b> Oyuncu cikinca {@link #clear(UUID)} ile tum
 * verileri temizlenir. Map'ler oyuncu UUID'si ile anahtarlanir.
 */
public final class AntiAfkManager {

    private final MossGoktasi plugin;

    // Oyuncu basina mevcut vurus sayisi.
    private final ConcurrentHashMap<UUID, Integer> hitCounts = new ConcurrentHashMap<>();
    // Oyuncu basina GUI'nin acilacagi hedef vurus sayisi.
    private final ConcurrentHashMap<UUID, Integer> targets = new ConcurrentHashMap<>();
    // Su an AFK menusu acik olan oyuncular (kapatma = basarisiz).
    private final Set<UUID> openMenus = ConcurrentHashMap.newKeySet();
    // Dogru esyaya tiklayip menuden gecmesine izin verilen oyuncular
    // (close event'inin yanlislikla afk-cmd tetiklemesini onlemek icin).
    private final Set<UUID> passing = ConcurrentHashMap.newKeySet();

    // Config'den okunan ayarlar
    private boolean enabled;
    private int minBreaks;
    private int maxBreaks;
    private String afkCommand;
    private String guiTitle;
    private int guiSize;
    private Material fillMaterial;
    private Material afkItem;
    private int afkSlot;
    private String afkItemName;
    private List<String> afkItemLore;

    public AntiAfkManager(MossGoktasi plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Ayarlari config'den (yeniden) yukler. */
    public void reload() {
        var c = plugin.getConfig();
        this.enabled = c.getBoolean("anti-afk.mode", true);

        int[] range = TextUtil.parseRange(c.getString("anti-afk.break-times-afk", "150-200"), 150, 200);
        this.minBreaks = range[0];
        this.maxBreaks = range[1];

        this.afkCommand = c.getString("anti-afk.afk-cmd", "");
        this.guiTitle = TextUtil.color(c.getString("anti-afk.afk-gui.title", "&cAFK"));
        this.guiSize = normalizeSize(c.getInt("anti-afk.afk-gui.size", 27));

        this.fillMaterial = Material.matchMaterial(c.getString("anti-afk.afk-gui.fills", "NONE"));
        this.afkItem = Material.matchMaterial(c.getString("anti-afk.afk-item.item", "BARRIER"));
        if (this.afkItem == null) {
            this.afkItem = Material.BARRIER;
        }
        this.afkSlot = Math.max(0, Math.min(guiSize - 1, c.getInt("anti-afk.afk-item.slot", guiSize / 2)));
        this.afkItemName = c.getString("anti-afk.afk-item.name", "&cTIKLA!");
        this.afkItemLore = c.getStringList("anti-afk.afk-item.lore");
    }

    private int normalizeSize(int size) {
        if (size < 9) return 9;
        if (size > 54) return 54;
        return (size / 9) * 9; // 9'un katina yuvarla
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Bir vurus kaydeder. Hedefe ulasildiysa AFK dogrulama GUI'sini acar.
     * Menu zaten aciksa yeni vurus sayilmaz. Cagiran taraf, menunun acilip
     * acilmadigini {@link #hasOpenMenu(UUID)} ile kontrol edip vurusu sayip
     * saymayacagina karar verir.
     */
    public void registerHit(Player player) {
        if (!enabled) {
            return;
        }
        UUID uuid = player.getUniqueId();

        // Menu zaten aciksa yeni vurus sayma.
        if (openMenus.contains(uuid)) {
            return;
        }

        int target = targets.computeIfAbsent(uuid, k -> randomTarget());
        int count = hitCounts.merge(uuid, 1, Integer::sum);

        if (count >= target) {
            openMenu(player);
        }
    }

    private int randomTarget() {
        return ThreadLocalRandom.current().nextInt(minBreaks, maxBreaks + 1);
    }

    /** Oyuncuya AFK dogrulama menusunu acar. */
    private void openMenu(Player player) {
        UUID uuid = player.getUniqueId();
        AntiAfkHolder holder = new AntiAfkHolder(player, afkSlot);
        Inventory inv = Bukkit.createInventory(holder, guiSize, guiTitle);
        holder.setInventory(inv);

        // Bos slotlari doldur.
        if (fillMaterial != null && fillMaterial != Material.AIR) {
            ItemStack filler = simpleItem(fillMaterial, " ", null);
            for (int i = 0; i < guiSize; i++) {
                inv.setItem(i, filler);
            }
        }

        // Dogru (BARRIER) esyayi yerlestir.
        inv.setItem(afkSlot, simpleItem(afkItem, afkItemName, afkItemLore));

        openMenus.add(uuid);
        passing.remove(uuid);
        player.openInventory(inv);
    }

    /** Oyuncu dogru esyaya tikladi: menuyu gecir, sayaci sifirla. */
    public void onCorrectClick(Player player) {
        UUID uuid = player.getUniqueId();
        passing.add(uuid);
        openMenus.remove(uuid);
        hitCounts.put(uuid, 0);
        targets.put(uuid, randomTarget());
        // Bir sonraki tick'te kapat (event icinde inventory kapatmak risklidir).
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            passing.remove(uuid);
        });
    }

    /**
     * Oyuncu menuyu kapatti veya yanlis esyaya tikladi: AFK komutunu calistir.
     * {@code passing} listesindeyse (dogru tiklama sonrasi normal kapanis) atla.
     */
    public void onFail(Player player) {
        UUID uuid = player.getUniqueId();
        if (passing.remove(uuid)) {
            return; // Normal (basarili) kapanis.
        }
        if (!openMenus.remove(uuid)) {
            return; // Bu oyuncu icin aktif bir AFK menusu yoktu.
        }
        hitCounts.put(uuid, 0);
        targets.put(uuid, randomTarget());
        runAfkCommand(player);
    }

    private void runAfkCommand(Player player) {
        if (afkCommand == null || afkCommand.isBlank()) {
            return;
        }
        String cmd = afkCommand.replace("%player%", player.getName());
        // Konsoldan, main thread'de calistir.
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    public boolean hasOpenMenu(UUID uuid) {
        return openMenus.contains(uuid);
    }

    /** Oyuncu cikinca tum verileri temizle (memory-leak korumasi). */
    public void clear(UUID uuid) {
        hitCounts.remove(uuid);
        targets.remove(uuid);
        openMenus.remove(uuid);
        passing.remove(uuid);
    }

    // ---- Esya olusturma yardimcisi ----
    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(TextUtil.color(name));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(TextUtil.color(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
