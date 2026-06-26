package dev.wulnrydev.goktasi.listener;

import dev.wulnrydev.goktasi.MossGoktasi;
import dev.wulnrydev.goktasi.gui.AntiAfkHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Anti-AFK GUI etkilesimlerini dinler.
 * <ul>
 *     <li>Dogru esyaya (BARRIER) tiklama -> menu gecilir.</li>
 *     <li>Yanlis esyaya tiklama veya menuyu kapatma -> afk-cmd.</li>
 * </ul>
 * GUI tespiti {@link AntiAfkHolder} marker'i ile O(1) yapilir.
 */
public final class AntiAfkListener implements Listener {

    private final MossGoktasi plugin;

    public AntiAfkListener(MossGoktasi plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AntiAfkHolder afkHolder)) {
            return;
        }

        // AFK menusunde esya tasinmasini tamamen engelle.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Dogru slota ve dogru envantere mi tiklandi?
        if (event.getRawSlot() == afkHolder.getCorrectSlot()
                && event.getClickedInventory() == event.getView().getTopInventory()) {
            plugin.getAntiAfkManager().onCorrectClick(player);
        }
        // Yanlis tiklamada bir sey yapmiyoruz; oyuncu dogru esyayi bulmali.
        // (Kapatmaya calisirsa onClose -> onFail devreye girer.)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AntiAfkHolder)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            // Dogru tiklama sonrasi normal kapanis ise onFail icinde atlanir.
            plugin.getAntiAfkManager().onFail(player);
        }
    }
}
