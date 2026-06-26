package dev.wulnrydev.goktasi.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * WorldGuard ile (varsa) konusan ince bir kopru.
 * <p>
 * WorldGuard sunucuda yoksa, sinif yine yuklenir ama {@link #isAvailable()}
 * false doner ve hicbir WorldGuard sinifi cagrilmadan kontroller atlanir.
 * Boylece soft-depend guvenli sekilde calisir.
 */
public final class WorldGuardHook {

    private final boolean available;

    public WorldGuardHook(Plugin plugin) {
        Plugin wg = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        this.available = wg != null && wg.isEnabled();
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Verilen konum, ismi verilen WorldGuard region'i icinde mi?
     *
     * @param loc        kontrol edilecek konum
     * @param regionName aranan region adi
     * @return region icindeyse {@code true}; WorldGuard yoksa guvenli tarafta
     *         {@code true} (kontrol etkin degil sayilir) doner.
     */
    public boolean isInRegion(Location loc, String regionName) {
        if (!available || loc == null || loc.getWorld() == null) {
            return true;
        }
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (regions == null) {
                return false;
            }
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null) {
                return false;
            }
            BlockVector3 vec = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
            return region.contains(vec);
        } catch (Throwable t) {
            // WorldGuard surum uyumsuzlugu vb. durumlarda sunucuyu kitleme.
            return false;
        }
    }
}
